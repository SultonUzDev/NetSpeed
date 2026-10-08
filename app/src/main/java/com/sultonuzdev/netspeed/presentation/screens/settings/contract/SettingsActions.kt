package com.sultonuzdev.netspeed.presentation.screens.settings.contract

/**
 * Everything the settings list can do. Bundled so [com.sultonuzdev.netspeed.presentation.screens.settings.SettingsScreenContent] takes one parameter
 * instead of twenty-odd lambdas, and so the preview can pass `SettingsActions()` and be done.
 */
 data class SettingsActions(
    val onMonitoringChange: (Boolean) -> Unit = {},
    val onFrequencyClick: () -> Unit = {},
    val onStyleClick: () -> Unit = {},
    val onDisplayModeClick: () -> Unit = {},
    val onUnitsClick: () -> Unit = {},
    val onMonitorWifiChange: (Boolean) -> Unit = {},
    val onMonitorMobileChange: (Boolean) -> Unit = {},
    val onBackgroundMonitoringChange: (Boolean) -> Unit = {},
    val onStartOnBootChange: (Boolean) -> Unit = {},
    val onOverlayChange: (Boolean) -> Unit = {},
    val onOverlaySizeClick: () -> Unit = {},
    val onOverlayColorClick: () -> Unit = {},
    val onOverlayOpacityClick: () -> Unit = {},
    val onResetDateClick: () -> Unit = {},
    val onDataLimitClick: () -> Unit = {},
    val onDataLimitAlertChange: (Boolean) -> Unit = {},
    val onThresholdClick: () -> Unit = {},
    val onRoamingAlertChange: (Boolean) -> Unit = {},
    val onBackgroundDataAlertChange: (Boolean) -> Unit = {},
    val onBackgroundThresholdClick: () -> Unit = {},
    val onAutoStartClick: () -> Unit = {},
    val onBatteryExemptionClick: () -> Unit = {},
    val onLanguageClick: () -> Unit = {},
    val onMoreAppsClick: () -> Unit = {},
    val onDarkThemeChange: (Boolean) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {}
)