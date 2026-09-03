package com.sultonuzdev.netspeed.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_table")
data class SpeedTestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val downloadBps: Double = 0.0,
    val uploadBps: Double = 0.0,
    val pingMillis: Int = 0,
    val jitterMillis: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: String = "Unknown"
)
