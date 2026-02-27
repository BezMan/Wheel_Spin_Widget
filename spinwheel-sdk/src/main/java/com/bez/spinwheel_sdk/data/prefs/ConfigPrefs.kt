package com.bez.spinwheel_sdk.data.prefs

import android.content.Context
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

private val lenientJson = Json { ignoreUnknownKeys = true }

@Singleton
internal class ConfigPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(config: WheelConfig) {
        prefs.edit {
            putString(KEY_CONFIG, Json.encodeToString(config))
                .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
        }
    }

    fun load(): WheelConfig? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return runCatching { lenientJson.decodeFromString<WheelConfig>(json) }.getOrNull()
    }

    fun lastFetchMillis(): Long = prefs.getLong(KEY_LAST_FETCH, 0L)

    companion object {
        private const val PREFS_NAME = "spinwheel_config"
        private const val KEY_CONFIG = "config_json"
        private const val KEY_LAST_FETCH = "last_fetch_ms"
    }
}
