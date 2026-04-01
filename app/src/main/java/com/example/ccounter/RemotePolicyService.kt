package com.example.ccounter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object RemotePolicyService {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Serializable
    private data class DeviceRegistry(
        @SerialName("openAiApiKeyBase63NoZ") val openAiApiKeyBase63NoZ: String = "",
        val devices: List<RemoteDeviceRecord> = emptyList(),
    )

    @Serializable
    private data class RemoteDeviceRecord(
        @SerialName("deviceId") val deviceId: String = "",
        @SerialName("profileName") val profileName: String = "",
        @SerialName("language") val language: String = "",
        @SerialName("accessEnabled") val accessEnabled: Boolean = false,
        @SerialName("paused") val paused: Boolean = false,
        @SerialName("expiresAtUtc") val expiresAtUtc: String? = null,
        @SerialName("createdAtUtc") val createdAtUtc: String? = null,
        @SerialName("updatedAtUtc") val updatedAtUtc: String? = null,
    )

    data class AccessDecision(
        val status: DeviceAccessStatus,
        val expiresAtUtcMillis: Long?,
        val message: String,
        val checkedAtUtcMillis: Long,
        val openAiApiKeyBase63NoZ: String,
    )

    suspend fun fetchNetworkUtcNowMillis(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/zen")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to get network time. HTTP ${response.code}")
                }
                val dateHeader = response.header("Date")
                    ?: error("Missing Date header in network time response.")
                ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
            }
        }
    }

    suspend fun fetchAccessDecision(deviceId: String): Result<AccessDecision> = withContext(Dispatchers.IO) {
        runCatching {
            val nowUtcMillis = fetchNetworkUtcNowMillis()
                .getOrElse { throw it }

            val url = BuildConfig.ACCESS_CONTROL_URL
            if (url.isBlank()) error("Access control URL is not configured.")

            val request = Request.Builder()
                .url(url)
                .addHeader("Cache-Control", "no-cache")
                .get()
                .build()

            val bodyText = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to fetch access list. HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }

            val registry = json.decodeFromString<DeviceRegistry>(bodyText)
            val record = registry.devices.firstOrNull { it.deviceId.equals(deviceId, ignoreCase = true) }

            if (record == null) {
                return@runCatching AccessDecision(
                    status = DeviceAccessStatus.PENDING,
                    expiresAtUtcMillis = null,
                    message = "Device is pending approval in admin panel.",
                    checkedAtUtcMillis = nowUtcMillis,
                    openAiApiKeyBase63NoZ = registry.openAiApiKeyBase63NoZ.trim(),
                )
            }

            val expiresAtMillis = parseIsoUtc(record.expiresAtUtc)
            val status = when {
                !record.accessEnabled -> DeviceAccessStatus.PENDING
                record.paused -> DeviceAccessStatus.PAUSED
                expiresAtMillis != null && nowUtcMillis >= expiresAtMillis -> DeviceAccessStatus.EXPIRED
                else -> DeviceAccessStatus.ACTIVE
            }

            val message = when (status) {
                DeviceAccessStatus.ACTIVE -> {
                    if (expiresAtMillis != null) {
                        val expiry = Instant.ofEpochMilli(expiresAtMillis).atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
                        "Access enabled. Valid until $expiry."
                    } else {
                        "Access enabled."
                    }
                }
                DeviceAccessStatus.PENDING -> "Access is not enabled yet. Please approve this device in admin panel."
                DeviceAccessStatus.PAUSED -> "Access is paused by admin."
                DeviceAccessStatus.EXPIRED -> "Access period ended. Ask admin to extend the expiration date."
            }

            AccessDecision(
                status = status,
                expiresAtUtcMillis = expiresAtMillis,
                message = message,
                checkedAtUtcMillis = nowUtcMillis,
                openAiApiKeyBase63NoZ = registry.openAiApiKeyBase63NoZ.trim(),
            )
        }
    }

    suspend fun submitDeviceRegistration(
        deviceId: String,
        profileName: String,
        language: AppLanguage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = BuildConfig.GITHUB_REGISTRATION_TOKEN.trim()
            if (token.isBlank()) {
                error("Registration bridge token is not configured.")
            }
            val owner = BuildConfig.GITHUB_REPO_OWNER.trim()
            val repo = BuildConfig.GITHUB_REPO_NAME.trim()
            if (owner.isBlank() || repo.isBlank()) {
                error("GitHub repository is not configured.")
            }

            val issueTitle = "[Device Registration] $deviceId"

            val existingRequest = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/issues?state=open&labels=device-registration&per_page=100")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github+json")
                .get()
                .build()

            val hasExistingIssue = client.newCall(existingRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    false
                } else {
                    val array = runCatching { JSONArray(response.body?.string().orEmpty()) }.getOrElse { JSONArray() }
                    (0 until array.length()).any { idx ->
                        array.optJSONObject(idx)?.optString("title").orEmpty().contains(deviceId, ignoreCase = true)
                    }
                }
            }
            if (hasExistingIssue) return@runCatching Unit

            val nowUtc = fetchNetworkUtcNowMillis().getOrElse { throw it }
            val payload = JSONObject()
                .put("title", issueTitle)
                .put(
                    "body",
                    buildString {
                        appendLine("Device ID: $deviceId")
                        appendLine("Profile Name: ${profileName.ifBlank { "User" }}")
                        appendLine("Language: ${language.label()}")
                        appendLine("Registered At (UTC): ${Instant.ofEpochMilli(nowUtc)}")
                        appendLine()
                        appendLine("Requested from mobile app.")
                    },
                )
                .put("labels", JSONArray().put("device-registration"))

            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/issues")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github+json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val err = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty()
                    error("Failed to submit device registration. HTTP ${response.code}. $err")
                }
            }
        }
    }

    private fun parseIsoUtc(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }
}
