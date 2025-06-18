package com.sultonuzdev.netspeed.data.repository

import com.sultonuzdev.netspeed.data.database.dao.UsageDao
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class UsageRepositoryImpl (
    private val usageDao: UsageDao,
    private val context: android.content.Context
) : UsageRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override fun getTodayUsage(): Flow<UsageData> {
        val today = dateFormat.format(Date())
        return usageDao.getAllUsage().map { entities ->
            val todayEntity = entities.find { it.date == today }
            todayEntity?.let { entity ->
                UsageData(
                    date = entity.date,
                    wifiUsage = entity.wifiUsage,
                    mobileUsage = entity.mobileUsage,
                    totalUsage = entity.totalUsage,
                    sessionTime = entity.sessionTime
                )
            } ?: UsageData(
                date = today,
                wifiUsage = 0L,
                mobileUsage = 0L,
                totalUsage = 0L,
                sessionTime = 0L
            )
        }
    }

    override fun getMonthlyUsage(monthYear: String): Flow<List<UsageData>> {
        return usageDao.getMonthlyUsageData(monthYear).map { entities ->
            entities.map { entity ->
                UsageData(
                    date = entity.date,
                    wifiUsage = entity.wifiUsage,
                    mobileUsage = entity.mobileUsage,
                    totalUsage = entity.totalUsage,
                    sessionTime = entity.sessionTime
                )
            }.sortedByDescending { it.date } // Sort by date descending (newest first)
        }
    }

    override fun getWeeklyUsage(): Flow<List<UsageData>> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = dateFormat.format(calendar.time)

        return usageDao.getUsageByDateRange(startDate, endDate).map { entities ->
            entities.map { entity ->
                UsageData(
                    date = entity.date,
                    wifiUsage = entity.wifiUsage,
                    mobileUsage = entity.mobileUsage,
                    totalUsage = entity.totalUsage,
                    sessionTime = entity.sessionTime
                )
            }
        }
    }

    override suspend fun saveUsageData(usageData: UsageData) {
        val entity = UsageEntity(
            date = usageData.date,
            wifiUsage = usageData.wifiUsage,
            mobileUsage = usageData.mobileUsage,
            totalUsage = usageData.totalUsage,
            sessionTime = usageData.sessionTime
        )
        usageDao.insertUsage(entity)
    }

    override suspend fun updateUsageData(usageData: UsageData) {
        val existingEntity = usageDao.getUsageByDate(usageData.date)
        if (existingEntity != null) {
            val updatedEntity = existingEntity.copy(
                wifiUsage = usageData.wifiUsage,
                mobileUsage = usageData.mobileUsage,
                totalUsage = usageData.totalUsage,
                sessionTime = usageData.sessionTime
            )
            usageDao.updateUsage(updatedEntity)
        } else {
            saveUsageData(usageData)
        }
    }

    override suspend fun getMonthlyTotal(monthYear: String): Long {
        return usageDao.getMonthlyUsage(monthYear) ?: 0L
    }

    // New implementations for daily history
    override fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageData>> {
        return usageDao.getUsageByDateRange(startDate, endDate).map { entities ->
            entities.map { entity ->
                UsageData(
                    date = entity.date,
                    wifiUsage = entity.wifiUsage,
                    mobileUsage = entity.mobileUsage,
                    totalUsage = entity.totalUsage,
                    sessionTime = entity.sessionTime
                )
            }.sortedByDescending { it.date }
        }
    }

    override fun getMultipleMonthsUsage(months: List<String>): Flow<List<UsageData>> {
        return usageDao.getAllUsage().map { entities ->
            entities.filter { entity ->
                months.any { month -> entity.date.startsWith(month) }
            }.map { entity ->
                UsageData(
                    date = entity.date,
                    wifiUsage = entity.wifiUsage,
                    mobileUsage = entity.mobileUsage,
                    totalUsage = entity.totalUsage,
                    sessionTime = entity.sessionTime
                )
            }.sortedByDescending { it.date }
        }
    }
}