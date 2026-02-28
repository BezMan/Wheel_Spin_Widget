package com.bez.spinwheel_sdk

import android.content.Context
import android.content.SharedPreferences
import com.bez.spinwheel_sdk.data.prefs.ConfigPrefs
import com.bez.spinwheel_sdk.domain.model.WheelConfig
import com.bez.spinwheel_sdk.presentation.widget.WidgetState
import com.bez.spinwheel_sdk.presentation.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private var appContext: Context? = null
    private var sdkScope: CoroutineScope? = null
    private var rotationListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /**
     * Initialises the SDK. Safe to call multiple times — subsequent calls are no-ops.
     * Seeds [spinState] from persisted SharedPreferences so state survives process restarts.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val appCtx = context.applicationContext
        appContext = appCtx
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val widgetState = WidgetState(appCtx)
        val config = ConfigPrefs(appCtx).load()

        _spinState.value = SpinWheelState(
            isSpinning = false,   // never inherit a stale spinning flag on cold start
            currentAngle = widgetState.getRotation(),
            activeConfig = config,
            lastSpinSource = SpinSource.NONE
        )

        // Clear any stale app-spinning flag left by a previous crash.
        widgetState.setAppSpinning(false)

        // Observe rotation changes in SharedPreferences. Fires when setRotation() is called
        // (at app or widget spin end), independently of the Compose frame clock.
        // Guard: skip during active animations to avoid competing with per-frame updates.
        val prefs = appCtx.getSharedPreferences(WidgetState.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == WidgetState.KEY_ROTATION) {
                val state = WidgetState(appCtx)
                if (!state.isSpinning() && !state.isAppSpinning()) {
                    sdkScope?.launch(Dispatchers.IO) { updateAllWidgets(appCtx) }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        rotationListener = listener
    }

    /**
     * Tears down the SDK. Resets [spinState] to defaults.
     * Call from [android.app.Application.onTerminate] (or equivalent lifecycle hook).
     */
    fun destroy() {
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(WidgetState.PREFS_NAME, Context.MODE_PRIVATE)
            rotationListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        }
        rotationListener = null
        sdkScope?.cancel()
        sdkScope = null
        appContext = null
        _spinState.value = SpinWheelState()
        initialized = false
    }

    // ── Internal update hooks ─────────────────────────────────────────────────

    internal fun onAppSpinStarted() {
        _spinState.update { it.copy(isSpinning = true, lastSpinSource = SpinSource.APP) }
        // Mark widget so it shows a placeholder immediately.
        appContext?.let { ctx ->
            WidgetState(ctx).setAppSpinning(true)
            sdkScope?.launch(Dispatchers.IO) { updateAllWidgets(ctx) }
        }
    }

    internal fun onAppAngleChanged(angle: Float) {
        _spinState.update { it.copy(currentAngle = angle) }
    }

    internal fun onAppSpinCompleted(angle: Float) {
        _spinState.update { it.copy(isSpinning = false, currentAngle = angle) }
        // Clear placeholder — widget will be redrawn by the rotation prefs listener.
        appContext?.let { ctx -> WidgetState(ctx).setAppSpinning(false) }
    }

    internal fun onWidgetSpinStarted() {
        _spinState.update { it.copy(isSpinning = true, lastSpinSource = SpinSource.WIDGET) }
    }

    internal fun onWidgetAngleChanged(angle: Float) {
        _spinState.update { it.copy(currentAngle = angle) }
    }

    internal fun onWidgetSpinCompleted(angle: Float) {
        _spinState.update { it.copy(isSpinning = false, currentAngle = angle) }
    }

    internal fun onConfigUpdated(config: WheelConfig) {
        _spinState.update { it.copy(activeConfig = config) }
    }
}
