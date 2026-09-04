package com.sultonuzdev.netspeed.domain.models

/** Where the current billing cycle is heading at the rate seen so far. */
data class UsageForecast(
    val usedBytes: Long,
    val limitBytes: Long,
    /** Total the cycle would reach if the current daily rate held. */
    val projectedBytes: Long,
    val perDayBytes: Long,
    val daysRemaining: Int,
    /** Days until the cap is reached, or null if the rate would not reach it this cycle. */
    val daysUntilLimit: Int?
) {
    val willExceed: Boolean get() = projectedBytes > limitBytes

    val overageBytes: Long get() = (projectedBytes - limitBytes).coerceAtLeast(0L)
}
