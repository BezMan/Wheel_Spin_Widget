package com.bez.spinwheel_sdk.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bez.spinwheel_sdk.R
import com.bez.spinwheel_sdk.SpinSource
import com.bez.spinwheel_sdk.SpinWheelSdk
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.domain.model.WheelResult
import com.bez.spinwheel_sdk.data.work.ConfigSyncWorker
import com.bez.spinwheel_sdk.presentation.widget.WidgetState
import com.bez.spinwheel_sdk.presentation.widget.updateAllWidgets
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Entry-point composable used by [SpinActivity]. */
@Composable
internal fun SpinScreen(onDismiss: () -> Unit) {
    val vm: SpinViewModel = hiltViewModel()
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // WidgetState is the single source of truth for resting angle, shared with the widget.
    val widgetState = remember { WidgetState(context) }

    var isSpinning by remember { mutableStateOf(false) }
    // Seed from persisted angle so the wheel opens at whatever position was last saved
    // (either by a previous in-app spin or by the home-screen widget animation).
    val wheelAngle = remember { Animatable(widgetState.getRotation()) }

    val sdkState by SpinWheelSdk.spinState.collectAsState()

    // When the activity returns to foreground, snap instantly to the latest saved angle.
    // snapTo() completes synchronously with no animation — no flag or listener needed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!isSpinning) {
            scope.launch { wheelAngle.snapTo(widgetState.getRotation()) }
        }
    }

    // Push live app angle into the SDK state on every animation frame so all
    // observers (debug card, external collectors) see a continuously updated angle.
    LaunchedEffect(isSpinning) {
        if (!isSpinning) return@LaunchedEffect
        snapshotFlow { wheelAngle.value }.collect { angle ->
            SpinWheelSdk.onAppAngleChanged(angle)
        }
    }

    // Mirror live widget angle into wheelAngle on every SDK frame emission.
    // Only active when the widget is spinning and the app is not — so a local
    // in-app spin is never interrupted.
    LaunchedEffect(Unit) {
        SpinWheelSdk.spinState.collect { state ->
            if (state.isSpinning && state.lastSpinSource == SpinSource.WIDGET && !isSpinning) {
                wheelAngle.snapTo(state.currentAngle)
            }
        }
    }


    fun triggerSpin() {
        val spins = (rotation.minimumSpins..rotation.maximumSpins).random()
        val target = wheelAngle.value + spins * 360f + Random.nextFloat() * 360f
        scope.launch {
            isSpinning = true
            SpinWheelSdk.onAppSpinStarted()
            wheelAngle.animateTo(target, tween(durationMillis = duration, easing = FastOutSlowInEasing))
            isSpinning = false
            val finalAngle = wheelAngle.value % 360f
            widgetState.setRotation(finalAngle)
            SpinWheelSdk.onAppSpinCompleted(finalAngle)
            launch(Dispatchers.IO) { updateAllWidgets(context) }
        }
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
                        .graphicsLayer { rotationZ = wheelAngle.value }
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── Config push simulation ────────────────────────────────
            Button(
                onClick = {
                    WorkManager.getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<ConfigSyncWorker>().build())
                },
                enabled = !isSpinning
            ) {
                Text("Simulate Config Push (FCM)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── SDK debug card ────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SDK State",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Spinning:  ${sdkState.isSpinning}")
                    Text("Angle:     ${"%.1f".format(sdkState.currentAngle % 360f)}°")
                    Text("Source:    ${sdkState.lastSpinSource}")
                    Text("Config:    ${sdkState.activeConfig?.name ?: "none"}")
                    Text("Duration:  ${sdkState.activeConfig?.wheel?.rotation?.duration ?: "-"} ms")
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
