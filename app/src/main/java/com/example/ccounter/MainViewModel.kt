package com.example.ccounter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.nio.charset.StandardCharsets

private const val DAILY_PROMPT_LIMIT = 80
private const val WINDOW_PROMPT_LIMIT = 20
private const val WINDOW_HOURS = 3
private const val ACCESS_SYNC_MIN_INTERVAL_MS = 45_000L
private const val BASE63_NO_Z_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy0123456789+/"
private val BASE63_NO_Z_BASE = BigInteger.valueOf(63L)

private fun tr(language: AppLanguage, en: String, ru: String, uk: String): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.RUSSIAN -> ru
    AppLanguage.UKRAINIAN -> uk
}

data class PromptQuotaInfo(
    val remainingToday: Int = DAILY_PROMPT_LIMIT,
    val remainingWindow: Int = WINDOW_PROMPT_LIMIT,
    val nextWindowResetUtcMillis: Long = 0L,
    val nextDayResetUtcMillis: Long = 0L,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = AppStorage(application)

    var appData by mutableStateOf(storage.load())
        private set

    var pendingAiDraft by mutableStateOf<AiMealDraft?>(null)
        private set

    var isAnalyzingMeal by mutableStateOf(false)
        private set

    var analyzeError by mutableStateOf<String?>(null)
        private set

    var promptQuotaInfo by mutableStateOf(PromptQuotaInfo())
        private set

    var isAccessAllowed by mutableStateOf(false)
        private set

    var accessMessage by mutableStateOf<String?>(null)
        private set

    var isSyncingPolicy by mutableStateOf(false)
        private set

    private var sessionOpenAiApiKey by mutableStateOf("")
    private var queuedForcePolicySync = false

    private fun l(en: String, ru: String, uk: String): String = tr(appData.language, en, ru, uk)

    init {
        ensureDeviceId()
        updateQuotaInfo(nowUtcMillis = System.currentTimeMillis(), usage = appData.promptUsage)
        applyAccessState(appData.deviceAccess, nowUtcMillis = System.currentTimeMillis())
        ReminderScheduler.rescheduleAll(getApplication(), appData.notifications)
    }

    fun completeOnboarding(profile: UserProfile, language: AppLanguage) {
        val normalizedProfile = profile.copy(
            goal = resolveGoalTypeByWeights(profile.startWeightKg, profile.goalWeightKg),
        )
        val updatedProfile = normalizedProfile.copy(
            dailyTargetCalories = calculateDailyCalories(normalizedProfile),
        )
        val initialWeight = WeightEntry(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            weightKg = updatedProfile.weightKg,
        )
        appData = appData.copy(
            onboardingCompleted = true,
            profile = updatedProfile,
            meals = emptyList(),
            weights = listOf(initialWeight),
            language = language,
            registrationSubmitted = false,
            deviceAccess = DeviceAccessState(
                status = DeviceAccessStatus.PENDING,
                expiresAtUtcMillis = null,
                lastCheckedUtcMillis = 0L,
                message = l(
                    "Device registration is pending admin approval.",
                    "Регистрация устройства ожидает одобрения администратора.",
                    "Реєстрація пристрою очікує схвалення адміністратора.",
                ),
            ),
            promptUsage = PromptUsage(),
        )
        applyAccessState(appData.deviceAccess, nowUtcMillis = System.currentTimeMillis())
        persist()
        refreshRemotePolicy(force = true)
    }

    fun updateLanguage(language: AppLanguage) {
        appData = appData.copy(language = language)
        persist()
    }

    fun refreshRemotePolicy(force: Boolean = false) {
        if (!appData.onboardingCompleted) {
            isAccessAllowed = true
            accessMessage = null
            sessionOpenAiApiKey = ""
            return
        }
        if (isSyncingPolicy) {
            if (force) {
                queuedForcePolicySync = true
            }
            return
        }

        viewModelScope.launch {
            isSyncingPolicy = true
            try {
                val nowUtcMillis = RemotePolicyService.fetchNetworkUtcNowMillis().getOrElse { throw it }
                val normalizedUsage = normalizePromptUsage(appData.promptUsage, nowUtcMillis)
                if (normalizedUsage != appData.promptUsage) {
                    appData = appData.copy(promptUsage = normalizedUsage)
                    persist()
                }
                updateQuotaInfo(nowUtcMillis = nowUtcMillis, usage = normalizedUsage)

                val recentlyChecked = nowUtcMillis - appData.deviceAccess.lastCheckedUtcMillis < ACCESS_SYNC_MIN_INTERVAL_MS
                if (!force && recentlyChecked && isAccessAllowed) {
                    applyAccessState(appData.deviceAccess, nowUtcMillis)
                    return@launch
                }

                val decision = RemotePolicyService.fetchAccessDecision(appData.deviceId).getOrElse { throw it }
                var registrationSubmitted = decision.hasDeviceRecord
                if (!decision.hasDeviceRecord) {
                    val submitted = RemotePolicyService.submitDeviceRegistration(
                        deviceId = appData.deviceId,
                        profileName = appData.profile.name,
                        language = appData.language,
                    )
                    registrationSubmitted = submitted.isSuccess
                }
                val updatedAccess = DeviceAccessState(
                    status = decision.status,
                    expiresAtUtcMillis = decision.expiresAtUtcMillis,
                    lastCheckedUtcMillis = decision.checkedAtUtcMillis,
                    message = decision.message,
                )
                sessionOpenAiApiKey = decodeBase63NoZ(decision.openAiApiKeyBase63NoZ).orEmpty()
                appData = appData.copy(
                    deviceAccess = updatedAccess,
                    registrationSubmitted = registrationSubmitted,
                )
                persist()
                applyAccessState(updatedAccess, nowUtcMillis)
            } catch (_: Throwable) {
                isAccessAllowed = false
                sessionOpenAiApiKey = ""
                accessMessage = l(
                    "Internet Connection is required to use the app.",
                    "Для использования приложения требуется подключение к интернету.",
                    "Для використання застосунку потрібне підключення до інтернету.",
                )
            } finally {
                isSyncingPolicy = false
                if (queuedForcePolicySync) {
                    queuedForcePolicySync = false
                    refreshRemotePolicy(force = true)
                }
            }
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        val normalizedWeekDays = settings.weightWeekDays
            .distinct()
            .ifEmpty { listOf(WeekDay.MON) }
        val normalized = settings.copy(
            sound = NotificationSound.DEFAULT,
            mealsPerDay = settings.mealsPerDay.coerceIn(1, 3),
            weightWeekDays = normalizedWeekDays,
            weightCustomEveryDays = settings.weightCustomEveryDays.coerceIn(2, 30),
        )
        appData = appData.copy(notifications = normalized)
        persist()
        ReminderScheduler.rescheduleAll(getApplication(), normalized)
    }

    fun updateProfile(profile: UserProfile) {
        val normalizedProfile = profile.copy(
            goal = resolveGoalTypeByWeights(profile.startWeightKg, profile.goalWeightKg),
        )
        val updatedProfile = normalizedProfile.copy(
            dailyTargetCalories = calculateDailyCalories(normalizedProfile),
        )
        val updatedWeights = if (appData.weights.isEmpty()) {
            listOf(
                WeightEntry(
                    id = 1L,
                    timestamp = System.currentTimeMillis(),
                    weightKg = updatedProfile.weightKg,
                ),
            )
        } else {
            appData.weights
        }
        appData = appData.copy(
            profile = updatedProfile,
            weights = updatedWeights,
        )
        persist()
    }

    fun addManualMeal(
        name: String,
        description: String,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
    ) {
        val entry = MealEntry(
            id = nextMealId(),
            timestamp = System.currentTimeMillis(),
            name = name.ifBlank {
                l("Manual meal", "Ручной прием пищи", "Ручний прийом їжі")
            },
            description = description.ifBlank {
                l("Manual input", "Ручной ввод", "Ручне введення")
            },
            totalKcal = kcal.coerceAtLeast(0),
            proteinG = protein.coerceAtLeast(0),
            carbsG = carbs.coerceAtLeast(0),
            fatG = fat.coerceAtLeast(0),
            source = MealSource.MANUAL,
        )
        appData = appData.copy(meals = appData.meals + entry)
        persist()
    }

    fun analyzeMeal(description: String, imageDataUrl: String?, onSuccess: () -> Unit) {
        analyzeError = null
        isAnalyzingMeal = true
        viewModelScope.launch {
            val quota = consumePromptAllowance()
            if (quota.isFailure) {
                isAnalyzingMeal = false
                analyzeError = quota.exceptionOrNull()?.message ?: l(
                    "Prompt limit reached.",
                    "Лимит запросов достигнут.",
                    "Ліміт запитів досягнуто.",
                )
                return@launch
            }

            val result = OpenAiMealAnalyzer.analyze(
                apiKey = sessionOpenAiApiKey.trim(),
                description = description,
                imageDataUrl = imageDataUrl,
                language = appData.language,
            )
            isAnalyzingMeal = false
            result.fold(
                onSuccess = {
                    pendingAiDraft = it.copy(photoDataUrl = imageDataUrl)
                    onSuccess()
                },
                onFailure = {
                    analyzeError = it.message ?: l(
                        "Failed to analyze meal.",
                        "Не удалось проанализировать прием пищи.",
                        "Не вдалося проаналізувати прийом їжі.",
                    )
                },
            )
        }
    }

    suspend fun calculateComponentCalories(
        productName: String,
        grams: Int,
    ): Result<OpenAiMealAnalyzer.ComponentCaloriesEstimate> {
        val quota = consumePromptAllowance()
        if (quota.isFailure) {
            return Result.failure(
                quota.exceptionOrNull() ?: IllegalStateException(
                    l(
                        "Prompt limit reached.",
                        "Лимит запросов достигнут.",
                        "Ліміт запитів досягнуто.",
                    ),
                ),
            )
        }
        return OpenAiMealAnalyzer.calculateComponentCalories(
            apiKey = sessionOpenAiApiKey.trim(),
            productName = productName,
            grams = grams,
            language = appData.language,
        )
    }

    fun clearAnalyzeError() {
        analyzeError = null
    }

    fun discardAiDraft() {
        pendingAiDraft = null
    }

    fun updatePendingAiDraft(draft: AiMealDraft) {
        pendingAiDraft = draft
    }

    fun confirmAiDraft() {
        val draft = pendingAiDraft ?: return
        val entry = MealEntry(
            id = nextMealId(),
            timestamp = System.currentTimeMillis(),
            name = draft.name,
            description = draft.description,
            totalKcal = draft.totalKcal,
            proteinG = draft.proteinG,
            carbsG = draft.carbsG,
            fatG = draft.fatG,
            source = draft.source,
            items = draft.items,
            photoDataUrl = draft.photoDataUrl,
        )
        appData = appData.copy(meals = appData.meals + entry)
        pendingAiDraft = null
        persist()
    }

    fun updateMeal(
        id: Long,
        name: String,
        description: String,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        timestamp: Long,
        photoDataUrl: String?,
    ) {
        if (kcal < 0 || protein < 0 || carbs < 0 || fat < 0) return
        if (appData.meals.none { it.id == id }) return
        val updatedMeals = appData.meals
            .map { meal ->
                if (meal.id == id) {
                    meal.copy(
                        name = name.ifBlank { meal.name },
                        description = description.ifBlank { meal.description },
                        totalKcal = kcal,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        timestamp = timestamp,
                        photoDataUrl = photoDataUrl,
                    )
                } else {
                    meal
                }
            }
            .sortedBy { it.timestamp }
        appData = appData.copy(meals = updatedMeals)
        persist()
    }

    fun deleteMeal(id: Long) {
        if (appData.meals.none { it.id == id }) return
        appData = appData.copy(meals = appData.meals.filterNot { it.id == id })
        persist()
    }

    fun addWeight(value: Double) {
        if (value <= 0.0) return
        val entry = WeightEntry(
            id = nextWeightId(),
            timestamp = System.currentTimeMillis(),
            weightKg = value,
        )
        val updated = (appData.weights + entry).sortedBy { it.timestamp }
        appData = appData.copy(weights = updated)
        persist()
    }

    fun updateWeight(id: Long, value: Double, timestamp: Long) {
        if (value <= 0.0) return
        if (appData.weights.none { it.id == id }) return
        val updated = appData.weights
            .map { item ->
                if (item.id == id) {
                    item.copy(weightKg = value, timestamp = timestamp)
                } else {
                    item
                }
            }
            .sortedBy { it.timestamp }
        appData = appData.copy(weights = updated)
        persist()
    }

    fun deleteWeight(id: Long) {
        if (appData.weights.none { it.id == id }) return
        appData = appData.copy(weights = appData.weights.filterNot { it.id == id })
        persist()
    }

    fun signOut() {
        val keptDeviceId = appData.deviceId.ifBlank { UUID.randomUUID().toString() }
        val keptLanguage = appData.language
        appData = AppData(
            language = keptLanguage,
            deviceId = keptDeviceId,
            registrationSubmitted = appData.registrationSubmitted,
            deviceAccess = appData.deviceAccess,
        )
        pendingAiDraft = null
        analyzeError = null
        isAnalyzingMeal = false
        sessionOpenAiApiKey = ""
        updateQuotaInfo(nowUtcMillis = System.currentTimeMillis(), usage = appData.promptUsage)
        applyAccessState(appData.deviceAccess, nowUtcMillis = System.currentTimeMillis())
        persist()
        ReminderScheduler.cancelAll(getApplication())
    }

    fun todaysMeals(): List<MealEntry> = mealsForDay(java.time.LocalDate.now())

    fun mealsForDay(day: java.time.LocalDate): List<MealEntry> = appData.meals
        .filter {
            java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate() == day
        }
        .sortedBy { it.timestamp }

    private suspend fun consumePromptAllowance(): Result<Unit> {
        if (!appData.onboardingCompleted) {
            return Result.failure(
                IllegalStateException(
                    l(
                        "Complete onboarding first.",
                        "Сначала завершите онбординг.",
                        "Спочатку завершіть онбординг.",
                    ),
                ),
            )
        }
        if (!isAccessAllowed) {
            return Result.failure(
                IllegalStateException(
                    accessMessage ?: l(
                        "Access for this device is not enabled.",
                        "Доступ для этого устройства не включен.",
                        "Доступ для цього пристрою не увімкнено.",
                    ),
                ),
            )
        }

        val nowUtcMillis = RemotePolicyService.fetchNetworkUtcNowMillis().getOrElse {
            return Result.failure(
                IllegalStateException(
                    l(
                        "Internet Connection is required to use AI features.",
                        "Для AI-функций требуется подключение к интернету.",
                        "Для AI-функцій потрібне підключення до інтернету.",
                    ),
                ),
            )
        }

        var usage = normalizePromptUsage(appData.promptUsage, nowUtcMillis)

        if (sessionOpenAiApiKey.isBlank()) {
            val latestDecision = RemotePolicyService.fetchAccessDecision(appData.deviceId).getOrNull()
            if (latestDecision != null) {
                val refreshedAccess = DeviceAccessState(
                    status = latestDecision.status,
                    expiresAtUtcMillis = latestDecision.expiresAtUtcMillis,
                    lastCheckedUtcMillis = latestDecision.checkedAtUtcMillis,
                    message = latestDecision.message,
                )
                sessionOpenAiApiKey = decodeBase63NoZ(latestDecision.openAiApiKeyBase63NoZ).orEmpty()
                appData = appData.copy(deviceAccess = refreshedAccess)
                persist()
                applyAccessState(refreshedAccess, nowUtcMillis)
                if (!isAccessAllowed) {
                    return Result.failure(
                        IllegalStateException(
                            accessMessage ?: l(
                                "Access for this device is not enabled.",
                                "Доступ для этого устройства не включен.",
                                "Доступ для цього пристрою не увімкнено.",
                            ),
                        ),
                    )
                }
            }
        }

        if (sessionOpenAiApiKey.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    l(
                        "AI key is not configured by admin. Please contact admin.",
                        "AI ключ не настроен администратором. Обратитесь к администратору.",
                        "AI ключ не налаштований адміністратором. Зверніться до адміністратора.",
                    ),
                ),
            )
        }

        if (usage.usedToday >= DAILY_PROMPT_LIMIT) {
            updateQuotaInfo(nowUtcMillis = nowUtcMillis, usage = usage)
            val reset = formatUtcTime(nextDayResetUtcMillis(nowUtcMillis))
            return Result.failure(
                IllegalStateException(
                    l(
                        "Daily prompt limit reached (80/80). Next reset: $reset.",
                        "Достигнут дневной лимит запросов (80/80). Следующий сброс: $reset.",
                        "Досягнуто денний ліміт запитів (80/80). Наступне скидання: $reset.",
                    ),
                ),
            )
        }
        if (usage.usedInWindow >= WINDOW_PROMPT_LIMIT) {
            updateQuotaInfo(nowUtcMillis = nowUtcMillis, usage = usage)
            val reset = formatUtcTime(nextWindowResetUtcMillis(nowUtcMillis))
            return Result.failure(
                IllegalStateException(
                    l(
                        "Prompt limit reached (20 per 3 hours). Next reset: $reset.",
                        "Достигнут лимит запросов (20 за 3 часа). Следующий сброс: $reset.",
                        "Досягнуто ліміт запитів (20 за 3 години). Наступне скидання: $reset.",
                    ),
                ),
            )
        }

        usage = usage.copy(
            usedToday = usage.usedToday + 1,
            usedInWindow = usage.usedInWindow + 1,
        )
        appData = appData.copy(promptUsage = usage)
        persist()
        updateQuotaInfo(nowUtcMillis = nowUtcMillis, usage = usage)
        return Result.success(Unit)
    }

    private fun ensureDeviceId() {
        if (appData.deviceId.isNotBlank()) return
        appData = appData.copy(deviceId = UUID.randomUUID().toString())
        persist()
    }

    private fun applyAccessState(state: DeviceAccessState, nowUtcMillis: Long) {
        if (!appData.onboardingCompleted) {
            isAccessAllowed = true
            accessMessage = null
            return
        }
        val normalizedStatus = when {
            state.status == DeviceAccessStatus.ACTIVE &&
                state.expiresAtUtcMillis != null &&
                nowUtcMillis >= state.expiresAtUtcMillis -> DeviceAccessStatus.EXPIRED
            else -> state.status
        }
        isAccessAllowed = normalizedStatus == DeviceAccessStatus.ACTIVE
        accessMessage = if (isAccessAllowed) null else state.message.ifBlank {
            when (normalizedStatus) {
                DeviceAccessStatus.PENDING -> l(
                    "Device is pending approval in admin panel.",
                    "Устройство ожидает одобрения в админ-панели.",
                    "Пристрій очікує схвалення в адмін-панелі.",
                )
                DeviceAccessStatus.PAUSED -> l(
                    "Access is paused by admin.",
                    "Доступ приостановлен администратором.",
                    "Доступ призупинений адміністратором.",
                )
                DeviceAccessStatus.EXPIRED -> l(
                    "Access period ended. Ask admin to extend it.",
                    "Срок доступа истек. Попросите администратора продлить его.",
                    "Термін доступу завершився. Попросіть адміністратора продовжити його.",
                )
                DeviceAccessStatus.ACTIVE -> ""
            }
        }
    }

    private fun normalizePromptUsage(usage: PromptUsage, nowUtcMillis: Long): PromptUsage {
        val dayKey = dayKeyUtc(nowUtcMillis)
        val windowKey = windowKeyUtc(nowUtcMillis)
        var normalized = usage
        if (normalized.dayKeyUtc != dayKey) {
            normalized = normalized.copy(
                dayKeyUtc = dayKey,
                usedToday = 0,
                windowKeyUtc = windowKey,
                usedInWindow = 0,
            )
        } else if (normalized.windowKeyUtc != windowKey) {
            normalized = normalized.copy(
                windowKeyUtc = windowKey,
                usedInWindow = 0,
            )
        }
        return normalized
    }

    private fun updateQuotaInfo(nowUtcMillis: Long, usage: PromptUsage) {
        val normalized = normalizePromptUsage(usage, nowUtcMillis)
        promptQuotaInfo = PromptQuotaInfo(
            remainingToday = (DAILY_PROMPT_LIMIT - normalized.usedToday).coerceAtLeast(0),
            remainingWindow = (WINDOW_PROMPT_LIMIT - normalized.usedInWindow).coerceAtLeast(0),
            nextWindowResetUtcMillis = nextWindowResetUtcMillis(nowUtcMillis),
            nextDayResetUtcMillis = nextDayResetUtcMillis(nowUtcMillis),
        )
    }

    private fun dayKeyUtc(nowUtcMillis: Long): String {
        return Instant.ofEpochMilli(nowUtcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()
    }

    private fun windowKeyUtc(nowUtcMillis: Long): String {
        val zoned = Instant.ofEpochMilli(nowUtcMillis).atZone(ZoneOffset.UTC)
        val blockHour = (zoned.hour / WINDOW_HOURS) * WINDOW_HOURS
        return "%s-%02d".format(Locale.ENGLISH, zoned.toLocalDate().toString(), blockHour)
    }

    private fun nextWindowResetUtcMillis(nowUtcMillis: Long): Long {
        val zoned = Instant.ofEpochMilli(nowUtcMillis).atZone(ZoneOffset.UTC)
        val blockHour = (zoned.hour / WINDOW_HOURS) * WINDOW_HOURS
        var reset = zoned
            .withHour(blockHour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plusHours(WINDOW_HOURS.toLong())
        if (!reset.isAfter(zoned)) {
            reset = reset.plusHours(WINDOW_HOURS.toLong())
        }
        return reset.toInstant().toEpochMilli()
    }

    private fun nextDayResetUtcMillis(nowUtcMillis: Long): Long {
        val zoned = Instant.ofEpochMilli(nowUtcMillis).atZone(ZoneOffset.UTC)
        return zoned.toLocalDate()
            .plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    private fun formatUtcTime(utcMillis: Long): String {
        return Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("HH:mm 'UTC', yyyy-MM-dd", Locale.ENGLISH))
    }

    private fun nextMealId(): Long = (appData.meals.maxOfOrNull { it.id } ?: 0L) + 1L
    private fun nextWeightId(): Long = (appData.weights.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun persist() {
        storage.save(appData)
    }

    private fun decodeBase63NoZ(encoded: String): String? {
        val source = encoded.trim()
        if (source.isBlank()) return null
        var value = BigInteger.ZERO
        for (char in source) {
            val idx = BASE63_NO_Z_ALPHABET.indexOf(char)
            if (idx < 0) return null
            value = value.multiply(BASE63_NO_Z_BASE).add(BigInteger.valueOf(idx.toLong()))
        }
        if (value == BigInteger.ZERO) return null
        val bytes = value.toByteArray()
        val normalized = if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
        if (normalized.isEmpty()) return null
        return runCatching { String(normalized, StandardCharsets.UTF_8) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
