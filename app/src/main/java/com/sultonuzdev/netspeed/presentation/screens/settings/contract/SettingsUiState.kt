package com.sultonuzdev.netspeed.presentation.screens.settings.contract

import androidx.annotation.StringRes
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.utils.NotificationStyle
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import com.sultonuzdev.netspeed.utils.SpeedUnit

data class SettingsUiState(
    /** Whether the monitoring service is running. A foreground service cannot exist without
     *  its notification, so this switch is the monitoring master, not a notification toggle. */
    val monitoringEnabled: Boolean = false,
    val updateFrequencySeconds: Int = 1,
    val notificationStyle: NotificationStyle = NotificationStyle.DETAILED,
    val monitorWifi: Boolean = true,
    val monitorMobile: Boolean = true,
    val backgroundMonitoring: Boolean = true,
    val startOnBoot: Boolean = true,
    val monthlyResetDay: Int = 1,
    val dataLimitAlert: Boolean = false,
    val dataLimitBytes: Long = 25L * 1024 * 1024 * 1024,
    val warningThresholdPercent: Int = 80,
    val roamingAlert: Boolean = true,
    val backgroundDataAlert: Boolean = false,
    val backgroundDataThresholdBytes: Long = 200L * 1024 * 1024,
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = true,
    val overlayEnabled: Boolean = false,
    val overlayTextSize: Int = 12,
    val overlayColor: Int = 0xFFFFFFFF.toInt(),
    @param:StringRes val overlayColorLabel: Int = R.string.colour_white,
    val overlayOpacity: Int = 55,
    val speedUnit: SpeedUnit = SpeedUnit.AUTO,
    val speedDisplayMode: SpeedDisplayMode = SpeedDisplayMode.DOWNLOAD
)