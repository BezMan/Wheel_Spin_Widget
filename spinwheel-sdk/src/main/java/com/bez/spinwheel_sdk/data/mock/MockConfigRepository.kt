package com.bez.spinwheel_sdk.data.mock

import android.content.Context
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.ConfigResponse
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

internal class MockConfigRepository(
    private val context: Context,
    private val prefs: ConfigPrefs = ConfigPrefs(context)
) : ConfigRepository {

    /**
     * Parse config.json from assets. The file contains multiple items in `data[]` —
     * each with a different rotation.duration — so the mock "FCM push" feels dynamic.
     */
    private val configPool: List<WheelConfig> by lazy {
        val raw = context.assets.open("config.json").bufferedReader().use { it.readText() }
        json.decodeFromString<ConfigResponse>(raw).data
    }

    // Seed from SharedPreferences so a returning user skips the Loading state.
    private val _config = MutableStateFlow<WheelResult<WheelConfig>>(
        prefs.load()?.let { WheelResult.Success(it) } ?: WheelResult.Loading
    )

    override fun observeConfig(): Flow<WheelResult<WheelConfig>> = _config.asStateFlow()

    override suspend fun fetchConfig(): WheelResult<WheelConfig> {
        val config = configPool.random()
        prefs.save(config)
        val result = WheelResult.Success(config)
        _config.value = result
        return result
    }
}
