package com.bez.spinwheel_sdk.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import kotlin.random.Random

/** Entry-point composable used by [SpinActivity]. */
@Composable
internal fun SpinScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: SpinViewModel = viewModel(
        factory = SpinViewModel.Factory(MockConfigRepository(context.applicationContext))
    )
    val state by vm.config.collectAsState()

    LaunchedEffect(Unit) {
        if (state is WheelResult.Loading) vm.refresh()
    }

    when (val s = state) {
        is WheelResult.Loading -> LoadingOverlay()
        is WheelResult.Success -> SpinWheelScreen(config = s.data)
        is WheelResult.Error -> LaunchedEffect(s) { onDismiss() }
    }
}

@Composable
private fun SpinWheelScreen(config: WheelConfig) {
    // Clamp duration to the advertised [1000, 5000] ms range.
    val duration = config.wheel.rotation.duration.coerceIn(1000, 5000)
    val rotation = config.wheel.rotation

    var isSpinning by remember { mutableStateOf(false) }
    var accumulated by remember { mutableFloatStateOf(0f) }

    val wheelAngle by animateFloatAsState(
        targetValue = accumulated,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        finishedListener = { isSpinning = false },
        label = "wheel_rotation"
    )

    fun triggerSpin() {
        val spins = (rotation.minimumSpins..rotation.maximumSpins).random()
        accumulated += spins * 360f + Random.nextFloat() * 360f
        isSpinning = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: background ─────────────────────────────────────
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // ── Layer 2: wheel — rotates ─────────────────────────
                Image(
                    painter = painterResource(R.drawable.wheel),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = wheelAngle }
                )

                // ── Layer 3: frame — static overlay ──────────────────
                Image(
                    painter = painterResource(R.drawable.wheel_frame),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                // ── Layer 4: spin button — disabled while spinning ────
                Image(
                    painter = painterResource(R.drawable.wheel_spin),
                    contentDescription = if (isSpinning) null else "Tap to spin",
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(if (isSpinning) 0.35f else 1f)
                        .clickable(enabled = !isSpinning) { triggerSpin() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Duration badge ────────────────────────────────────────
            Text(
                text = "Spin duration: ${duration} ms",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
