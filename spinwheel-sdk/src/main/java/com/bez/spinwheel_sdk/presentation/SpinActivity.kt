package com.bez.spinwheel_sdk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main (and only) activity of the demo app.
 * Hosts the spin wheel UI, the SDK state debug card, and the config-push simulation button.
 * The widget spins in-place via [com.bez.spinwheel_sdk.presentation.widget.SpinAnimationWorker].
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
