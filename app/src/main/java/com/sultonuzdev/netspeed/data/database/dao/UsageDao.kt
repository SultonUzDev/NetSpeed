package com.sultonuzdev.netspeed.data.database.dao

import androidx.room.*
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_table ORDER BY date DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_table WHERE date LIKE :monthYear || '%' ORDER BY date DESC")
    fun getMonthlyUsageData(monthYear: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_table WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(usage: UsageEntity)

    @Query(
        """
        UPDATE usage_table
        SET wifiUsage = wifiUsage + :wifiDelta,
            mobileUsage = mobileUsage + :mobileDelta,
            totalUsage = totalUsage + :wifiDelta + :mobileDelta,
            sessionTime = sessionTime + :sessionDelta
        WHERE date = :date
        """
    )
    suspend fun incrementUsage(
        date: String,
        wifiDelta: Long,
        mobileDelta: Long,
        sessionDelta: Long
    )

    /**
     * Adds a delta onto a day's row, creating it first if the day has not been seen.
     *
     * Deltas rather than absolutes: a writer that restarts mid-day no longer rewrites the day's
     * total downward from its own zeroed counters, and a process death only loses the unflushed
     * tail instead of the whole day.
     */
    @Transaction
    suspend fun addUsageDelta(
        date: String,
        wifiDelta: Long,
        mobileDelta: Long,
        sessionDelta: Long
    ) {
        insertIfAbsent(UsageEntity(date = date))
        incrementUsage(date, wifiDelta, mobileDelta, sessionDelta)
    }
}