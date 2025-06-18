package com.sultonuzdev.netspeed.domain.models

data class DailyUsageData(
    val title: String,
    val mobileUsage: String = "0 B",
    val wifiUsage: String = "0 B",
    val totalUsage: String = "0 B",
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true
)