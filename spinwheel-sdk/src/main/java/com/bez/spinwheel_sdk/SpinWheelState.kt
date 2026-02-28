package com.bez.spinwheel_sdk

import com.bez.spinwheel_sdk.domain.model.WheelConfig

/** Where the last spin originated. */
enum class SpinSource { NONE, APP, WIDGET }

/** Snapshot of the wheel's current state, emitted by [SpinWheelSdk.spinState]. */
data class SpinWheelState(
    val isSpinning: Boolean = false,
    val currentAngle: Float = 0f,
    val activeConfig: WheelConfig? = null,
    val lastSpinSource: SpinSource = SpinSource.NONE
)
