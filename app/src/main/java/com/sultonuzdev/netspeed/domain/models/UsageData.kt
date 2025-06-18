package com.sultonuzdev.netspeed.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UsageData(
    val date: String,
    val wifiUsage: Long = 0L,
    val mobileUsage: Long = 0L,
    val totalUsage: Long = 0L,
    val sessionTime: Long = 0L
)