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
import androidx.activity.compose.LocalActivity
import com.sultonuzdev.netspeed.utils.AppLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.PermissionRationale
import com.sultonuzdev.netspeed.presentation.components.SelectionDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import com.sultonuzdev.netspeed.presentation.components.GradientSettingItem
import com.sultonuzdev.netspeed.presentation.components.SettingItem
import com.sultonuzdev.netspeed.presentation.components.hasPermission
import com.sultonuzdev.netspeed.presentation.components.isPermanentlyDenied
import com.sultonuzdev.netspeed.presentation.components.openNotificationSettings
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.presentation.theme.supportsDynamicColor
import com.sultonuzdev.netspeed.utils.AutoStartHelper
import com.sultonuzdev.netspeed.utils.BatteryOptimizationHelper
import androidx.core.net.toUri
import android.net.Uri
import com.sultonuzdev.netspeed.presentation.screens.settings.contract.SettingsActions
import com.sultonuzdev.netspeed.presentation.screens.settings.contract.SettingsUiState
import com.sultonuzdev.netspeed.utils.Constants
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

    var showLanguageDialog by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    if (showLanguageDialog) {
        // "System default" first, then every supported language under its own endonym.
        val options = listOf(stringResource(R.string.language_system_default)) +
                AppLocale.supported.map { it.second }
        val currentTag = AppLocale.current(context)
        val selected = AppLocale.supported
            .indexOfFirst { it.first.equals(currentTag, ignoreCase = true) }
            .let { if (it >= 0) it + 1 else 0 }

        SelectionDialog(
            title = stringResource(R.string.dialog_language),
            options = options,
            selectedIndex = selected,
            onOptionSelected = { index ->
                showLanguageDialog = false
                val tag = if (index == 0) null else AppLocale.supported[index - 1].first
                // Below Android 13 nothing is watching this, so the activity restarts itself to
                // pick up the new configuration; from 13 on the system does that for us.
                if (AppLocale.set(context, tag)) activity?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Doze is what stops the meter on a Pixel or a Samsung, neither of which has an autostart
    // screen to offer. Read once here and re-read on the way back from the settings list, so the
    // row stops being offered the moment it is granted.
    var needsBatteryExemption by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        needsBatteryExemption = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
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
            title = stringResource(R.string.perm_notification_title),
            body = stringResource(R.string.perm_notification_body_settings),
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
        languageName = AppLocale.displayName(context)
            ?: stringResource(R.string.language_system_default),
        showBatteryExemption = needsBatteryExemption,
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
            onBatteryExemptionClick = {
                BatteryOptimizationHelper.openBatteryOptimizationSettings(
                    context = context,
                    launcher = batteryLauncher,
                    // A handful of builds ship without the screen. A row that leads nowhere is
                    // worse than no row, so it withdraws the offer instead of failing on tap.
                    onUnavailable = { needsBatteryExemption = false }
                )
            },
            onLanguageClick = { showLanguageDialog = true },
            onMoreAppsClick = { openDeveloperPage(context) },
            onDarkThemeChange = viewModel::updateDarkTheme,
            onDynamicColorChange = viewModel::updateDynamicColor
        ),
        modifier = modifier
    )

    SettingsDialogs(viewModel = viewModel, uiState = uiState)
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    showAutoStart: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    // Resolved by the caller: the current language comes from the platform or AppLocale's own
    // storage, neither of which this content-only composable should have to reach for.
    languageName: String = "",
    showBatteryExemption: Boolean = false,
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
                SettingsSection(title = stringResource(R.string.settings_section_notification)) {
                    // A declined permission is a dead end unless something on screen offers the
                    // way back, so the row appears only while notifications are off.
                    if (notificationsDenied) {
                        Text(
                            text = stringResource(R.string.settings_notifications_off),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onEnableNotifications)
                                .padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SettingItem(
                        label = stringResource(R.string.settings_monitor_speed),
                        isToggle = true,
                        isEnabled = uiState.monitoringEnabled,
                        // The service writes the preference back, so the switch reflects what is
                        // actually running rather than a wish stored beside it.
                        onToggleChange = actions.onMonitoringChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_update_frequency),
                        value = pluralStringResource(
                            R.plurals.frequency_seconds,
                            uiState.updateFrequencySeconds,
                            uiState.updateFrequencySeconds
                        ),
                        onValueClick = actions.onFrequencyClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_notification_style),
                        value = stringResource(uiState.notificationStyle.labelRes),
                        onValueClick = actions.onStyleClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_status_bar_shows),
                        value = stringResource(uiState.speedDisplayMode.labelRes),
                        onValueClick = actions.onDisplayModeClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_speed_units),
                        value = stringResource(uiState.speedUnit.labelRes),
                        onValueClick = actions.onUnitsClick
                    )
                }


                // Monitoring Section
                SettingsSection(title = stringResource(R.string.settings_section_monitoring)) {
                    SettingItem(
                        label = stringResource(R.string.settings_monitor_wifi),
                        isToggle = true,
                        isEnabled = uiState.monitorWifi,
                        onToggleChange = actions.onMonitorWifiChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_monitor_mobile),
                        isToggle = true,
                        isEnabled = uiState.monitorMobile,
                        onToggleChange = actions.onMonitorMobileChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_background_monitoring),
                        isToggle = true,
                        isEnabled = uiState.backgroundMonitoring,
                        onToggleChange = actions.onBackgroundMonitoringChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_start_on_boot),
                        isToggle = true,
                        isEnabled = uiState.startOnBoot,
                        onToggleChange = actions.onStartOnBootChange
                    )
                }


                // Floating Overlay Section
                SettingsSection(title = stringResource(R.string.settings_section_overlay)) {
                    SettingItem(
                        label = stringResource(R.string.settings_show_overlay),
                        isToggle = true,
                        isEnabled = uiState.overlayEnabled,
                        onToggleChange = actions.onOverlayChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_overlay_size),
                        value = stringResource(R.string.measure_sp, uiState.overlayTextSize),
                        onValueClick = actions.onOverlaySizeClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_overlay_colour),
                        value = stringResource(uiState.overlayColorLabel),
                        onValueClick = actions.onOverlayColorClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_overlay_opacity),
                        value = stringResource(R.string.measure_percent, uiState.overlayOpacity),
                        onValueClick = actions.onOverlayOpacityClick
                    )
                }


                // Data & Privacy Section
                // Split from the alerts below: the cycle day and the cap describe your plan, while
                // the switches under ALERTS decide what the app says about it.
                SettingsSection(title = stringResource(R.string.settings_section_data_limit)) {
                    SettingItem(
                        label = stringResource(R.string.settings_billing_cycle_start),
                        value = ordinalDay(uiState.monthlyResetDay),
                        onValueClick = actions.onResetDateClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_mobile_data_limit),
                        value = NetworkUtils.formatBytes(uiState.dataLimitBytes),
                        onValueClick = actions.onDataLimitClick
                    )
                }


                SettingsSection(title = stringResource(R.string.settings_section_alerts)) {
                    SettingItem(
                        label = stringResource(R.string.settings_warn_before_limit),
                        isToggle = true,
                        isEnabled = uiState.dataLimitAlert,
                        onToggleChange = actions.onDataLimitAlertChange
                    )

                    // Only meaningful while the warning above is on; shown as a dead row otherwise,
                    // it invites the user to configure something that will never fire.
                    if (uiState.dataLimitAlert) {
                        SettingItem(
                            label = stringResource(R.string.settings_warn_at),
                            value = stringResource(
                                R.string.measure_percent,
                                uiState.warningThresholdPercent
                            ),
                            onValueClick = actions.onThresholdClick
                        )
                    }

                    SettingItem(
                        label = stringResource(R.string.settings_warn_roaming),
                        isToggle = true,
                        isEnabled = uiState.roamingAlert,
                        onToggleChange = actions.onRoamingAlertChange
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_warn_background_data),
                        isToggle = true,
                        isEnabled = uiState.backgroundDataAlert,
                        onToggleChange = actions.onBackgroundDataAlertChange
                    )

                    if (uiState.backgroundDataAlert) {
                        SettingItem(
                            label = stringResource(R.string.settings_warn_above),
                            value = NetworkUtils.formatBytes(uiState.backgroundDataThresholdBytes),
                            onValueClick = actions.onBackgroundThresholdClick
                        )
                    }
                }


                // Each row is offered only where it leads somewhere, so the section as a whole
                // appears only if at least one of them does.
                if (showAutoStart || showBatteryExemption) {

                    SettingsSection(title = stringResource(R.string.settings_section_device)) {
                        // Says what tapping does and why it matters. "Allow autostart / Open" read
                        // like a setting whose current value was the word "Open".
                        if (showAutoStart) {
                            SettingItem(
                                label = stringResource(R.string.settings_autostart),
                                value = stringResource(R.string.settings_value_open),
                                onValueClick = actions.onAutoStartClick
                            )
                        }

                        // Gone once granted: unlike autostart, this one can be read back, so a
                        // row that is present always has something left to fix.
                        if (showBatteryExemption) {
                            SettingItem(
                                label = stringResource(R.string.settings_battery_unrestricted),
                                value = stringResource(R.string.settings_value_open),
                                // The one row on this screen the user has a reason to act on:
                                // until it is granted the meter can be stopped at any time, and
                                // nothing else here says so.
                                showAttention = true,
                                onValueClick = actions.onBatteryExemptionClick
                            )
                        }
                    }
                }


                // Appearance Section
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    SettingItem(
                        label = stringResource(R.string.settings_language),
                        value = languageName,
                        onValueClick = actions.onLanguageClick
                    )

                    SettingItem(
                        label = stringResource(R.string.settings_dark_theme),
                        isToggle = true,
                        isEnabled = uiState.darkTheme,
                        onToggleChange = actions.onDarkThemeChange
                    )

                    // Wallpaper-derived colour only exists from Android 12; on older devices the row
                    // would be a switch that does nothing, so it is not offered at all.
                    if (supportsDynamicColor) {
                        SettingItem(
                            label = stringResource(R.string.settings_dynamic_colour),
                            isToggle = true,
                            isEnabled = uiState.dynamicColor,
                            onToggleChange = actions.onDynamicColorChange
                        )
                    }

                }

                GradientSettingItem(
                    label = stringResource(R.string.settings_more_apps),
                    value = stringResource(R.string.settings_more_apps_subtitle),
                    icon = Icons.Default.Apps,
                    onClick = actions.onMoreAppsClick
                )
//                SettingsSection(title = "More") {
//
//                }
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
            languageName = "English",
            showBatteryExemption = true,
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
            title = stringResource(R.string.dialog_update_frequency),
            options = viewModel.frequencyOptions.map {
                pluralStringResource(R.plurals.frequency_seconds, it, it)
            },
            selectedIndex = viewModel.frequencyOptions.indexOf(
                // Extract number from current frequency text
                uiState.updateFrequencySeconds
            ),
            onOptionSelected = { index ->
                viewModel.updateUpdateFrequency(viewModel.frequencyOptions[index])
            },
            onDismiss = { viewModel.hideFrequencyDialog() }
        )
    }

    if (showStyleDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_notification_style),
            options = viewModel.styleOptions.map { stringResource(it.labelRes) },
            selectedIndex = viewModel.styleOptions.indexOf(uiState.notificationStyle),
            onOptionSelected = { index ->
                viewModel.updateNotificationStyle(viewModel.styleOptions[index])
            },
            onDismiss = { viewModel.hideStyleDialog() }
        )
    }

    if (showUnitsDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_speed_units),
            options = viewModel.unitsOptions.map { stringResource(it.labelRes) },
            selectedIndex = viewModel.unitsOptions.indexOf(uiState.speedUnit),
            onOptionSelected = { index ->
                viewModel.updateSpeedUnits(viewModel.unitsOptions[index])
            },
            onDismiss = { viewModel.hideUnitsDialog() }
        )
    }

    if (showDisplayModeDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_speed_display),
            options = viewModel.displayModeOptions.map { stringResource(it.labelRes) },
            selectedIndex = viewModel.displayModeOptions.indexOf(uiState.speedDisplayMode),
            onOptionSelected = { index ->
                viewModel.updateSpeedDisplayMode(viewModel.displayModeOptions[index])
            },
            onDismiss = { viewModel.hideDisplayModeDialog() }
        )
    }

    if (showBackgroundThresholdDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_background_threshold),
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
            title = stringResource(R.string.dialog_overlay_size),
            options = viewModel.overlaySizeOptions.map { stringResource(R.string.measure_sp, it) },
            selectedIndex = viewModel.overlaySizeOptions.indexOf(uiState.overlayTextSize),
            onOptionSelected = { index ->
                viewModel.updateOverlayTextSize(viewModel.overlaySizeOptions[index])
            },
            onDismiss = { viewModel.hideOverlaySizeDialog() }
        )
    }

    if (showOverlayColorDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_overlay_colour),
            options = viewModel.overlayColorOptions.map { stringResource(it.second) },
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
            title = stringResource(R.string.dialog_overlay_opacity),
            options = viewModel.overlayOpacityOptions.map {
                if (it == 0) stringResource(R.string.settings_value_none)
                else stringResource(R.string.measure_percent, it)
            },
            selectedIndex = viewModel.overlayOpacityOptions.indexOf(uiState.overlayOpacity),
            onOptionSelected = { index ->
                viewModel.updateOverlayOpacity(viewModel.overlayOpacityOptions[index])
            },
            onDismiss = { viewModel.hideOverlayOpacityDialog() }
        )
    }

    if (showLimitDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_mobile_data_limit),
            options = viewModel.limitOptionsGb.map {
                stringResource(R.string.measure_gigabytes_whole, it)
            },
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
            title = stringResource(R.string.dialog_warn_at),
            options = viewModel.thresholdOptions.map {
                stringResource(R.string.threshold_percent_of_limit, it)
            },
            selectedIndex = viewModel.thresholdOptions.indexOf(uiState.warningThresholdPercent),
            onOptionSelected = { index ->
                viewModel.updateWarningThreshold(viewModel.thresholdOptions[index])
            },
            onDismiss = { viewModel.hideThresholdDialog() }
        )
    }

    if (showDateDialog) {
        SelectionDialog(
            title = stringResource(R.string.dialog_monthly_reset_date),
            options = viewModel.dateOptions.map { date ->
                when {
                    else -> ordinalDay(date)
                }
            },
            selectedIndex = viewModel.dateOptions.indexOf(
                // Extract number from current date text
                uiState.monthlyResetDay
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

/**
 * The publisher's other apps on Play.
 *
 * The Play app first -- it opens the publisher page in place rather than a browser tab -- and the
 * web URL only when Play is not installed, which is also the case on a sideloaded build.
 */
/**
 * The day of the month a cycle starts on.
 *
 * English is the odd one out here: it needs four different suffixes and a rule about teens. Every
 * suffix is its own resource, so a language that writes "1." or "1日" or just "1" supplies one
 * form and ignores the rest rather than having a suffix concatenated onto its number in Kotlin.
 */
@Composable
private fun ordinalDay(date: Int): String = when {
    date % 10 == 1 && date != 11 -> stringResource(R.string.ordinal_day_st, date)
    date % 10 == 2 && date != 12 -> stringResource(R.string.ordinal_day_nd, date)
    date % 10 == 3 && date != 13 -> stringResource(R.string.ordinal_day_rd, date)
    else -> stringResource(R.string.ordinal_day_th, date)
}

private fun openDeveloperPage(context: Context) {
    val publisher = Uri.encode(Constants.PLAY_PUBLISHER)
    val candidates = listOf(
        "market://search?q=pub:$publisher",
        "https://play.google.com/store/apps/developer?id=$publisher"
    )
    candidates.firstOrNull { target ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, target.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
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

