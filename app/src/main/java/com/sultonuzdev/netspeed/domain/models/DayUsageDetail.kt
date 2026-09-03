package com.sultonuzdev.netspeed.domain.models

/**
 * One day, opened from the History table.
 *
 * Carries the per-app breakdown for that day, which is the reason to open a row at all -- the
 * totals are already visible in the table.
 */
data class DayUsageDetail(
    val dateKey: String,
    val dateLabel: String,
    val mobileBytes: Long = 0L,
    val wifiBytes: Long = 0L,
    /** That day's share of the mobile cap; zero when no limit is configured. */
    val budgetBytes: Long = 0L,
    val level: DataLimitLevel = DataLimitLevel.NONE,
    val topApps: List<AppUsage> = emptyList(),
    val isLoadingApps: Boolean = false
) {
    val totalBytes: Long get() = mobileBytes + wifiBytes

    val budgetFraction: Float
        get() = if (budgetBytes > 0L) {
            (mobileBytes.toFloat() / budgetBytes).coerceIn(0f, 1f)
        } else 0f
}
