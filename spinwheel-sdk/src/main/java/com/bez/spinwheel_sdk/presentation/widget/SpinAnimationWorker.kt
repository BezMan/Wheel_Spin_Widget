package com.bez.spinwheel_sdk.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

/**
 * Runs the in-widget spin animation frame by frame.
 *
 * Uses [AppWidgetManager.updateAppWidget] directly (no Glance intermediary) so every
 * RemoteViews push is guaranteed to reach the launcher synchronously.
 * Runs in a [CoroutineWorker] to avoid the BroadcastReceiver goAsync() 10-second timeout.
 */
class SpinAnimationWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val state = WidgetState(context)

        // Guard — a previous worker may still be running.
        if (state.isSpinning()) return Result.success()

        val config = ConfigPrefs(context).load() ?: return Result.failure()
        val rotation = config.wheel.rotation
        val duration = rotation.duration.coerceIn(1000, 5000)
        val delta = (rotation.minimumSpins..rotation.maximumSpins).random() * 360f +
                Random.nextFloat() * 360f

        val startAngle = state.getRotation()
        val frameInterval = 50L          // ~20 fps
        val frames = (duration / frameInterval).toInt().coerceAtLeast(1)

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, SpinWheelWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return Result.success()

        // Decode wheel source once — every frame reuses this bitmap for rotation.
        val wheelSrc = decodeBitmap(context, R.drawable.wheel)

        state.setSpinning(true)
        ids.forEach { updateWidget(context, manager, it) }

        try {
            for (frame in 1..frames) {
                val frameStart = System.currentTimeMillis()
                val angle = startAngle + delta * easeInOutCubic(frame.toFloat() / frames)
                state.setRotation(angle)

                // Only push the wheel layer — avoids re-decoding the 3 static assets each frame.
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setImageViewBitmap(R.id.widget_wheel, rotatedBitmap(wheelSrc, angle))
                ids.forEach { manager.partiallyUpdateAppWidget(it, views) }

                // Subtract frame processing time so actual cadence stays close to frameInterval.
                val remaining = frameInterval - (System.currentTimeMillis() - frameStart)
                if (frame < frames && remaining > 0) delay(remaining)
            }
            state.setRotation((startAngle + delta) % 360f)
        } finally {
            // Runs even on CancellationException so the widget never stays permanently locked.
            state.setSpinning(false)
            ids.forEach { updateWidget(context, manager, it) }
        }

        return Result.success()
    }

    private fun easeInOutCubic(t: Float): Float =
        if (t < 0.5f) 4f * t * t * t
        else 1f - (-2f * t + 2f).pow(3) / 2f
}
