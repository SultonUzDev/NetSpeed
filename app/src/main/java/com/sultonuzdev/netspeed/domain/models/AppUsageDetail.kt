package com.sultonuzdev.netspeed.domain.models

/**
 * A single app's usage broken down further than the list needs.
 *
 * The foreground/background split is the part the system's own app-info screen does not surface
 * clearly, and it is what answers "why is this app using data when I'm not in it".
 */
data class AppUsageDetail(
    val uid: Int,
    val packageName: String,
    val appLabel: String,
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val foregroundBytes: Long = 0L,
    val backgroundBytes: Long = 0L,
    /** Fraction of all app traffic in the same window, 0..1. */
    val shareOfPeriod: Float = 0f,
    val periodLabel: String = ""
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes

    /**
     * Whether the platform actually split the buckets by state. Some devices report everything as
     * STATE_ALL, in which case the split is meaningless rather than zero.
     */
    val hasStateBreakdown: Boolean get() = foregroundBytes > 0L || backgroundBytes > 0L

    /** Synthetic buckets (tethering, removed apps) have no installed package to open. */
    val isRealPackage: Boolean
        get() = packageName.isNotBlank() &&
                !packageName.startsWith("uid:") &&
                !packageName.startsWith("shared:") &&
                packageName.contains('.') &&
                !packageName.startsWith("com.sultonuzdev.netspeed.")
}
