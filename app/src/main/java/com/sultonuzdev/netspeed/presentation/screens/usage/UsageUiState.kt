package com.sultonuzdev.netspeed.presentation.screens.usage

import com.sultonuzdev.netspeed.domain.models.DailyUsageData
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.domain.models.DayUsageDetail
import com.sultonuzdev.netspeed.presentation.components.UsageBar

/** Window the per-app breakdown covers. */
enum class AppUsagePeriod(val label: String) {
    TODAY("Today"),
    CYCLE("Billing cycle")
}

/** One app's share of usage over the selected window, pre-formatted for display. */
data class AppUsageRow(
    val uid: Int,
    val packageName: String,
    val appLabel: String,
    val mobileUsage: String,
    val wifiUsage: String,
    val totalUsage: String,
    val totalBytes: Long,
    /** Fraction of the window's largest consumer, for the inline bar. */
    val shareOfMax: Float
)

data class UsageUiState(
    val todayWifi: String = "0 B",
    val todayMobile: String = "0 B",
    val todayTotal: String = "0 B",
    val todayProgress: Float = 0f,
    val sessionUsage: String = "0",
    val sessionUnit: String = "B",
    val sessionTime: String = "0s",
    /** Per-day columns for the History chart, oldest first. */
    val dailyChart: List<UsageBar> = emptyList(),
    val dailyUsageHistory: List<DailyUsageData> = emptyList(),
    val last7DaysUsage: DailyUsageData = DailyUsageData("Last 7 days"),
    val last30DaysUsage: DailyUsageData = DailyUsageData("Last 30 days"),

    /** Totals for the current billing cycle, shown in the pinned bottom row. */
    val cycleTotals: DailyUsageData = DailyUsageData("This cycle"),

    /** Mobile usage measured against the cap; null while unknown. */
    val dataLimitStatus: DataLimitStatus? = null,
    val dataLimitAlertEnabled: Boolean = false,

    val appUsagePeriod: AppUsagePeriod = AppUsagePeriod.TODAY,
    val appUsage: List<AppUsageRow> = emptyList(),

    /** Set while the per-app detail dialog is open; null closes it. */
    val selectedApp: AppUsageDetail? = null,
    /** Set while a History day is open; null closes it. */
    val selectedDay: DayUsageDetail? = null,
    val isLoadingAppDetail: Boolean = false,

    /** Whether the user has granted usage access; per-app data is unavailable without it. */
    val hasUsageAccess: Boolean = false,
    /** True when figures come from NetworkStatsManager rather than sampled TrafficStats. */
    val isUsageAccurate: Boolean = false,

    val isLoading: Boolean = false,
    val isLoadingApps: Boolean = false,
    val error: String? = null
)
