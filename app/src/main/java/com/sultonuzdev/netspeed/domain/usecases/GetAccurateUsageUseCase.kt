package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.NetworkStatsRepository

/**
 * Platform-accounted totals, when the user has granted usage access. Callers treat a null/empty
 * result as "fall back to the sampled history in Room".
 */
class GetAccurateUsageUseCase(private val repository: NetworkStatsRepository) {

    fun hasUsageAccess(): Boolean = repository.hasUsageAccess()

    suspend fun today(): UsageData? = repository.getUsageForDay()

    suspend fun forDay(dayMillis: Long): UsageData? = repository.getUsageForDay(dayMillis)

    suspend fun dailyHistory(days: Int): List<UsageData> = repository.getDailyHistory(days)

    suspend fun cycleTotal(resetDayOfMonth: Int): UsageData? =
        repository.getCycleUsage(resetDayOfMonth)
}
