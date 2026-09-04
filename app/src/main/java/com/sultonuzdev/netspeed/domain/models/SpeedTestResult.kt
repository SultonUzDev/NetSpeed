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
 * Speeds are in bytes/sec so they can be formatted into whichever unit the user prefers; latency
 * is in milliseconds. Not persisted -- a result is only meaningful when it is taken.
 */
data class SpeedTestResult(
    val downloadBytesPerSecond: Double = 0.0,
    val uploadBytesPerSecond: Double = 0.0,
    val pingMillis: Int = 0,
    val jitterMillis: Int = 0
)
