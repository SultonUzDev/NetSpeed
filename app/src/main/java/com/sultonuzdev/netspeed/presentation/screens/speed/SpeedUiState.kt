package com.sultonuzdev.netspeed.presentation.screens.speed

data class SpeedUiState(
    /** What the hero circle shows; follows the Status bar display-mode setting. */
    val heroSpeed: String = "0",
    val heroUnit: String = "MB/s",
    val heroLabel: String = "Download",
    /** Second line inside the circle, used only in the download-and-upload mode. */
    val heroSecondary: String? = null,

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

    /** Recent samples of whatever the hero shows, in bytes/sec, oldest first. */
    val recentDownload: List<Float> = emptyList()
)