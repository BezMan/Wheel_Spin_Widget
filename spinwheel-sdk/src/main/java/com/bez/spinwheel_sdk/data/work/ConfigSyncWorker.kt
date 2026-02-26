package com.bez.spinwheel_sdk.data.work

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.presentation.widget.SpinWheelWidget

/**
 * Simulates an FCM-triggered config push.
 * Picks a random variant from the mock pool, persists it, and refreshes all widget instances.
 */
class ConfigSyncWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = MockConfigRepository.randomConfig()
        ConfigPrefs(appContext).save(config)

        // Notify every pinned widget instance to re-render.
        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = manager.getGlanceIds(SpinWheelWidget::class.java)
        glanceIds.forEach { id -> SpinWheelWidget().update(appContext, id) }

        return Result.success()
    }
}
