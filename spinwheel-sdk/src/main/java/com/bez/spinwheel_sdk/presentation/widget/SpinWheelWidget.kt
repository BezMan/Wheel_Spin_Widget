package com.bez.spinwheel_sdk.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import kotlinx.coroutines.runBlocking

private const val ACTION_SPIN = "com.bez.spinwheel_sdk.ACTION_SPIN"

// ── Provider ───────────────────────────────────────────────────────────────────

class SpinWheelWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        WidgetState(context).setSpinning(false)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SPIN) return

        val state = WidgetState(context)
        if (state.isSpinning()) return

        // Seed config from assets if widget tapped before any FCM push simulation.
        if (ConfigPrefs(context).load() == null) {
            runBlocking { MockConfigRepository(context, ConfigPrefs(context)).fetchConfig() }
        }

        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<SpinAnimationWorker>().build())
    }
}

// ── Shared update helpers — also used by SpinAnimationWorker / ConfigSyncWorker ──

fun updateAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(
        ComponentName(context, SpinWheelWidgetProvider::class.java)
    )
    for (id in ids) updateWidget(context, manager, id)
}

fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
    val state = WidgetState(context)
    val isSpinning = state.isSpinning()

    val wheelBitmap = rotatedBitmap(context, R.drawable.wheel, state.getRotation())
    val spinBitmap = if (isSpinning) {
        dimmedBitmap(context, R.drawable.wheel_spin, alpha = 90)
    } else {
        decodeBitmap(context, R.drawable.wheel_spin)
    }

    val views = RemoteViews(context.packageName, R.layout.widget_layout)
    views.setImageViewBitmap(R.id.widget_wheel, wheelBitmap)
    views.setImageViewBitmap(R.id.widget_spin_btn, spinBitmap)

    // Wire up the tap — onReceive guards against re-spin while already spinning.
    val spinIntent = Intent(ACTION_SPIN).apply {
        component = ComponentName(context, SpinWheelWidgetProvider::class.java)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, spinIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    manager.updateAppWidget(appWidgetId, views)
}

// ── Bitmap helpers ─────────────────────────────────────────────────────────────

internal fun decodeBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
    return BitmapFactory.decodeResource(context.resources, resId, opts)
}

/**
 * Rotates [src] around its centre onto a canvas of the same dimensions.
 * Using Canvas.rotate keeps output size fixed regardless of angle.
 * Bitmap.createBitmap(matrix) expands the bounding box at diagonal angles,
 * which causes the image to visually pulse larger/smaller as it spins.
 */
internal fun rotatedBitmap(src: Bitmap, angleDegrees: Float): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).apply {
        rotate(angleDegrees, src.width / 2f, src.height / 2f)
        drawBitmap(src, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
    }
    return out
}

private fun rotatedBitmap(context: Context, @DrawableRes resId: Int, angleDegrees: Float): Bitmap =
    rotatedBitmap(decodeBitmap(context, resId), angleDegrees)

private fun dimmedBitmap(context: Context, @DrawableRes resId: Int, alpha: Int): Bitmap {
    val src = decodeBitmap(context, resId)
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { this.alpha = alpha })
    return out
}
