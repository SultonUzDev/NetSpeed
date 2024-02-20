package com.uzdev.netspeed.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.uzdev.netspeed.domain.model.NetworkHistory

@Database(entities = [NetworkHistory::class], version = 1, exportSchema = false)
abstract class NetworkDatabase : RoomDatabase() {
    abstract val networkDao: NetworkDao
}