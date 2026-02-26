package com.bez.spinwheel_sdk.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.presentation.SpinActivity

/**
 * Glance-based home-screen widget.
 * Shows the wheel graphic; tapping it launches [SpinActivity] for the spin animation.
 */
class SpinWheelWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(Intent(context, SpinActivity::class.java))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.wheel),
                contentDescription = "Spin the wheel"
            )
        }
    }
}

/** Registered in AndroidManifest; routes APPWIDGET_UPDATE broadcasts to [SpinWheelWidget]. */
class SpinWheelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpinWheelWidget()
}
