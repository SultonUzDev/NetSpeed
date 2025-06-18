package com.sultonuzdev.netspeed.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkSpeed(
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val ping: String = "N/A",
    val timestamp: Long = System.currentTimeMillis()
)