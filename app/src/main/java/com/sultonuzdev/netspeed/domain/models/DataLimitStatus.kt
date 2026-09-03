package com.sultonuzdev.netspeed.domain.models

/** How far into the data cap the current billing cycle has got. */
enum class DataLimitLevel {
    /** Below the warning threshold. */
    NONE,

    /** At or past the warning threshold, but still under the cap. */
    WARNING,

    /** At or past the cap. */
    REACHED
}

/**
 * Mobile-data usage for the current billing cycle measured against the user's cap.
 *
 * Mobile only: a data cap is a property of a mobile plan, so counting Wi-Fi against it would make
 * the warning fire for traffic that costs the user nothing.
 */
data class DataLimitStatus(
    val usedBytes: Long,
    val limitBytes: Long,
    val level: DataLimitLevel,
    val cycleKey: String,
    val warningThresholdPercent: Int
) {
    /** Progress toward the cap, clamped to 0..1 for display. */
    val fraction: Float
        get() = if (limitBytes > 0L) {
            (usedBytes.toFloat() / limitBytes).coerceIn(0f, 1f)
        } else 0f

    val percentUsed: Int
        get() = if (limitBytes > 0L) {
            ((usedBytes.toDouble() / limitBytes) * 100).toInt()
        } else 0

    val remainingBytes: Long get() = (limitBytes - usedBytes).coerceAtLeast(0L)
}
