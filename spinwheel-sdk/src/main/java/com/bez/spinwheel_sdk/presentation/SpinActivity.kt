package com.bez.spinwheel_sdk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Transparent full-screen activity that plays the spin animation.
 * Launched via PendingIntent from the home-screen widget.
 * Finishes automatically when the animation completes.
 */
class SpinActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpinScreen(onDismiss = { finish() })
        }
    }
}
