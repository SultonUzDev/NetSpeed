package com.sultonuzdev.netspeed.domain.models

/** Stage a running test is in, used to drive the gauge and the labels around it. */
enum class SpeedTestPhase {
    IDLE,
    PINGING,
    DOWNLOADING,
    UPLOADING,
    DONE,
    FAILED
}

/**
 * One completed test.
 *
 * Speeds are stored in bytes/sec so they can be re-formatted into whichever unit the user prefers
 * later; latency is in milliseconds.
 */
data class SpeedTestResult(
    val id: Long = 0L,
    val downloadBytesPerSecond: Double = 0.0,
    val uploadBytesPerSecond: Double = 0.0,
    val pingMillis: Int = 0,
    val jitterMillis: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: String = "Unknown"
)
