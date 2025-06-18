package com.sultonuzdev.netspeed.data.database.dao

import androidx.room.*
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_data ORDER BY createdAt DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_data WHERE date = :date")
    suspend fun getUsageByDate(date: String): UsageEntity?

    @Query("SELECT * FROM usage_data WHERE date >= :startDate AND date <= :endDate ORDER BY createdAt ASC")
    fun getUsageByDateRange(startDate: String, endDate: String): Flow<List<UsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageEntity)

    @Update
    suspend fun updateUsage(usage: UsageEntity)

    @Delete
    suspend fun deleteUsage(usage: UsageEntity)

    @Query("DELETE FROM usage_data")
    suspend fun deleteAllUsage()

    @Query("SELECT SUM(totalUsage) FROM usage_data WHERE date LIKE :monthYear || '%'")
    suspend fun getMonthlyUsage(monthYear: String): Long?

    @Query("SELECT * FROM usage_data WHERE date LIKE :monthYear || '%' ORDER BY createdAt ASC")
    fun getMonthlyUsageData(monthYear: String): Flow<List<UsageEntity>>
}