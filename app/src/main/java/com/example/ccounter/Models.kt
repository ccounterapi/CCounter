package com.example.ccounter

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Serializable
enum class GoalType {
    LOSE,
    MAINTAIN,
    GAIN,
}

@Serializable
enum class GenderType {
    MALE,
    FEMALE,
    OTHER,
}

@Serializable
enum class ActivityLevel {
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
enum class MealSource {
    MANUAL,
    AI_TEXT,
    AI_PHOTO,
}

@Serializable
enum class AppLanguage {
    ENGLISH,
    RUSSIAN,
    UKRAINIAN,
}

@Serializable
enum class DeviceAccessStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    EXPIRED,
}

@Serializable
data class DeviceAccessState(
    val status: DeviceAccessStatus = DeviceAccessStatus.PENDING,
    val expiresAtUtcMillis: Long? = null,
    val lastCheckedUtcMillis: Long = 0L,
    val message: String = "",
)

@Serializable
data class PromptUsage(
    val dayKeyUtc: String = "",
    val usedToday: Int = 0,
    val windowKeyUtc: String = "",
    val usedInWindow: Int = 0,
)

@Serializable
enum class NotificationSound {
    DEFAULT,
    BELL,
    CHIME,
}

@Serializable
enum class WeightReminderFrequency {
    DAILY,
    WEEKLY,
    CUSTOM,
}

@Serializable
enum class WeekDay {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN,
}

@Serializable
data class NotificationSettings(
    val sound: NotificationSound = NotificationSound.DEFAULT,
    val mealRemindersEnabled: Boolean = false,
    val mealsPerDay: Int = 3,
    val breakfastMinutes: Int = 8 * 60,
    val lunchMinutes: Int = 13 * 60,
    val dinnerMinutes: Int = 19 * 60,
    val weightReminderEnabled: Boolean = false,
    val weightFrequency: WeightReminderFrequency = WeightReminderFrequency.WEEKLY,
    val weightWeekDays: List<WeekDay> = listOf(WeekDay.MON),
    val weightCustomEveryDays: Int = 3,
    val weightTimeMinutes: Int = 20 * 60,
)

@Serializable
data class UserProfile(
    val name: String = "Alex Johnson",
    val profilePhotoUri: String? = null,
    val goal: GoalType = GoalType.MAINTAIN,
    val gender: GenderType = GenderType.MALE,
    val age: Int = 25,
    val heightCm: Int = 180,
    val weightKg: Double = 75.9,
    val activityLevel: ActivityLevel = ActivityLevel.MEDIUM,
    val dailyTargetCalories: Int = 1800,
    val startWeightKg: Double = 78.5,
    val goalWeightKg: Double = 72.0,
)

@Serializable
data class MealItem(
    val name: String,
    val grams: Int,
    val kcal: Int,
    val confidence: String = "High",
)

@Serializable
data class MealEntry(
    val id: Long,
    val timestamp: Long,
    val name: String,
    val description: String,
    val totalKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val source: MealSource,
    val items: List<MealItem> = emptyList(),
    val photoDataUrl: String? = null,
)

@Serializable
data class WeightEntry(
    val id: Long,
    val timestamp: Long,
    val weightKg: Double,
)

@Serializable
data class AppData(
    val onboardingCompleted: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val meals: List<MealEntry> = defaultMeals(),
    val weights: List<WeightEntry> = defaultWeights(),
    val openAiApiKey: String = "",
    val notifications: NotificationSettings = NotificationSettings(),
    val language: AppLanguage = AppLanguage.ENGLISH,
    val deviceId: String = "",
    val registrationSubmitted: Boolean = false,
    val deviceAccess: DeviceAccessState = DeviceAccessState(),
    val promptUsage: PromptUsage = PromptUsage(),
)

@Serializable
data class AiMealDraft(
    val name: String,
    val description: String,
    val totalKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val items: List<MealItem>,
    val source: MealSource,
    val photoDataUrl: String? = null,
)

data class DayStats(
    val consumed: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

fun calculateDailyCalories(profile: UserProfile): Int {
    val bmr = when (profile.gender) {
        GenderType.FEMALE -> 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.age - 161
        GenderType.MALE, GenderType.OTHER -> 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.age + 5
    }
    val activityMultiplier = when (profile.activityLevel) {
        ActivityLevel.LOW -> 1.375
        ActivityLevel.MEDIUM -> 1.55
        ActivityLevel.HIGH -> 1.725
    }
    val adjustment = when (profile.goal) {
        GoalType.LOSE -> -300
        GoalType.MAINTAIN -> 0
        GoalType.GAIN -> 300
    }
    return (bmr * activityMultiplier + adjustment).toInt().coerceAtLeast(1100)
}

fun dayStats(meals: List<MealEntry>, day: LocalDate = LocalDate.now()): DayStats {
    val zone = ZoneId.systemDefault()
    val dayMeals = meals.filter {
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), zone).toLocalDate() == day
    }
    return DayStats(
        consumed = dayMeals.sumOf { it.totalKcal },
        protein = dayMeals.sumOf { it.proteinG },
        carbs = dayMeals.sumOf { it.carbsG },
        fat = dayMeals.sumOf { it.fatG },
    )
}

fun defaultMeals(): List<MealEntry> = emptyList()

fun defaultWeights(): List<WeightEntry> = emptyList()

fun GoalType.label(): String = when (this) {
    GoalType.LOSE -> "Lose weight"
    GoalType.MAINTAIN -> "Maintain"
    GoalType.GAIN -> "Gain weight"
}

fun resolveGoalTypeByWeights(currentWeightKg: Double, goalWeightKg: Double): GoalType {
    return when {
        goalWeightKg > currentWeightKg + 0.0001 -> GoalType.GAIN
        goalWeightKg < currentWeightKg - 0.0001 -> GoalType.LOSE
        else -> GoalType.MAINTAIN
    }
}

fun ActivityLevel.label(): String = when (this) {
    ActivityLevel.LOW -> "Low"
    ActivityLevel.MEDIUM -> "Medium"
    ActivityLevel.HIGH -> "High"
}

fun GenderType.label(): String = when (this) {
    GenderType.MALE -> "Male"
    GenderType.FEMALE -> "Female"
    GenderType.OTHER -> "Other"
}

fun NotificationSound.label(): String = when (this) {
    NotificationSound.DEFAULT -> "Default"
    NotificationSound.BELL -> "Bell"
    NotificationSound.CHIME -> "Chime"
}

fun WeightReminderFrequency.label(): String = when (this) {
    WeightReminderFrequency.DAILY -> "Daily"
    WeightReminderFrequency.WEEKLY -> "Weekly"
    WeightReminderFrequency.CUSTOM -> "Custom"
}

fun WeekDay.label(): String = when (this) {
    WeekDay.MON -> "Mon"
    WeekDay.TUE -> "Tue"
    WeekDay.WED -> "Wed"
    WeekDay.THU -> "Thu"
    WeekDay.FRI -> "Fri"
    WeekDay.SAT -> "Sat"
    WeekDay.SUN -> "Sun"
}

fun AppLanguage.label(): String = when (this) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.RUSSIAN -> "Русский"
    AppLanguage.UKRAINIAN -> "Українська"
}
