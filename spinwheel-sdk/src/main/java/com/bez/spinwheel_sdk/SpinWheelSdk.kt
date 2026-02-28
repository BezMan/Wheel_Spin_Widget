package com.bez.spinwheel_sdk

import android.content.Context
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.presentation.widget.WidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Entry point for the SpinWheel SDK.
 *
 * Usage:
 * ```
 * // In Application.onCreate():
 * SpinWheelSdk.init(this)
 *
 * // Anywhere in the app:
 * SpinWheelSdk.spinState.collect { state -> ... }
 *
 * // In Application.onTerminate():
 * SpinWheelSdk.destroy()
 * ```
 *
 * Hilt is an internal implementation detail — consumers never see it.
 */
object SpinWheelSdk {

    private val _spinState = MutableStateFlow(SpinWheelState())

    /** Observable stream of the wheel's current state. Safe to collect from any coroutine. */
    val spinState: StateFlow<SpinWheelState> = _spinState.asStateFlow()

    private var initialized = false

    /**
     * Initialises the SDK. Safe to call multiple times — subsequent calls are no-ops.
     * Seeds [spinState] from persisted SharedPreferences so state survives process restarts.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val appCtx = context.applicationContext
        val angle = WidgetState(appCtx).getRotation()
        val config = ConfigPrefs(appCtx).load()

        _spinState.value = SpinWheelState(
            isSpinning = false,   // never inherit a stale spinning flag on cold start
            currentAngle = angle,
            activeConfig = config,
            lastSpinSource = SpinSource.NONE
        )
    }

    /**
     * Tears down the SDK. Resets [spinState] to defaults.
     * Call from [android.app.Application.onTerminate] (or equivalent lifecycle hook).
     */
    fun destroy() {
        _spinState.value = SpinWheelState()
        initialized = false
    }

    // ── Internal update hooks ─────────────────────────────────────────────────
    // These are called by SDK-internal components; they are not part of the public API.

    internal fun onAppSpinStarted() {
        _spinState.update { it.copy(isSpinning = true, lastSpinSource = SpinSource.APP) }
    }

    internal fun onAppSpinCompleted(angle: Float) {
        _spinState.update { it.copy(isSpinning = false, currentAngle = angle) }
    }

    internal fun onWidgetSpinStarted() {
        _spinState.update { it.copy(isSpinning = true, lastSpinSource = SpinSource.WIDGET) }
    }

    internal fun onWidgetSpinCompleted(angle: Float) {
        _spinState.update { it.copy(isSpinning = false, currentAngle = angle) }
    }

    internal fun onConfigUpdated(config: WheelConfig) {
        _spinState.update { it.copy(activeConfig = config) }
    }
}
