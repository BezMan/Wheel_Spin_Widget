package com.bez.spinwheel_sdk.domain.repository

import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    fun observeConfig(): Flow<WheelResult<WheelConfig>>
    suspend fun fetchConfig(): WheelResult<WheelConfig>
}
