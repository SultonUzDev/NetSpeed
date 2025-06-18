package com.sultonuzdev.netspeed.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.presentation.components.SettingItem
import com.sultonuzdev.netspeed.presentation.components.SelectionDialog
import com.sultonuzdev.netspeed.presentation.theme.*
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryVariant
            )
            Text(
                text = "Customize your monitoring preferences",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // Notification Section
            SettingsSection(title = "NOTIFICATION") {
                SettingItem(
                    label = "Show Speed Notification",
                    description = "Display real-time speed in notification bar",
                    isToggle = true,
                    isEnabled = uiState.speedNotificationEnabled,
                    onToggleChange = { viewModel.updateSpeedNotification(it) }
                )

                SettingItem(
                    label = "Update Frequency",
                    description = "How often to refresh speed data",
                    value = uiState.updateFrequency,
                    onValueClick = { viewModel.showFrequencyDialog() }
                )

                SettingItem(
                    label = "Notification Style",
                    description = "Choose compact or detailed view",
                    value = uiState.notificationStyle,
                    onValueClick = { viewModel.showStyleDialog() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Monitoring Section
            SettingsSection(title = "MONITORING") {
                SettingItem(
                    label = "Monitor WiFi",
                    description = "Track WiFi speed and usage",
                    isToggle = true,
                    isEnabled = uiState.monitorWifi,
                    onToggleChange = { viewModel.updateMonitorWifi(it) }
                )

                SettingItem(
                    label = "Monitor Mobile Data",
                    description = "Track mobile data speed and usage",
                    isToggle = true,
                    isEnabled = uiState.monitorMobile,
                    onToggleChange = { viewModel.updateMonitorMobile(it) }
                )

                SettingItem(
                    label = "Background Monitoring",
                    description = "Continue monitoring when app is closed",
                    isToggle = true,
                    isEnabled = uiState.backgroundMonitoring,
                    onToggleChange = { viewModel.updateBackgroundMonitoring(it) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Data & Privacy Section
            SettingsSection(title = "DATA & PRIVACY") {
                SettingItem(
                    label = "Monthly Reset Date",
                    description = "When to reset monthly usage counter",
                    value = uiState.monthlyResetDate,
                    onValueClick = { viewModel.showDateDialog() }
                )

                SettingItem(
                    label = "Data Limit Alert",
                    description = "Warn when approaching data limit",
                    isToggle = true,
                    isEnabled = uiState.dataLimitAlert,
                    onToggleChange = { viewModel.updateDataLimitAlert(it) }
                )

                SettingItem(
                    label = "Export Usage Data",
                    description = "Export your usage history",
                    value = "Export",
                    onValueClick = {
                        // TODO: Implement export functionality
                        // This could open a file picker or sharing dialog
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Appearance Section
            SettingsSection(title = "APPEARANCE") {
                SettingItem(
                    label = "Dark Theme",
                    description = "Use dark theme throughout the app",
                    isToggle = true,
                    isEnabled = uiState.darkTheme,
                    onToggleChange = { viewModel.updateDarkTheme(it) }
                )

                SettingItem(
                    label = "Speed Units",
                    description = "Choose preferred speed units",
                    value = uiState.speedUnits,
                    onValueClick = { viewModel.showUnitsDialog() }
                )
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
            options = viewModel.styleOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedIndex = viewModel.styleOptions.indexOf(
                uiState.notificationStyle.lowercase()
            ),
            onOptionSelected = { index ->
                viewModel.updateNotificationStyle(viewModel.styleOptions[index])
            },
            onDismiss = { viewModel.hideStyleDialog() }
        )
    }

    if (showUnitsDialog) {
        SelectionDialog(
            title = "Speed Units",
            options = viewModel.unitsOptions.map { units ->
                when (units) {
                    "auto" -> "Auto"
                    "mbps" -> "Mbps"
                    "kbps" -> "Kbps"
                    "mb/s" -> "MB/s"
                    "kb/s" -> "KB/s"
                    else -> units.replaceFirstChar { it.uppercase() }
                }
            },
            selectedIndex = viewModel.unitsOptions.indexOf(
                when (uiState.speedUnits.lowercase()) {
                    "auto" -> "auto"
                    "mbps" -> "mbps"
                    "kbps" -> "kbps"
                    "mb/s" -> "mb/s"
                    "kb/s" -> "kb/s"
                    else -> "auto"
                }
            ),
            onOptionSelected = { index ->
                viewModel.updateSpeedUnits(viewModel.unitsOptions[index])
            },
            onDismiss = { viewModel.hideUnitsDialog() }
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
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        content()
    }
}