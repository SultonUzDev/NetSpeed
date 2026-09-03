package com.sultonuzdev.netspeed.domain.repository

import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.models.UsageData

/**
 * Platform-accounted usage. Every method returns null/empty when usage access has not been
 * granted, which is the caller's signal to fall back to the sampled numbers in Room.
 */
interface NetworkStatsRepository {
    fun hasUsageAccess(): Boolean

    suspend fun getUsageForDay(dayMillis: Long = System.currentTimeMillis()): UsageData?

    /** Per-day totals for the last [days] calendar days, newest first. */
    suspend fun getDailyHistory(days: Int): List<UsageData>

    suspend fun getCycleUsage(resetDayOfMonth: Int): UsageData?

    suspend fun getAppUsageForDay(dayMillis: Long = System.currentTimeMillis()): List<AppUsage>

    suspend fun getAppUsageForCycle(resetDayOfMonth: Int): List<AppUsage>

    suspend fun getAppDetailForDay(uid: Int, dayMillis: Long = System.currentTimeMillis()): AppUsageDetail?

    suspend fun getAppDetailForCycle(uid: Int, resetDayOfMonth: Int): AppUsageDetail?
}
