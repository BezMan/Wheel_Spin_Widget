package com.bez.spinwheel_sdk.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bez.spinwheel_sdk.SpinWheelSdk
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import com.bez.spinwheel_sdk.presentation.widget.updateAllWidgets
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Simulates an FCM-triggered config push.
 * Picks a random variant from config.json, persists it, and refreshes all widget instances.
 */
@HiltWorker
class ConfigSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: ConfigRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = repo.fetchConfig()
        if (result is WheelResult.Success) SpinWheelSdk.onConfigUpdated(result.data)
        updateAllWidgets(appContext)
        return Result.success()
    }
}
