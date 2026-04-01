package com.example.ccounter

import android.content.Context
import kotlinx.serialization.json.Json

class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences("ccounter_store", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    fun load(): AppData {
        val raw = prefs.getString(KEY_DATA, null) ?: return AppData()
        val parsed = runCatching { json.decodeFromString<AppData>(raw) }.getOrElse { AppData() }
        return migrateLegacySeedData(parsed)
    }

    fun save(data: AppData) {
        prefs.edit().putString(KEY_DATA, json.encodeToString(data)).apply()
    }

    companion object {
        private const val KEY_DATA = "app_data"
        private val LEGACY_SEED_MEAL_NAMES = setOf(
            "Oatmeal with berries",
            "Grilled chicken salad",
            "Greek yogurt",
            "Salmon with rice",
        )
    }

    private fun migrateLegacySeedData(data: AppData): AppData {
        val hasLegacyMeals = data.meals.size == 4 && data.meals.all { it.name in LEGACY_SEED_MEAL_NAMES }
        if (!hasLegacyMeals) return data
        val migrated = data.copy(meals = emptyList())
        save(migrated)
        return migrated
    }
}
