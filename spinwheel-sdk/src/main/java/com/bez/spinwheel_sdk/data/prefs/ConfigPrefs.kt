package com.bez.spinwheel_sdk.data.prefs

import android.content.Context
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class ConfigPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(config: WheelConfig) {
        prefs.edit()
            .putString(KEY_CONFIG, Json.encodeToString(config))
            .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
            .apply()
    }

    fun load(): WheelConfig? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return runCatching { Json.decodeFromString<WheelConfig>(json) }.getOrNull()
    }

    fun lastFetchMillis(): Long = prefs.getLong(KEY_LAST_FETCH, 0L)

    companion object {
        private const val PREFS_NAME = "spinwheel_config"
        private const val KEY_CONFIG = "config_json"
        private const val KEY_LAST_FETCH = "last_fetch_ms"
    }
}
