package com.sultonuzdev.netspeed.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sultonuzdev.netspeed.data.database.entities.SpeedTestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {

    @Query("SELECT * FROM speed_test_table ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<SpeedTestEntity>>

    @Insert
    suspend fun insert(result: SpeedTestEntity): Long

    @Query("DELETE FROM speed_test_table")
    suspend fun deleteAll()
}
