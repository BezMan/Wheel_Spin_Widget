package com.bez.spinwheel_sdk.data.remote

import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient

/**
 * Stub implementation backed by OkHttp.
 * Wire up [baseUrl] and replace [fetchConfig] with a real HTTP call when a remote is available.
 */
internal class RemoteConfigRepository(
    @Suppress("unused") private val client: OkHttpClient,
    @Suppress("unused") private val prefs: ConfigPrefs,
    @Suppress("unused") private val baseUrl: String = ""
) : ConfigRepository {

    override fun observeConfig(): Flow<WheelResult<WheelConfig>> = flow {
        emit(WheelResult.Error(UnsupportedOperationException("Remote config not yet implemented")))
    }

    override suspend fun fetchConfig(): WheelResult<WheelConfig> =
        WheelResult.Error(UnsupportedOperationException("Remote config not yet implemented"))
}
