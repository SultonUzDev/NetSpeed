package com.sultonuzdev.netspeed.domain.models

data class DailyUsageData(
    val title: String,
    val mobileUsage: String = "0 B",
    val wifiUsage: String = "0 B",
    val totalUsage: String = "0 B",
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true,

    /** "yyyy-MM-dd"; empty for aggregate rows, which cannot be opened. */
    val dateKey: String = "",
    val mobileBytes: Long = 0L,
    /** Where this day's mobile usage sits against its share of the cap. */
    val limitLevel: DataLimitLevel = DataLimitLevel.NONE
)