package com.sultonuzdev.netspeed.presentation.screens.speed

data class SpeedUiState(
    val downloadSpeed: String = "0",
    val downloadUnit: String = "MB/s",
    val uploadSpeed: String = "0",
    val uploadUnit: String = "MB/s",
    val ping: String = "N/A",
    val peakDownload: String = "0 B/s",
    val peakUpload: String = "0 B/s",
    val peakDownloadValue: Double = 0.0,
    val peakUploadValue: Double = 0.0,
    val sessionTime: String = "0s",
    val sessionStartTime: Long = System.currentTimeMillis() / 1000,
    val isConnected: Boolean = false,
    val networkType: String = "NONE",
    val networkName: String = "No Connection",
    val signalStrength: Int = 0,

    // Data usage fields
    val todayWifiUsage: String = "0 B",
    val todayMobileUsage: String = "0 B",
    val todayTotalUsage: String = "0 B",
    val wifiProgress: Float = 0f,
    val mobileProgress: Float = 0f,
    val totalProgress: Float = 0f
)