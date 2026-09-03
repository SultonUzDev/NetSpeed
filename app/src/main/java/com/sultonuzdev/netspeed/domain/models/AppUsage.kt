package com.sultonuzdev.netspeed.domain.models

/**
 * Network usage attributed to a single uid over some window. Uid, not package: the platform
 * accounts per-uid, so packages that share a uid are reported together.
 */
data class AppUsage(
    val uid: Int,
    val packageName: String,
    val appLabel: String,
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}
