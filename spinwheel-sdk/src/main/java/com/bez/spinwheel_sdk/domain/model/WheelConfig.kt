package com.bez.spinwheel_sdk.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Top-level shape of config.json / the remote config endpoint response. */
@Serializable
data class ConfigResponse(
    val data: List<WheelConfig>,
    val meta: ConfigMeta
)

@Serializable
data class ConfigMeta(
    val version: Int,
    val copyright: String
)

@Serializable
data class WheelConfig(
    val id: String,
    val name: String,
    val type: String,
    val network: NetworkConfig,
    val wheel: WheelDefinition,
    val extras: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class NetworkConfig(
    val attributes: NetworkAttributes,
    val assets: NetworkAssets
)

@Serializable
data class NetworkAttributes(
    val refreshInterval: Int,
    val networkTimeout: Int,
    val retryAttempts: Int,
    val cacheExpiration: Int,
    val debugMode: Boolean
)

@Serializable
data class NetworkAssets(
    val host: String
)

@Serializable
data class WheelDefinition(
    val rotation: RotationConfig,
    val assets: WheelAssets
)

@Serializable
data class RotationConfig(
    val duration: Int,
    val minimumSpins: Int,
    val maximumSpins: Int,
    val spinEasing: String
)

@Serializable
data class WheelAssets(
    val bg: String,
    val wheelFrame: String,
    val wheelSpin: String,
    val wheel: String
)
