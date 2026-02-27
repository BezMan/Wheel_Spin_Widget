package com.bez.spinwheel_sdk.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.presentation.widget.updateAllWidgets

/**
 * Simulates an FCM-triggered config push.
 * Picks a random variant from config.json, persists it, and refreshes all widget instances.
 */
class ConfigSyncWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        MockConfigRepository(appContext).fetchConfig()
        updateAllWidgets(appContext)
        return Result.success()
    }
}
