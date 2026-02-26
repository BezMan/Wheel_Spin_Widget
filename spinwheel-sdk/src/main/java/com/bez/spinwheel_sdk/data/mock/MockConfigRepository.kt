package com.bez.spinwheel_sdk.data.mock

import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.AssetsConfig
import com.bez.spinwheel_sdk.domain.model.RotationConfig
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MockConfigRepository(private val prefs: ConfigPrefs) : ConfigRepository {

    // Seed from SharedPreferences so returning users see their last config immediately.
    private val _config = MutableStateFlow<WheelResult<WheelConfig>>(
        prefs.load()?.let { WheelResult.Success(it) } ?: WheelResult.Loading
    )

    override fun observeConfig(): Flow<WheelResult<WheelConfig>> = _config.asStateFlow()

    override suspend fun fetchConfig(): WheelResult<WheelConfig> {
        val config = CONFIG_POOL.random()
        prefs.save(config)
        val result = WheelResult.Success(config)
        _config.value = result
        return result
    }

    companion object {
        /** Exposed so [ConfigSyncWorker] can pick a random config without a full repository. */
        fun randomConfig(): WheelConfig = CONFIG_POOL.random()

        private val ASSETS = AssetsConfig(
            bg = "bg.jpeg",
            wheelFrame = "wheel-frame.png",
            wheelSpin = "wheel-spin.png",
            wheel = "wheel.png"
        )

        private val CONFIG_POOL = listOf(
            WheelConfig(
                id = "wheel_minimal",
                rotation = RotationConfig(
                    duration = 2000,
                    minimumSpins = 3,
                    maximumSpins = 5,
                    spinEasing = "easeInOutCubic"
                ),
                assets = ASSETS
            ),
            WheelConfig(
                id = "wheel_fast",
                rotation = RotationConfig(
                    duration = 1500,
                    minimumSpins = 5,
                    maximumSpins = 8,
                    spinEasing = "easeInOutCubic"
                ),
                assets = ASSETS
            ),
            WheelConfig(
                id = "wheel_slow",
                rotation = RotationConfig(
                    duration = 4000,
                    minimumSpins = 2,
                    maximumSpins = 3,
                    spinEasing = "easeInOutCubic"
                ),
                assets = ASSETS
            )
        )
    }
}
