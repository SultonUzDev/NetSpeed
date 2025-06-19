package com.sultonuzdev.netspeed.presentation.screens.settings

import com.sultonuzdev.netspeed.utils.NotificationStyle

data class SettingsUiState(
    val speedNotificationEnabled: Boolean = true,
    val updateFrequency: String = "1 second",
    val notificationStyle: NotificationStyle = NotificationStyle.DETAILED,
    val monitorWifi: Boolean = true,
    val monitorMobile: Boolean = true,
    val backgroundMonitoring: Boolean = true,
    val monthlyResetDate: String = "1st",
    val dataLimitAlert: Boolean = false,
    val darkTheme: Boolean = true,
    val speedUnits: String = "Auto"
)