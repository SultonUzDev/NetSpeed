package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.data.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetUsageDataUseCase(private val repository: UsageRepository) {
    fun getTodayUsage(): Flow<UsageData> = repository.getTodayUsage()

    fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>> =
        repository.getMonthlyUsage(monthYear)

    /** Sum of the stored rows between two "yyyy-MM-dd" keys, inclusive. Fallback for billing cycles. */
    suspend fun getUsageInRange(startDate: String, endDate: String): UsageData {
        val days = repository.getUsageByDateRange(startDate, endDate).first()
        return UsageData(
            date = startDate,
            wifiUsage = days.sumOf { it.wifiUsage },
            mobileUsage = days.sumOf { it.mobileUsage },
            totalUsage = days.sumOf { it.totalUsage },
            sessionTime = days.sumOf { it.sessionTime }
        )
    }
}
