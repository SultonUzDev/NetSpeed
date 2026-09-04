package com.sultonuzdev.netspeed.presentation.screens.settings

import com.sultonuzdev.netspeed.utils.NotificationStyle
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import com.sultonuzdev.netspeed.utils.SpeedUnit

data class SettingsUiState(
    /** Whether the monitoring service is running. A foreground service cannot exist without
     *  its notification, so this switch is the monitoring master, not a notification toggle. */
    val monitoringEnabled: Boolean = false,
    val updateFrequency: String = "1 second",
    val notificationStyle: NotificationStyle = NotificationStyle.DETAILED,
    val monitorWifi: Boolean = true,
    val monitorMobile: Boolean = true,
    val backgroundMonitoring: Boolean = true,
    val startOnBoot: Boolean = true,
    val monthlyResetDate: String = "1st",
    val dataLimitAlert: Boolean = false,
    val dataLimit: String = "25.0 GB",
    val dataLimitBytes: Long = 25L * 1024 * 1024 * 1024,
    val warningThreshold: String = "80%",
    val warningThresholdPercent: Int = 80,
    val roamingAlert: Boolean = true,
    val backgroundDataAlert: Boolean = false,
    val backgroundDataThreshold: String = "200.0 MB",
    val backgroundDataThresholdBytes: Long = 200L * 1024 * 1024,
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = true,
    val overlayEnabled: Boolean = false,
    val overlayTextSize: Int = 12,
    val overlayColor: Int = 0xFFFFFFFF.toInt(),
    val overlayColorName: String = "White",
    val overlayOpacity: Int = 55,
    val speedUnits: String = SpeedUnit.AUTO.label,
    val speedUnit: SpeedUnit = SpeedUnit.AUTO,
    val speedDisplayMode: SpeedDisplayMode = SpeedDisplayMode.DOWNLOAD
)