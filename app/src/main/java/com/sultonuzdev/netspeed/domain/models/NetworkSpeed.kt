package com.sultonuzdev.netspeed.domain.models


data class NetworkSpeed(
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)