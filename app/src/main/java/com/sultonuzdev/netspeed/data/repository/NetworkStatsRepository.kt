package com.sultonuzdev.netspeed.data.repository

import com.sultonuzdev.netspeed.data.datasource.NetworkStatsDataSource
import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.utils.UsagePeriods

/**
 * Platform-accounted usage. Every method returns null/empty when usage access has not been
 * granted, which is the caller's signal to fall back to the sampled numbers in Room.
 */
class NetworkStatsRepository(
    private val dataSource: NetworkStatsDataSource
) {

    fun hasUsageAccess(): Boolean = dataSource.hasUsageAccess()

    suspend fun getUsageForDay(dayMillis: Long = System.currentTimeMillis()): UsageData? {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)
            ?.copy(date = UsagePeriods.dayKey(dayMillis))
    }

    suspend fun getDailyHistory(days: Int): List<UsageData> {
        if (!hasUsageAccess()) return emptyList()
        return UsagePeriods.lastDays(days)
            .mapNotNull { (key, bounds) ->
                dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)?.copy(date = key)
            }
            .sortedByDescending { it.date }
    }

    suspend fun getCycleUsage(resetDayOfMonth: Int): UsageData? {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)
            ?.copy(date = UsagePeriods.dayKey(bounds.first))
    }

    suspend fun getAppUsageForDay(dayMillis: Long = System.currentTimeMillis()): List<AppUsage> {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryAppUsage(bounds.first, bounds.last + 1)
    }

    suspend fun getAppUsageForCycle(resetDayOfMonth: Int): List<AppUsage> {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryAppUsage(bounds.first, bounds.last + 1)
    }

    suspend fun getAppDetailForDay(uid: Int, dayMillis: Long = System.currentTimeMillis()): AppUsageDetail? {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryAppDetail(uid, bounds.first, bounds.last + 1)
    }

    suspend fun getAppDetailForCycle(uid: Int, resetDayOfMonth: Int): AppUsageDetail? {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryAppDetail(uid, bounds.first, bounds.last + 1)
    }
}
