package com.bez.spinwheel_sdk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Transparent full-screen activity that plays the in-app spin animation.
 * Launched from [com.example.wheelspinwidget.MainActivity] via the "Open Spin Wheel" button.
 * The widget spins in-place via [com.bez.spinwheel_sdk.presentation.widget.SpinAnimationWorker];
 * this activity is a separate demo entry-point for the Compose UI.
 */
class SpinActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpinScreen(onDismiss = { finish() })
        }
    }
}
