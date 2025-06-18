package com.sultonuzdev.netspeed.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Dialog states
    private val _showFrequencyDialog = MutableStateFlow(false)
    val showFrequencyDialog: StateFlow<Boolean> = _showFrequencyDialog.asStateFlow()

    private val _showStyleDialog = MutableStateFlow(false)
    val showStyleDialog: StateFlow<Boolean> = _showStyleDialog.asStateFlow()

    private val _showUnitsDialog = MutableStateFlow(false)
    val showUnitsDialog: StateFlow<Boolean> = _showUnitsDialog.asStateFlow()

    private val _showDateDialog = MutableStateFlow(false)
    val showDateDialog: StateFlow<Boolean> = _showDateDialog.asStateFlow()

    // Available options
    val frequencyOptions = listOf(1, 2, 3, 5, 10)
    val styleOptions = listOf("compact", "detailed")
    val unitsOptions = listOf("auto", "mbps", "kbps", "mb/s", "kb/s")
    val dateOptions = (1..28).toList()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                preferencesManager.speedNotificationEnabled,
                preferencesManager.updateFrequency,
                preferencesManager.notificationStyle,
                preferencesManager.monitorWifi,
                preferencesManager.monitorMobile,
                preferencesManager.backgroundMonitoring,
                preferencesManager.monthlyResetDate,
                preferencesManager.dataLimitAlert,
                preferencesManager.darkTheme,
                preferencesManager.speedUnits
            ) { values ->
                SettingsUiState(
                    speedNotificationEnabled = values[0] as Boolean,
                    updateFrequency = formatFrequencyText(values[1] as Int),
                    notificationStyle = formatStyleText(values[2] as String),
                    monitorWifi = values[3] as Boolean,
                    monitorMobile = values[4] as Boolean,
                    backgroundMonitoring = values[5] as Boolean,
                    monthlyResetDate = formatDateText(values[6] as Int),
                    dataLimitAlert = values[7] as Boolean,
                    darkTheme = values[8] as Boolean,
                    speedUnits = formatUnitsText(values[9] as String)
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // Dialog control functions
    fun showFrequencyDialog() { _showFrequencyDialog.value = true }
    fun hideFrequencyDialog() { _showFrequencyDialog.value = false }

    fun showStyleDialog() { _showStyleDialog.value = true }
    fun hideStyleDialog() { _showStyleDialog.value = false }

    fun showUnitsDialog() { _showUnitsDialog.value = true }
    fun hideUnitsDialog() { _showUnitsDialog.value = false }

    fun showDateDialog() { _showDateDialog.value = true }
    fun hideDateDialog() { _showDateDialog.value = false }

    // Update functions
    fun updateSpeedNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSpeedNotificationEnabled(enabled)
        }
    }

    fun updateMonitorWifi(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateMonitorWifi(enabled)
        }
    }

    fun updateMonitorMobile(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateMonitorMobile(enabled)
        }
    }

    fun updateBackgroundMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateBackgroundMonitoring(enabled)
        }
    }

    fun updateDataLimitAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateDataLimitAlert(enabled)
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateDarkTheme(enabled)
        }
    }

    fun updateUpdateFrequency(frequency: Int) {
        viewModelScope.launch {
            preferencesManager.updateUpdateFrequency(frequency)
            hideFrequencyDialog()
        }
    }

    fun updateNotificationStyle(style: String) {
        viewModelScope.launch {
            preferencesManager.updateNotificationStyle(style.lowercase())
            hideStyleDialog()
        }
    }

    fun updateMonthlyResetDate(date: Int) {
        viewModelScope.launch {
            preferencesManager.updateMonthlyResetDate(date)
            hideDateDialog()
        }
    }

    fun updateSpeedUnits(units: String) {
        viewModelScope.launch {
            preferencesManager.updateSpeedUnits(units.lowercase())
            hideUnitsDialog()
        }
    }

    // Formatting helper functions
    private fun formatFrequencyText(frequency: Int): String {
        return if (frequency == 1) "1 second" else "$frequency seconds"
    }

    private fun formatStyleText(style: String): String {
        return style.replaceFirstChar { it.uppercase() }
    }

    private fun formatUnitsText(units: String): String {
        return when (units.lowercase()) {
            "auto" -> "Auto"
            "mbps" -> "Mbps"
            "kbps" -> "Kbps"
            "mb/s" -> "MB/s"
            "kb/s" -> "KB/s"
            else -> units.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatDateText(date: Int): String {
        return when {
            date % 10 == 1 && date != 11 -> "${date}st"
            date % 10 == 2 && date != 12 -> "${date}nd"
            date % 10 == 3 && date != 13 -> "${date}rd"
            else -> "${date}th"
        }
    }
}