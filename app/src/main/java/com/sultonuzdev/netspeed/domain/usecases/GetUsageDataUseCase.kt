package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class GetUsageDataUseCase(private val repository: UsageRepository) {
    fun getTodayUsage(): Flow<UsageData> = repository.getTodayUsage()

    fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>> =
        repository.getMonthlyUsage(monthYear)

    fun getWeeklyUsage(): Flow<List<UsageData>> = repository.getWeeklyUsage()

    suspend fun getMonthlyTotal(monthYear: String): Long =
        repository.getMonthlyTotal(monthYear)

    // New method to get daily usage history for multiple months
    fun getDailyUsageHistory(days: Int = 30): Flow<List<UsageData>> {
        val calendar = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return repository.getUsageByDateRange(startDate, endDate)
    }

    // Method to get current and previous month data
    fun getCurrentAndPreviousMonthUsage(): Flow<List<UsageData>> {
        val calendar = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        calendar.add(Calendar.MONTH, -1)
        val previousMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        return repository.getMultipleMonthsUsage(listOf(currentMonth, previousMonth))
    }
}