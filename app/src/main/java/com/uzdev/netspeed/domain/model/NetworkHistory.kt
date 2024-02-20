package com.uzdev.netspeed.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_history")
data class NetworkHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "type")
    val type: String,


    @ColumnInfo(name = "time")
    val time: Long,
    @ColumnInfo(name = "download")
    val download: Long,
    @ColumnInfo(name = "upload")
    val upload: Long,

    @ColumnInfo(name = "ping_duration")
    val pingDuration: Long
)