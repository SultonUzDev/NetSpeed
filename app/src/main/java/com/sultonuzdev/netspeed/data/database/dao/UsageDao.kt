package com.sultonuzdev.netspeed.data.database.dao

import androidx.room.*
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_table ORDER BY date DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_table WHERE date = :date LIMIT 1")
    suspend fun getUsageByDate(date: String): UsageEntity?

    @Query("SELECT * FROM usage_table WHERE date LIKE :monthYear || '%' ORDER BY date DESC")
    fun getMonthlyUsageData(monthYear: String): Flow<List<UsageEntity>>

    @Query("SELECT SUM(totalUsage) FROM usage_table WHERE date LIKE :monthYear || '%'")
    suspend fun getMonthlyUsage(monthYear: String): Long?

    @Query("SELECT * FROM usage_table WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageEntity)

    @Update
    suspend fun updateUsage(usage: UsageEntity)

    @Delete
    suspend fun deleteUsage(usage: UsageEntity)

    @Query("DELETE FROM usage_table")
    suspend fun deleteAllUsage()
}