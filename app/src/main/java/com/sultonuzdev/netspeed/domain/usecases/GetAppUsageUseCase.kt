package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.repository.NetworkStatsRepository

class GetAppUsageUseCase(private val repository: NetworkStatsRepository) {

    fun hasUsageAccess(): Boolean = repository.hasUsageAccess()

    suspend fun forToday(): List<AppUsage> = repository.getAppUsageForDay()

    suspend fun forDay(dayMillis: Long): List<AppUsage> = repository.getAppUsageForDay(dayMillis)

    suspend fun forCycle(resetDayOfMonth: Int): List<AppUsage> =
        repository.getAppUsageForCycle(resetDayOfMonth)

    suspend fun detailForToday(uid: Int): AppUsageDetail? = repository.getAppDetailForDay(uid)

    suspend fun detailForCycle(uid: Int, resetDayOfMonth: Int): AppUsageDetail? =
        repository.getAppDetailForCycle(uid, resetDayOfMonth)
}
