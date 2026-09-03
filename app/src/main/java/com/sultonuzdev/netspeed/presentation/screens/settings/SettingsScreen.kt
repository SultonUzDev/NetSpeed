package com.sultonuzdev.netspeed.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.presentation.components.SelectionDialog
import com.sultonuzdev.netspeed.presentation.components.SettingItem
import com.sultonuzdev.netspeed.utils.OverlayPermissionHelper
import com.sultonuzdev.netspeed.presentation.theme.supportsDynamicColor
import org.koin.androidx.compose.koinViewModel


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showFrequencyDialog by viewModel.showFrequencyDialog.collectAsStateWithLifecycle()
    val showStyleDialog by viewModel.showStyleDialog.collectAsStateWithLifecycle()
    val showUnitsDialog by viewModel.showUnitsDialog.collectAsStateWithLifecycle()
    val showDateDialog by viewModel.showDateDialog.collectAsStateWithLifecycle()
    val showLimitDialog by viewModel.showLimitDialog.collectAsStateWithLifecycle()
    val showThresholdDialog by viewModel.showThresholdDialog.collectAsStateWithLifecycle()
    val showDisplayModeDialog by viewModel.showDisplayModeDialog.collectAsStateWithLifecycle()
    val showOverlaySizeDialog by viewModel.showOverlaySizeDialog.collectAsStateWithLifecycle()
    val showOverlayColorDialog by viewModel.showOverlayColorDialog.collectAsStateWithLifecycle()
    val showOverlayOpacityDialog by viewModel.showOverlayOpacityDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // See SpeedScreen: the Scaffold's content padding already clears the bottom bar.
            .padding(bottom = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Notification Section
            SettingsSection(title = "NOTIFICATION") {
                SettingItem(
                    label = "Show speed in notification bar",
                    isToggle = true,
                    isEnabled = uiState.speedNotificationEnabled,
                    onToggleChange = { viewModel.updateSpeedNotification(it) }
                )

                SettingItem(
                    label = "Update frequency",
                    value = uiState.updateFrequency,
                    onValueClick = { viewModel.showFrequencyDialog() }
                )

                SettingItem(
                    label = "Notification style",
                    value = uiState.notificationStyle.styleName,
                    onValueClick = { viewModel.showStyleDialog() }
                )

                SettingItem(
                    label = "Status bar shows",
                    value = uiState.speedDisplayMode.label,
                    onValueClick = { viewModel.showDisplayModeDialog() }
                )

                SettingItem(
                    label = "Speed units",
                    value = uiState.speedUnits,
                    onValueClick = { viewModel.showUnitsDialog() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Monitoring Section
            SettingsSection(title = "MONITORING") {
                SettingItem(
                    label = "Monitor Wi-Fi",
                    isToggle = true,
                    isEnabled = uiState.monitorWifi,
                    onToggleChange = { viewModel.updateMonitorWifi(it) }
                )

                SettingItem(
                    label = "Monitor mobile data",
                    isToggle = true,
                    isEnabled = uiState.monitorMobile,
                    onToggleChange = { viewModel.updateMonitorMobile(it) }
                )

                SettingItem(
                    label = "Keep monitoring in background",
                    isToggle = true,
                    isEnabled = uiState.backgroundMonitoring,
                    onToggleChange = { viewModel.updateBackgroundMonitoring(it) }
                )

                SettingItem(
                    label = "Start after device restart",
                    isToggle = true,
                    isEnabled = uiState.startOnBoot,
                    onToggleChange = { viewModel.updateStartOnBoot(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Floating Overlay Section
            SettingsSection(title = "FLOATING OVERLAY") {
                SettingItem(
                    label = "Show floating overlay",
                    isToggle = true,
                    isEnabled = uiState.overlayEnabled,
                    onToggleChange = { wantsOverlay ->
                        // "Draw over other apps" cannot be requested in-app; without it the
                        // switch would flip on and nothing would appear, so send the user to
                        // settings instead of storing a preference we cannot honour.
                        if (wantsOverlay && !OverlayPermissionHelper.canDrawOverlays(context)) {
                            openOverlaySettings(context)
                        } else {
                            viewModel.updateOverlayEnabled(wantsOverlay)
                        }
                    }
                )

                SettingItem(
                    label = "Overlay text size",
                    value = "${uiState.overlayTextSize} sp",
                    onValueClick = { viewModel.showOverlaySizeDialog() }
                )

                SettingItem(
                    label = "Overlay text colour",
                    value = uiState.overlayColorName,
                    onValueClick = { viewModel.showOverlayColorDialog() }
                )

                SettingItem(
                    label = "Overlay background opacity",
                    value = "${uiState.overlayOpacity}%",
                    onValueClick = { viewModel.showOverlayOpacityDialog() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data & Privacy Section
            SettingsSection(title = "DATA & PRIVACY") {
                SettingItem(
                    label = "Billing cycle starts on",
                    value = uiState.monthlyResetDate,
                    onValueClick = { viewModel.showDateDialog() }
                )

                SettingItem(
                    label = "Warn before data limit",
                    isToggle = true,
                    isEnabled = uiState.dataLimitAlert,
                    onToggleChange = { viewModel.updateDataLimitAlert(it) }
                )

                SettingItem(
                    label = "Mobile data limit",
                    value = uiState.dataLimit,
                    onValueClick = { viewModel.showLimitDialog() }
                )

                SettingItem(
                    label = "Warn at",
                    value = uiState.warningThreshold,
                    onValueClick = { viewModel.showThresholdDialog() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            SettingsSection(title = "APPEARANCE") {
                SettingItem(
                    label = "Dark theme",
                    isToggle = true,
                    isEnabled = uiState.darkTheme,
                    onToggleChange = { viewModel.updateDarkTheme(it) }
                )

                // Wallpaper-derived colour only exists from Android 12; on older devices the row
                // would be a switch that does nothing, so it is not offered at all.
                if (supportsDynamicColor) {
                    SettingItem(
                        label = "Match wallpaper colours",
                        isToggle = true,
                        isEnabled = uiState.dynamicColor,
                        onToggleChange = { viewModel.updateDynamicColor(it) }
                    )
                }

            }
        }
    }

    // Dialogs
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
        // An all-caps section label at headlineSmall (18sp) with wide tracking read as a page
        // heading and competed with the setting names beneath it. Section labels are a small,
        // quiet type role.
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        content()
    }
}

private fun openOverlaySettings(context: android.content.Context) {
    val launch = { intent: android.content.Intent ->
        runCatching {
            context.startActivity(
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }
    if (!launch(OverlayPermissionHelper.settingsIntent(context))) {
        launch(OverlayPermissionHelper.settingsFallbackIntent())
    }
}
