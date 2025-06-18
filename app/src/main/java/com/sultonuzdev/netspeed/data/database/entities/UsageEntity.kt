package com.sultonuzdev.netspeed.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_data")
data class UsageEntity(
    @PrimaryKey
    val date: String,
    val wifiUsage: Long = 0L,
    val mobileUsage: Long = 0L,
    val totalUsage: Long = 0L,
    val sessionTime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)