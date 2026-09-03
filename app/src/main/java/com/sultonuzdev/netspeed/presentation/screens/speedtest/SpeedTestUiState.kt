package com.sultonuzdev.netspeed.presentation.screens.speedtest

import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult

data class SpeedTestUiState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,

    /** Live throughput driving the gauge while a phase is running. */
    val liveBytesPerSecond: Double = 0.0,
    val liveValue: String = "0",
    val liveUnit: String = "Mbps",

    val downloadResult: String = "—",
    val uploadResult: String = "—",
    val pingResult: String = "—",
    val jitterResult: String = "—",

    val history: List<SpeedTestResult> = emptyList(),
    val error: String? = null
) {
    val isRunning: Boolean
        get() = phase == SpeedTestPhase.PINGING ||
                phase == SpeedTestPhase.DOWNLOADING ||
                phase == SpeedTestPhase.UPLOADING

    val phaseLabel: String
        get() = when (phase) {
            SpeedTestPhase.IDLE -> "Ready"
            SpeedTestPhase.PINGING -> "Latency"
            SpeedTestPhase.DOWNLOADING -> "Download"
            SpeedTestPhase.UPLOADING -> "Upload"
            SpeedTestPhase.DONE -> "Done"
            SpeedTestPhase.FAILED -> "Failed"
        }
}
