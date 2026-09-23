package com.sultonuzdev.netspeed.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.PermissionRationale
import com.sultonuzdev.netspeed.presentation.components.SelectionDialog
import com.sultonuzdev.netspeed.presentation.components.SettingItem
import com.sultonuzdev.netspeed.presentation.components.hasPermission
import com.sultonuzdev.netspeed.presentation.components.isPermanentlyDenied
import com.sultonuzdev.netspeed.presentation.components.openNotificationSettings
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.presentation.theme.supportsDynamicColor
import com.sultonuzdev.netspeed.utils.AutoStartHelper
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.ACTION_STOP_MONITORING
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.OverlayPermissionHelper
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Turning monitoring on is the moment the notification matters, so that is where it is
    // asked for -- not on launch, where the request arrived before the user had seen anything
    // to say yes to. Monitoring starts either way: denied, the service still runs and the
    // in-app figures keep working, and the row below the switch offers the way back.
    var askNotifications by remember { mutableStateOf(false) }
    var notificationsDenied by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsDenied = !granted
        setMonitoring(context, true)
        // Only once the system has stopped offering its dialog is the settings page the only
        // way left; before that, the dialog is what the user should see.
        if (!granted && isPermanentlyDenied(context, Manifest.permission.POST_NOTIFICATIONS)) {
            openNotificationSettings(context)
        }
    }

    val requestMonitoring: (Boolean) -> Unit = { enabled ->
        val needsNotificationPermission = enabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        when {
            !needsNotificationPermission -> {
                if (enabled) notificationsDenied = false
                setMonitoring(context, enabled)
            }
            else -> askNotifications = true
        }
    }

    if (askNotifications) {
        PermissionRationale(
            title = "Show the speed in your status bar",
            body = "NetSpeed posts one ongoing notification carrying the live figure. " +
                    "Android needs your permission to show it. Everything in the app keeps " +
                    "working if you say no.",
            onConfirm = {
                askNotifications = false
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                askNotifications = false
                notificationsDenied = true
                setMonitoring(context, true)
            }
        )
    }

    SettingsScreenContent(
        uiState = uiState,
        // Offered only where such a screen exists and resolves; on a Pixel there is nothing to
        // link to and the row would be a dead end.
        showAutoStart = AutoStartHelper.hasAutoStartSettings(context),
        notificationsDenied = notificationsDenied,
        onEnableNotifications = { openNotificationSettings(context) },
        actions = SettingsActions(
            // The service writes the preference back, so the switch reflects what is actually
            // running rather than a wish stored beside it.
            onMonitoringChange = requestMonitoring,
            onFrequencyClick = viewModel::showFrequencyDialog,
            onStyleClick = viewModel::showStyleDialog,
            onDisplayModeClick = viewModel::showDisplayModeDialog,
            onUnitsClick = viewModel::showUnitsDialog,
            onMonitorWifiChange = viewModel::updateMonitorWifi,
            onMonitorMobileChange = viewModel::updateMonitorMobile,
            onBackgroundMonitoringChange = viewModel::updateBackgroundMonitoring,
            onStartOnBootChange = viewModel::updateStartOnBoot,
            onOverlayChange = { wantsOverlay ->
                // "Draw over other apps" cannot be requested in-app; without it the switch would
                // flip on and nothing would appear, so send the user to settings instead of
                // storing a preference we cannot honour.
                if (wantsOverlay && !OverlayPermissionHelper.canDrawOverlays(context)) {
                    openOverlaySettings(context)
                } else {
                    viewModel.updateOverlayEnabled(wantsOverlay)
                }
            },
            onOverlaySizeClick = viewModel::showOverlaySizeDialog,
            onOverlayColorClick = viewModel::showOverlayColorDialog,
            onOverlayOpacityClick = viewModel::showOverlayOpacityDialog,
            onResetDateClick = viewModel::showDateDialog,
            onDataLimitClick = viewModel::showLimitDialog,
            onDataLimitAlertChange = viewModel::updateDataLimitAlert,
            onThresholdClick = viewModel::showThresholdDialog,
            onRoamingAlertChange = viewModel::updateRoamingAlert,
            onBackgroundDataAlertChange = viewModel::updateBackgroundDataAlert,
            onBackgroundThresholdClick = viewModel::showBackgroundThresholdDialog,
            onAutoStartClick = {
                AutoStartHelper.resolveIntent(context)?.let { intent ->
                    runCatching { context.startActivity(intent) }
                }
            },
            onDarkThemeChange = viewModel::updateDarkTheme,
            onDynamicColorChange = viewModel::updateDynamicColor
        ),
        modifier = modifier
    )

    SettingsDialogs(viewModel = viewModel, uiState = uiState)
}

/**
 * Everything the settings list can do. Bundled so [SettingsScreenContent] takes one parameter
 * instead of twenty-odd lambdas, and so the preview can pass `SettingsActions()` and be done.
 */
private data class SettingsActions(
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
    val onDarkThemeChange: (Boolean) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {}
)

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    showAutoStart: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    notificationsDenied: Boolean = false,
    onEnableNotifications: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // MainActivity already pads every tab below the status bar; a second
                // statusBarsPadding() here dropped this screen ~40dp lower than the others.
                .navigationBarsPadding()
                // See SpeedScreen: the floating bar overlays content, so clear its height here.
                // Applied before verticalScroll so it bounds the viewport, not the content: the
                // other way round the last rows scrolled up underneath the bar.
                .padding(bottom = BottomNavigationHeight)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notification Section
                SettingsSection(title = "Notification") {
                    // A declined permission is a dead end unless something on screen offers the
                    // way back, so the row appears only while notifications are off.
                    if (notificationsDenied) {
                        Text(
                            text = "Notifications are off, so the speed cannot appear in your " +
                                    "status bar. Tap to turn them on.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onEnableNotifications)
                                .padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SettingItem(
                        label = "Monitor network speed",
                        isToggle = true,
                        isEnabled = uiState.monitoringEnabled,
                        // The service writes the preference back, so the switch reflects what is
                        // actually running rather than a wish stored beside it.
                        onToggleChange = actions.onMonitoringChange
                    )

                    SettingItem(
                        label = "Update frequency",
                        value = uiState.updateFrequency,
                        onValueClick = actions.onFrequencyClick
                    )

                    SettingItem(
                        label = "Notification style",
                        value = uiState.notificationStyle.styleName,
                        onValueClick = actions.onStyleClick
                    )

                    SettingItem(
                        label = "Status bar shows",
                        value = uiState.speedDisplayMode.label,
                        onValueClick = actions.onDisplayModeClick
                    )

                    SettingItem(
                        label = "Speed units",
                        value = uiState.speedUnits,
                        onValueClick = actions.onUnitsClick
                    )
                }


                // Monitoring Section
                SettingsSection(title = "Monitoring") {
                    SettingItem(
                        label = "Monitor Wi-Fi",
                        isToggle = true,
                        isEnabled = uiState.monitorWifi,
                        onToggleChange = actions.onMonitorWifiChange
                    )

                    SettingItem(
                        label = "Monitor mobile data",
                        isToggle = true,
                        isEnabled = uiState.monitorMobile,
                        onToggleChange = actions.onMonitorMobileChange
                    )

                    SettingItem(
                        label = "Keep monitoring in background",
                        isToggle = true,
                        isEnabled = uiState.backgroundMonitoring,
                        onToggleChange = actions.onBackgroundMonitoringChange
                    )

                    SettingItem(
                        label = "Start after device restart",
                        isToggle = true,
                        isEnabled = uiState.startOnBoot,
                        onToggleChange = actions.onStartOnBootChange
                    )
                }


                // Floating Overlay Section
                SettingsSection(title = "Floating overlay") {
                    SettingItem(
                        label = "Show floating overlay",
                        isToggle = true,
                        isEnabled = uiState.overlayEnabled,
                        onToggleChange = actions.onOverlayChange
                    )

                    SettingItem(
                        label = "Overlay text size",
                        value = "${uiState.overlayTextSize} sp",
                        onValueClick = actions.onOverlaySizeClick
                    )

                    SettingItem(
                        label = "Overlay text colour",
                        value = uiState.overlayColorName,
                        onValueClick = actions.onOverlayColorClick
                    )

                    SettingItem(
                        label = "Overlay background opacity",
                        value = "${uiState.overlayOpacity}%",
                        onValueClick = actions.onOverlayOpacityClick
                    )
                }


                // Data & Privacy Section
                // Split from the alerts below: the cycle day and the cap describe your plan, while
                // the switches under ALERTS decide what the app says about it.
                SettingsSection(title = "Data limit") {
                    SettingItem(
                        label = "Billing cycle starts on",
                        value = uiState.monthlyResetDate,
                        onValueClick = actions.onResetDateClick
                    )

                    SettingItem(
                        label = "Mobile data limit",
                        value = uiState.dataLimit,
                        onValueClick = actions.onDataLimitClick
                    )
                }


                SettingsSection(title = "Alerts") {
                    SettingItem(
                        label = "Warn before data limit",
                        isToggle = true,
                        isEnabled = uiState.dataLimitAlert,
                        onToggleChange = actions.onDataLimitAlertChange
                    )

                    // Only meaningful while the warning above is on; shown as a dead row otherwise,
                    // it invites the user to configure something that will never fire.
                    if (uiState.dataLimitAlert) {
                        SettingItem(
                            label = "Warn at",
                            value = uiState.warningThreshold,
                            onValueClick = actions.onThresholdClick
                        )
                    }

                    SettingItem(
                        label = "Warn when roaming",
                        isToggle = true,
                        isEnabled = uiState.roamingAlert,
                        onToggleChange = actions.onRoamingAlertChange
                    )

                    SettingItem(
                        label = "Warn about background data",
                        isToggle = true,
                        isEnabled = uiState.backgroundDataAlert,
                        onToggleChange = actions.onBackgroundDataAlertChange
                    )

                    if (uiState.backgroundDataAlert) {
                        SettingItem(
                            label = "Warn above",
                            value = uiState.backgroundDataThreshold,
                            onValueClick = actions.onBackgroundThresholdClick
                        )
                    }
                }


                // Offered only where such a screen exists and resolves; on a Pixel there is
                // nothing to link to and the row would be a dead end.
                if (showAutoStart) {

                    SettingsSection(title = "Device") {
                        // Says what tapping does and why it matters. "Allow autostart / Open" read
                        // like a setting whose current value was the word "Open".
                        SettingItem(
                            label = "Allow autostart",
                            value = "Settings",
                            onValueClick = actions.onAutoStartClick
                        )
                    }
                }


                // Appearance Section
                SettingsSection(title = "Appearance") {
                    SettingItem(
                        label = "Dark theme",
                        isToggle = true,
                        isEnabled = uiState.darkTheme,
                        onToggleChange = actions.onDarkThemeChange
                    )

                    // Wallpaper-derived colour only exists from Android 12; on older devices the row
                    // would be a switch that does nothing, so it is not offered at all.
                    if (supportsDynamicColor) {
                        SettingItem(
                            label = "Match wallpaper colours",
                            isToggle = true,
                            isEnabled = uiState.dynamicColor,
                            onToggleChange = actions.onDynamicColorChange
                        )
                    }

                }
            }
        }
    }}
@Composable
private fun SettingsScreenContentPreview() {
    NetSpeedTheme(darkTheme = true) {
        SettingsScreenContent(
            uiState = SettingsUiState(
                monitoringEnabled = true,
                overlayEnabled = true,
                dataLimitAlert = true,
                backgroundDataAlert = true
            ),
            showAutoStart = true,
            actions = SettingsActions()
        )
    }
}

/**
 * The selection dialogs, kept beside the view model rather than inside [SettingsScreenContent]:
 * each one reads its option list off the view model, so passing them through would mean another
 * dozen parameters for no gain in testability.
 */
@Composable
private fun SettingsDialogs(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val showFrequencyDialog by viewModel.showFrequencyDialog.collectAsStateWithLifecycle()
    val showStyleDialog by viewModel.showStyleDialog.collectAsStateWithLifecycle()
    val showUnitsDialog by viewModel.showUnitsDialog.collectAsStateWithLifecycle()
    val showDateDialog by viewModel.showDateDialog.collectAsStateWithLifecycle()
    val showLimitDialog by viewModel.showLimitDialog.collectAsStateWithLifecycle()
    val showThresholdDialog by viewModel.showThresholdDialog.collectAsStateWithLifecycle()
    val showDisplayModeDialog by viewModel.showDisplayModeDialog.collectAsStateWithLifecycle()
    val showBackgroundThresholdDialog by
        viewModel.showBackgroundThresholdDialog.collectAsStateWithLifecycle()
    val showOverlaySizeDialog by viewModel.showOverlaySizeDialog.collectAsStateWithLifecycle()
    val showOverlayColorDialog by viewModel.showOverlayColorDialog.collectAsStateWithLifecycle()
    val showOverlayOpacityDialog by viewModel.showOverlayOpacityDialog.collectAsStateWithLifecycle()

    if (showFrequencyDialog) {
        SelectionDialog(
            title = "Update Frequency",
            options = viewModel.frequencyOptions.map {
                if (it == 1) "1 second" else "$it seconds"
            },
            selectedIndex = viewModel.frequencyOptions.indexOf(
                // Extract number from current frequency text
                uiState.updateFrequency.split(" ")[0].toIntOrNull() ?: 1
            ),
            onOptionSelected = { index ->
                viewModel.updateUpdateFrequency(viewModel.frequencyOptions[index])
            },
            onDismiss = { viewModel.hideFrequencyDialog() }
        )
    }

    if (showStyleDialog) {
        SelectionDialog(
            title = "Notification Style",
            options = viewModel.styleOptions.map { it.styleName.replaceFirstChar { char -> char.uppercase() } },
            selectedIndex = viewModel.styleOptions.indexOf(uiState.notificationStyle),
            onOptionSelected = { index ->
                viewModel.updateNotificationStyle(viewModel.styleOptions[index])
            },
            onDismiss = { viewModel.hideStyleDialog() }
        )
    }

    if (showUnitsDialog) {
        SelectionDialog(
            title = "Speed Units",
            options = viewModel.unitsOptions.map { it.label },
            selectedIndex = viewModel.unitsOptions.indexOf(uiState.speedUnit),
            onOptionSelected = { index ->
                viewModel.updateSpeedUnits(viewModel.unitsOptions[index])
            },
            onDismiss = { viewModel.hideUnitsDialog() }
        )
    }

    if (showDisplayModeDialog) {
        SelectionDialog(
            title = "Speed Display",
            options = viewModel.displayModeOptions.map { it.label },
            selectedIndex = viewModel.displayModeOptions.indexOf(uiState.speedDisplayMode),
            onOptionSelected = { index ->
                viewModel.updateSpeedDisplayMode(viewModel.displayModeOptions[index])
            },
            onDismiss = { viewModel.hideDisplayModeDialog() }
        )
    }

    if (showBackgroundThresholdDialog) {
        SelectionDialog(
            title = "Background data threshold",
            options = viewModel.backgroundThresholdOptions.map { NetworkUtils.formatBytes(it) },
            selectedIndex = viewModel.backgroundThresholdOptions
                .indexOf(uiState.backgroundDataThresholdBytes),
            onOptionSelected = { index ->
                viewModel.updateBackgroundDataThreshold(
                    viewModel.backgroundThresholdOptions[index]
                )
            },
            onDismiss = { viewModel.hideBackgroundThresholdDialog() }
        )
    }

    if (showOverlaySizeDialog) {
        SelectionDialog(
            title = "Overlay Size",
            options = viewModel.overlaySizeOptions.map { "$it sp" },
            selectedIndex = viewModel.overlaySizeOptions.indexOf(uiState.overlayTextSize),
            onOptionSelected = { index ->
                viewModel.updateOverlayTextSize(viewModel.overlaySizeOptions[index])
            },
            onDismiss = { viewModel.hideOverlaySizeDialog() }
        )
    }

    if (showOverlayColorDialog) {
        SelectionDialog(
            title = "Overlay Color",
            options = viewModel.overlayColorOptions.map { it.second },
            selectedIndex = viewModel.overlayColorOptions.indexOfFirst {
                it.first == uiState.overlayColor
            },
            onOptionSelected = { index ->
                viewModel.updateOverlayColor(viewModel.overlayColorOptions[index].first)
            },
            onDismiss = { viewModel.hideOverlayColorDialog() }
        )
    }

    if (showOverlayOpacityDialog) {
        SelectionDialog(
            title = "Overlay Transparency",
            options = viewModel.overlayOpacityOptions.map { if (it == 0) "None" else "$it%" },
            selectedIndex = viewModel.overlayOpacityOptions.indexOf(uiState.overlayOpacity),
            onOptionSelected = { index ->
                viewModel.updateOverlayOpacity(viewModel.overlayOpacityOptions[index])
            },
            onDismiss = { viewModel.hideOverlayOpacityDialog() }
        )
    }

    if (showLimitDialog) {
        SelectionDialog(
            title = "Mobile Data Limit",
            options = viewModel.limitOptionsGb.map { "$it GB" },
            selectedIndex = viewModel.limitOptionsGb.indexOf(
                (uiState.dataLimitBytes / (1024L * 1024 * 1024)).toInt()
            ),
            onOptionSelected = { index ->
                viewModel.updateDataLimitGb(viewModel.limitOptionsGb[index])
            },
            onDismiss = { viewModel.hideLimitDialog() }
        )
    }

    if (showThresholdDialog) {
        SelectionDialog(
            title = "Warn At",
            options = viewModel.thresholdOptions.map { "$it% of limit" },
            selectedIndex = viewModel.thresholdOptions.indexOf(uiState.warningThresholdPercent),
            onOptionSelected = { index ->
                viewModel.updateWarningThreshold(viewModel.thresholdOptions[index])
            },
            onDismiss = { viewModel.hideThresholdDialog() }
        )
    }

    if (showDateDialog) {
        SelectionDialog(
            title = "Monthly Reset Date",
            options = viewModel.dateOptions.map { date ->
                when {
                    date % 10 == 1 && date != 11 -> "${date}st"
                    date % 10 == 2 && date != 12 -> "${date}nd"
                    date % 10 == 3 && date != 13 -> "${date}rd"
                    else -> "${date}th"
                }
            },
            selectedIndex = viewModel.dateOptions.indexOf(
                // Extract number from current date text
                uiState.monthlyResetDate.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
            ),
            onOptionSelected = { index ->
                viewModel.updateMonthlyResetDate(viewModel.dateOptions[index])
            },
            onDismiss = { viewModel.hideDateDialog() }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // Sentence case, no tracking: tracked capitals were the one all-caps element left in the
        // app, and a section label is a small, quiet type role rather than a badge.
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        content()
    }
}

private fun openOverlaySettings(context: Context) {
    val launch = { intent: Intent ->
        runCatching {
            context.startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }
    if (!launch(OverlayPermissionHelper.settingsIntent(context))) {
        launch(OverlayPermissionHelper.settingsFallbackIntent())
    }
}

/** Starts or stops the monitoring service; it persists the resulting state itself. */
private fun setMonitoring(context: Context, enabled: Boolean) {
    val intent = Intent(context, SpeedMonitorService::class.java).apply {
        action = if (enabled) ACTION_START_MONITORING else ACTION_STOP_MONITORING
    }
    runCatching { ContextCompat.startForegroundService(context, intent) }
}

