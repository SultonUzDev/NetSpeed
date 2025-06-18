package com.sultonuzdev.netspeed.data.database


import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sultonuzdev.netspeed.data.database.dao.UsageDao
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity

@Database(
    entities = [UsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetSpeedDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}