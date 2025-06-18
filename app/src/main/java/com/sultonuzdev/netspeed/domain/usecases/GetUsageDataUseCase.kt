package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow

class GetUsageDataUseCase(private val repository: UsageRepository) {
    fun getTodayUsage(): Flow<UsageData> = repository.getTodayUsage()

    fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>> =
        repository.getMonthlyUsage(monthYear)

    fun getWeeklyUsage(): Flow<List<UsageData>> = repository.getWeeklyUsage()

    suspend fun getMonthlyTotal(monthYear: String): Long =
        repository.getMonthlyTotal(monthYear)
}