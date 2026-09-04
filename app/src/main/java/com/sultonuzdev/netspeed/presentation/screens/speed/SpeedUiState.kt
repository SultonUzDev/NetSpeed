package com.sultonuzdev.netspeed.presentation.screens.speed

data class SpeedUiState(
    val downloadSpeed: String = "0",
    val downloadUnit: String = "MB/s",
    val uploadSpeed: String = "0",
    val uploadUnit: String = "MB/s",
    val isConnected: Boolean = false,
    val networkType: String = "NONE",
    val networkName: String = "No Connection",
    val signalStrength: Int = 0,

    /** Recent download samples in bytes/sec, oldest first. */
    val recentDownload: List<Float> = emptyList()
)