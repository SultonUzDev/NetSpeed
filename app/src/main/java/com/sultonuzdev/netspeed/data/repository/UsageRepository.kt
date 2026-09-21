package com.sultonuzdev.netspeed.data.repository

import com.sultonuzdev.netspeed.data.database.dao.UsageDao
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity
import com.sultonuzdev.netspeed.domain.models.UsageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageRepository(private val usageDao: UsageDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayUsage(): Flow<UsageData> {
        val today = dateFormat.format(Date())
        return usageDao.getAllUsage().map { entities ->
            val todayEntity = entities.find { it.date == today }
            todayEntity?.toModel() ?: UsageData(
                date = today,
                wifiUsage = 0L,
                mobileUsage = 0L,
                totalUsage = 0L,
                sessionTime = 0L
            )
        }
    }

    fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>> {
        return usageDao.getMonthlyUsageData(monthYear).map { entities ->
            entities.map { it.toModel() }.sortedByDescending { it.date } // Sort by date descending (newest first)
        }
    }

    suspend fun addUsageDelta(
        date: String,
        wifiDelta: Long,
        mobileDelta: Long,
        sessionDelta: Long
    ) {
        if (wifiDelta <= 0L && mobileDelta <= 0L && sessionDelta <= 0L) return
        usageDao.addUsageDelta(date, wifiDelta, mobileDelta, sessionDelta)
    }

    fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageData>> {
        return usageDao.getUsageByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toModel() }.sortedByDescending { it.date }
        }
    }
}

private fun UsageEntity.toModel() = UsageData(
    date = date,
    wifiUsage = wifiUsage,
    mobileUsage = mobileUsage,
    totalUsage = totalUsage,
    sessionTime = sessionTime
)
