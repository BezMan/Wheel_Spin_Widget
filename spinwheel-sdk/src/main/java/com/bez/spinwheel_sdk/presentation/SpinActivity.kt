package com.bez.spinwheel_sdk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

/**
 * SDK-provided activity that hosts the spin wheel Compose UI, Launched by the consumer app.
 * The widget spins independently in-place via [com.bez.spinwheel_sdk.presentation.widget.SpinAnimationWorker].
 */
@AndroidEntryPoint
class SpinActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpinScreen(onDismiss = { finish() })
        }
    }
}
