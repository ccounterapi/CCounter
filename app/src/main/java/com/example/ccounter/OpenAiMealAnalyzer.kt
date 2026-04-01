package com.example.ccounter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object OpenAiMealAnalyzer {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private const val OPENAI_MODEL = "gpt-4.1-nano"

    data class ComponentCaloriesEstimate(
        val kcal: Int,
        val isEdible: Boolean,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(
        apiKey: String,
        description: String,
        imageDataUrl: String?,
        language: AppLanguage = AppLanguage.ENGLISH,
    ): Result<AiMealDraft> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    tr(
                        language,
                        "AI key is not configured by admin. Please contact admin.",
                        "AI ключ не настроен администратором. Обратитесь к администратору.",
                        "AI ключ не налаштований адміністратором. Зверніться до адміністратора.",
                    ),
                ),
            )
        }

        try {
            val userContent = JSONArray()
            userContent.put(
                JSONObject()
                    .put("type", "text")
                    .put(
                        "text",
                        buildString {
                            append("Meal description: ")
                            append(description.ifBlank { "No description provided" })
                            append(". Estimate calories and macros for this meal.")
                        },
                    ),
            )
            if (!imageDataUrl.isNullOrBlank()) {
                userContent.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", imageDataUrl)),
                )
            }

            val messages = JSONArray()
            messages.put(
                JSONObject().put("role", "system").put(
                    "content",
                    "You are a nutrition assistant. Reply ONLY valid JSON with keys " +
                        "meal_name,total_kcal,protein_g,carbs_g,fat_g,items,summary. " +
                        "items is an array of objects with keys name,grams,kcal. " +
                        "Do not include weight in the name. Put weight only in grams.",
                ),
            )
            messages.put(JSONObject().put("role", "user").put("content", userContent))

            val payload = JSONObject()
                .put("model", OPENAI_MODEL)
                .put("temperature", 0.2)
                .put("messages", messages)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val responseText = client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(bodyText).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    val errorMessage = message?.takeIf { it.isNotBlank() }
                        ?: "OpenAI request failed with code ${response.code}."
                    throw IllegalStateException(errorMessage)
                }
                bodyText
            }

            val content = extractAssistantContent(responseText)
            val parsed = JSONObject(cleanJson(content))

            val items = parsed.optJSONArray("items")?.toMealItems(language).orEmpty()
            val total = parsed.optInt("total_kcal").takeIf { it > 0 }
                ?: items.sumOf { it.kcal }.coerceAtLeast(50)

            val draft = AiMealDraft(
                name = parsed.optString("meal_name").ifBlank { "AI meal" },
                description = parsed.optString("summary").ifBlank { description.ifBlank { "AI analyzed meal" } },
                totalKcal = total,
                proteinG = parsed.optInt("protein_g").coerceAtLeast(0),
                carbsG = parsed.optInt("carbs_g").coerceAtLeast(0),
                fatG = parsed.optInt("fat_g").coerceAtLeast(0),
                items = items,
                source = if (imageDataUrl.isNullOrBlank()) MealSource.AI_TEXT else MealSource.AI_PHOTO,
            )
            Result.success(draft)
        } catch (error: Throwable) {
            Result.failure(error.toUserFacingAnalyzerError(language))
        }
    }

    suspend fun calculateComponentCalories(
        apiKey: String,
        productName: String,
        grams: Int,
        language: AppLanguage = AppLanguage.ENGLISH,
    ): Result<ComponentCaloriesEstimate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    tr(
                        language,
                        "AI key is not configured by admin. Please contact admin.",
                        "AI ключ не настроен администратором. Обратитесь к администратору.",
                        "AI ключ не налаштований адміністратором. Зверніться до адміністратора.",
                    ),
                ),
            )
        }

        val normalizedName = productName.trim()
        val safeGrams = grams.coerceAtLeast(0)
        if (normalizedName.isBlank()) {
            return@withContext Result.success(
                ComponentCaloriesEstimate(
                    kcal = 0,
                    isEdible = false,
                ),
            )
        }

        try {
            val messages = JSONArray()
            messages.put(
                JSONObject().put("role", "system").put(
                    "content",
                    "You are a nutrition assistant. Reply ONLY valid JSON with keys edible,kcal. " +
                        "edible is boolean and kcal is integer calories for the provided grams. " +
                        "If the name is not an edible food, drink, ingredient, or dish, set edible to false and kcal to 0. " +
                        "Do not include any extra text.",
                ),
            )
            messages.put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "content",
                        buildString {
                            append("Food name: ")
                            append(normalizedName)
                            append("\nWeight (g): ")
                            append(safeGrams)
                        },
                    ),
            )

            val payload = JSONObject()
                .put("model", OPENAI_MODEL)
                .put("temperature", 0.1)
                .put("messages", messages)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val responseText = client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(bodyText).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    val errorMessage = message?.takeIf { it.isNotBlank() }
                        ?: "OpenAI request failed with code ${response.code}."
                    throw IllegalStateException(errorMessage)
                }
                bodyText
            }

            val content = extractAssistantContent(responseText)
            val parsed = JSONObject(cleanJson(content))
            val rawKcal = parsed.opt("kcal")
            val kcal = when (rawKcal) {
                is Number -> rawKcal.toDouble().roundToInt()
                is String -> rawKcal.replace(',', '.').toDoubleOrNull()?.roundToInt() ?: 0
                else -> 0
            }.coerceAtLeast(0)
            val edible = parsed.optBoolean("edible", kcal > 0)

            Result.success(
                ComponentCaloriesEstimate(
                    kcal = if (edible) kcal else 0,
                    isEdible = edible,
                ),
            )
        } catch (error: Throwable) {
            Result.failure(error.toUserFacingAnalyzerError(language))
        }
    }

    private fun JSONArray.toMealItems(language: AppLanguage): List<MealItem> {
        return buildList {
            for (index in 0 until length()) {
                val obj = optJSONObject(index) ?: continue
                val rawName = obj.optString("name")
                val (normalizedName, extractedWeight) = extractNameAndWeight(rawName)
                val gramsFromJson = obj.optInt("grams").coerceAtLeast(0)
                val normalizedGrams = when {
                    gramsFromJson > 0 -> gramsFromJson
                    extractedWeight != null -> extractedWeight
                    else -> 0
                }
                add(
                    MealItem(
                        name = normalizedName.ifBlank {
                            tr(
                                language,
                                "Item ${index + 1}",
                                "Компонент ${index + 1}",
                                "Компонент ${index + 1}",
                            )
                        },
                        grams = normalizedGrams,
                        kcal = obj.optInt("kcal").coerceAtLeast(0),
                        confidence = "High",
                    ),
                )
            }
        }
    }

    private val weightInParenthesesRegex = Regex("""\(\s*(\d+(?:[.,]\d+)?)\s*[gг]\s*\)""", RegexOption.IGNORE_CASE)
    private val weightSuffixRegex = Regex("""\b(\d+(?:[.,]\d+)?)\s*[gг]\b""", RegexOption.IGNORE_CASE)

    private fun extractNameAndWeight(rawName: String): Pair<String, Int?> {
        var cleanedName = rawName.trim()
        var extractedWeight: Int? = null

        val parenthesizedWeight = weightInParenthesesRegex.find(cleanedName)
        if (parenthesizedWeight != null) {
            extractedWeight = parenthesizedWeight.groupValues.getOrNull(1)
                ?.replace(',', '.')
                ?.toDoubleOrNull()
                ?.roundToInt()
                ?.coerceAtLeast(0)
            cleanedName = cleanedName.replace(weightInParenthesesRegex, " ")
        }

        if (extractedWeight == null) {
            val suffixWeight = weightSuffixRegex.find(cleanedName)
            if (suffixWeight != null) {
                extractedWeight = suffixWeight.groupValues.getOrNull(1)
                    ?.replace(',', '.')
                    ?.toDoubleOrNull()
                    ?.roundToInt()
                    ?.coerceAtLeast(0)
                cleanedName = cleanedName.replace(weightSuffixRegex, " ")
            }
        }

        val normalizedName = cleanedName
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', '-', '—')

        return normalizedName to extractedWeight
    }

    private fun extractAssistantContent(rawResponse: String): String {
        val root = JSONObject(rawResponse)
        val choice = root.optJSONArray("choices")?.optJSONObject(0)
            ?: error("No assistant response from OpenAI")
        val message = choice.optJSONObject("message")
            ?: error("No message payload in OpenAI response")
        return when (val content = message.opt("content")) {
            is String -> content
            is JSONArray -> {
                val text = content.optJSONObject(0)?.optString("text")
                text?.takeIf { it.isNotBlank() } ?: error("OpenAI returned empty content")
            }
            else -> error("Unsupported OpenAI response format")
        }
    }

    private fun cleanJson(input: String): String {
        return input
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun Throwable.toUserFacingAnalyzerError(language: AppLanguage): Throwable {
        if (isNoInternetConnectionError()) {
            return IllegalStateException(
                tr(
                    language,
                    "No Internet Connection. Please check your internet and try again.",
                    "Нет подключения к интернету. Проверьте интернет и попробуйте снова.",
                    "Немає підключення до інтернету. Перевірте інтернет і спробуйте ще раз.",
                ),
            )
        }
        if (this is SocketTimeoutException) {
            return IllegalStateException(
                tr(
                    language,
                    "Request timed out. Please check your internet and try again.",
                    "Время ожидания запроса истекло. Проверьте интернет и попробуйте снова.",
                    "Час очікування запиту вичерпано. Перевірте інтернет і спробуйте ще раз.",
                ),
            )
        }
        if (this is IllegalArgumentException || this is IllegalStateException) {
            return this
        }
        return IllegalStateException(
            message?.takeIf { it.isNotBlank() } ?: tr(
                language,
                "Failed to analyze meal. Try again.",
                "Не удалось проанализировать прием пищи. Попробуйте снова.",
                "Не вдалося проаналізувати прийом їжі. Спробуйте ще раз.",
            ),
        )
    }

    private fun Throwable.isNoInternetConnectionError(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (
                current is UnknownHostException ||
                current is ConnectException ||
                current is NoRouteToHostException ||
                current is SocketException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun tr(language: AppLanguage, en: String, ru: String, uk: String): String = when (language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.RUSSIAN -> ru
        AppLanguage.UKRAINIAN -> uk
    }
}
