package com.uzdev.netspeed.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.uzdev.netspeed.domain.model.NetworkHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {

    @Insert
    fun insertNetworkResult(vararg networkHistory: NetworkHistory)


    @Query("select * from network_history")
     fun getNetworkTestHistory(): Flow<List<NetworkHistory>>

    @Query("DELETE FROM network_history")
    fun clearHistory()

}