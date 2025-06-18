package com.sultonuzdev.netspeed.domain.repository

import com.sultonuzdev.netspeed.domain.models.UsageData
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun getTodayUsage(): Flow<UsageData>
    fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>>
    fun getWeeklyUsage(): Flow<List<UsageData>>
    suspend fun saveUsageData(usageData: UsageData)
    suspend fun updateUsageData(usageData: UsageData)
    suspend fun getMonthlyTotal(monthYear: String): Long


    // New methods for daily history
    fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageData>>
    fun getMultipleMonthsUsage(months: List<String>): Flow<List<UsageData>>
}