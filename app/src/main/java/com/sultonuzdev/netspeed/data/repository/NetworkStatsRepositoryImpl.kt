package com.sultonuzdev.netspeed.data.repository

import com.sultonuzdev.netspeed.data.datasource.NetworkStatsDataSource
import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.NetworkStatsRepository
import com.sultonuzdev.netspeed.utils.UsagePeriods

class NetworkStatsRepositoryImpl(
    private val dataSource: NetworkStatsDataSource
) : NetworkStatsRepository {

    override fun hasUsageAccess(): Boolean = dataSource.hasUsageAccess()

    override suspend fun getUsageForDay(dayMillis: Long): UsageData? {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)
            ?.copy(date = UsagePeriods.dayKey(dayMillis))
    }

    override suspend fun getDailyHistory(days: Int): List<UsageData> {
        if (!hasUsageAccess()) return emptyList()
        return UsagePeriods.lastDays(days)
            .mapNotNull { (key, bounds) ->
                dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)?.copy(date = key)
            }
            .sortedByDescending { it.date }
    }

    override suspend fun getCycleUsage(resetDayOfMonth: Int): UsageData? {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryDeviceUsage(bounds.first, bounds.last + 1)
            ?.copy(date = UsagePeriods.dayKey(bounds.first))
    }

    override suspend fun getAppUsageForDay(dayMillis: Long): List<AppUsage> {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryAppUsage(bounds.first, bounds.last + 1)
    }

    override suspend fun getAppUsageForCycle(resetDayOfMonth: Int): List<AppUsage> {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryAppUsage(bounds.first, bounds.last + 1)
    }

    override suspend fun getAppDetailForDay(uid: Int, dayMillis: Long): AppUsageDetail? {
        val bounds = UsagePeriods.dayBounds(dayMillis)
        return dataSource.queryAppDetail(uid, bounds.first, bounds.last + 1)
    }

    override suspend fun getAppDetailForCycle(uid: Int, resetDayOfMonth: Int): AppUsageDetail? {
        val bounds = UsagePeriods.billingCycleBounds(resetDayOfMonth)
        return dataSource.queryAppDetail(uid, bounds.first, bounds.last + 1)
    }
}
