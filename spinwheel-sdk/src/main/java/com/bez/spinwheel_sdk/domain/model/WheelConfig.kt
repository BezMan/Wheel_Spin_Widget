package com.bez.spinwheel_sdk.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WheelConfig(
    val id: String,
    val rotation: RotationConfig,
    val assets: AssetsConfig,
    val extras: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class RotationConfig(
    val duration: Int,
    val minimumSpins: Int,
    val maximumSpins: Int,
    val spinEasing: String
)

@Serializable
data class AssetsConfig(
    val bg: String,
    val wheelFrame: String,
    val wheelSpin: String,
    val wheel: String
)
