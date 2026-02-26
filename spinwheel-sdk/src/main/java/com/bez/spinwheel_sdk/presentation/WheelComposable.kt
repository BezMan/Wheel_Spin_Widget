package com.bez.spinwheel_sdk.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import kotlin.random.Random

/**
 * Entry-point composable for SpinActivity.
 * Wires up the ViewModel, waits for a valid config, then hands off to [SpinWheelAnimation].
 */
@Composable
internal fun SpinScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: SpinViewModel = viewModel(
        factory = SpinViewModel.Factory(
            MockConfigRepository(ConfigPrefs(context.applicationContext))
        )
    )
    val state by vm.config.collectAsState()

    // Kick off a fetch if nothing is cached yet.
    LaunchedEffect(Unit) {
        if (state is WheelResult.Loading) vm.refresh()
    }

    when (val s = state) {
        is WheelResult.Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        is WheelResult.Success -> SpinWheelAnimation(config = s.data, onFinished = onDismiss)
        is WheelResult.Error -> LaunchedEffect(s) { onDismiss() }
    }
}

@Composable
private fun SpinWheelAnimation(config: WheelConfig, onFinished: () -> Unit) {
    var spinning by remember { mutableStateOf(false) }

    // Pick a random total rotation in [minimumSpins, maximumSpins] full turns + random landing.
    val totalDegrees = remember {
        val spins = config.rotation.minimumSpins +
                Random.nextInt(config.rotation.maximumSpins - config.rotation.minimumSpins + 1)
        spins * 360f + Random.nextFloat() * 360f
    }

    val rotation by animateFloatAsState(
        targetValue = if (spinning) totalDegrees else 0f,
        animationSpec = tween(
            durationMillis = config.rotation.duration,
            easing = FastOutSlowInEasing   // maps to easeInOutCubic feel
        ),
        finishedListener = { onFinished() },
        label = "wheel_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.wheel),
            contentDescription = null,
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }

    // Start spinning on first frame.
    LaunchedEffect(Unit) { spinning = true }
}
