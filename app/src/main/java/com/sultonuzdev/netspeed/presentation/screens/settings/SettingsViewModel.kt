package com.sultonuzdev.netspeed.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.presentation.screens.settings.contract.SettingsUiState
import com.sultonuzdev.netspeed.utils.NotificationStyle
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import com.sultonuzdev.netspeed.utils.SpeedUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _showLimitDialog = MutableStateFlow(false)
    val showLimitDialog: StateFlow<Boolean> = _showLimitDialog.asStateFlow()

    private val _showThresholdDialog = MutableStateFlow(false)
    val showThresholdDialog: StateFlow<Boolean> = _showThresholdDialog.asStateFlow()

    private val _showDisplayModeDialog = MutableStateFlow(false)
    val showDisplayModeDialog: StateFlow<Boolean> = _showDisplayModeDialog.asStateFlow()

    private val _showBackgroundThresholdDialog = MutableStateFlow(false)
    val showBackgroundThresholdDialog: StateFlow<Boolean> =
        _showBackgroundThresholdDialog.asStateFlow()

    private val _showOverlaySizeDialog = MutableStateFlow(false)
    val showOverlaySizeDialog: StateFlow<Boolean> = _showOverlaySizeDialog.asStateFlow()

    private val _showOverlayColorDialog = MutableStateFlow(false)
    val showOverlayColorDialog: StateFlow<Boolean> = _showOverlayColorDialog.asStateFlow()

    private val _showOverlayOpacityDialog = MutableStateFlow(false)
    val showOverlayOpacityDialog: StateFlow<Boolean> = _showOverlayOpacityDialog.asStateFlow()

    // Available options
    val frequencyOptions = listOf(1, 2, 3, 5, 10)
    val styleOptions = listOf(NotificationStyle.COMPACT, NotificationStyle.DETAILED)
    val unitsOptions = SpeedUnit.entries
    val displayModeOptions = SpeedDisplayMode.entries

    /** Background bytes per day that warrant a warning. */
    val backgroundThresholdOptions = listOf(
        50L * 1024 * 1024,
        100L * 1024 * 1024,
        200L * 1024 * 1024,
        500L * 1024 * 1024,
        1024L * 1024 * 1024
    )

    val overlaySizeOptions = listOf(10, 12, 14, 16, 18, 22)
    val overlayOpacityOptions = listOf(0, 25, 40, 55, 70, 85, 100)

    /** A small named palette: a full colour picker is more UI than this setting deserves. */
    val overlayColorOptions: List<Pair<Int, Int>> = listOf(
        0xFFFFFFFF.toInt() to R.string.colour_white,
        0xFF00E5FF.toInt() to R.string.colour_cyan,
        0xFF69F0AE.toInt() to R.string.colour_green,
        0xFFFFD54F.toInt() to R.string.colour_amber,
        0xFFFF8A80.toInt() to R.string.colour_red,
        0xFF000000.toInt() to R.string.colour_black
    )
    val dateOptions = (1..28).toList()

    /** Cap choices in whole GB. */
    val limitOptionsGb = listOf(1, 2, 3, 5, 8, 10, 15, 20, 25, 30, 40, 50, 75, 100, 150, 200)

    /** Percentages of the cap at which to warn. */
    val thresholdOptions = listOf(50, 60, 70, 75, 80, 85, 90, 95)

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            // Explicit <Any, SettingsUiState>: the varargs combine reifies T, and Kotlin 2.4
            // infers the intersection of the flows' unrelated types (Boolean, Int, String,
            // NotificationStyle) rather than a common supertype. Naming Any keeps the casts
            // below honest and the inference predictable.
            val base = combine<Any, SettingsUiState>(
                preferencesManager.monitoringEnabled,
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
                    monitoringEnabled = values[0] as Boolean,
                    updateFrequencySeconds = values[1] as Int,
                    notificationStyle = values[2] as NotificationStyle,
                    monitorWifi = values[3] as Boolean,
                    monitorMobile = values[4] as Boolean,
                    backgroundMonitoring = values[5] as Boolean,
                    monthlyResetDay = values[6] as Int,
                    dataLimitAlert = values[7] as Boolean,
                    darkTheme = values[8] as Boolean,
                    speedUnit = SpeedUnit.fromPrefName(values[9] as String)
                )
            }

            // combine()'s typed overloads stop at five flows, so the rest are layered on.
            val withMonitoring = combine(
                base,
                preferencesManager.startOnBoot,
                preferencesManager.dataLimit,
                preferencesManager.warningThreshold,
                preferencesManager.speedDisplayMode
            ) { state, startOnBoot, limit, threshold, displayMode ->
                state.copy(
                    startOnBoot = startOnBoot,
                    dataLimitBytes = limit,
                    warningThresholdPercent = threshold,
                    speedDisplayMode = displayMode
                )
            }

            val withAppearance =
                combine(withMonitoring, preferencesManager.dynamicColor) { state, dynamicColor ->
                    state.copy(dynamicColor = dynamicColor)
                }

            val withAlerts = combine(
                withAppearance,
                preferencesManager.roamingAlert,
                preferencesManager.backgroundDataAlert,
                preferencesManager.backgroundDataThreshold
            ) { state, roaming, backgroundAlert, threshold ->
                state.copy(
                    roamingAlert = roaming,
                    backgroundDataAlert = backgroundAlert,
                    backgroundDataThresholdBytes = threshold
                )
            }

            combine(
                withAlerts,
                preferencesManager.overlayEnabled,
                preferencesManager.overlayTextSize,
                preferencesManager.overlayColor,
                preferencesManager.overlayOpacity
            ) { state, enabled, textSize, color, opacity ->
                state.copy(
                    overlayEnabled = enabled,
                    overlayTextSize = textSize,
                    overlayColor = color,
                    overlayColorLabel = overlayColorOptions
                        .firstOrNull { it.first == color }?.second ?: R.string.option_custom,
                    overlayOpacity = opacity
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // Dialog control functions
    fun showFrequencyDialog() {
        _showFrequencyDialog.value = true
    }

    fun hideFrequencyDialog() {
        _showFrequencyDialog.value = false
    }

    fun showStyleDialog() {
        _showStyleDialog.value = true
    }

    fun hideStyleDialog() {
        _showStyleDialog.value = false
    }

    fun showUnitsDialog() {
        _showUnitsDialog.value = true
    }

    fun hideUnitsDialog() {
        _showUnitsDialog.value = false
    }

    fun showDateDialog() {
        _showDateDialog.value = true
    }

    fun hideDateDialog() {
        _showDateDialog.value = false
    }

    fun showLimitDialog() {
        _showLimitDialog.value = true
    }

    fun hideLimitDialog() {
        _showLimitDialog.value = false
    }

    fun showThresholdDialog() {
        _showThresholdDialog.value = true
    }

    fun hideThresholdDialog() {
        _showThresholdDialog.value = false
    }

    fun showDisplayModeDialog() {
        _showDisplayModeDialog.value = true
    }

    fun hideDisplayModeDialog() {
        _showDisplayModeDialog.value = false
    }

    // Update functions
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

    fun updateStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateStartOnBoot(enabled)
        }
    }

    fun updateDataLimitGb(gigabytes: Int) {
        viewModelScope.launch {
            preferencesManager.updateDataLimit(gigabytes.toLong() * 1024 * 1024 * 1024)
            hideLimitDialog()
        }
    }

    fun updateWarningThreshold(percent: Int) {
        viewModelScope.launch {
            preferencesManager.updateWarningThreshold(percent)
            hideThresholdDialog()
        }
    }

    fun updateDataLimitAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateDataLimitAlert(enabled)
        }
    }

    fun showOverlaySizeDialog() {
        _showOverlaySizeDialog.value = true
    }

    fun hideOverlaySizeDialog() {
        _showOverlaySizeDialog.value = false
    }

    fun showOverlayColorDialog() {
        _showOverlayColorDialog.value = true
    }

    fun hideOverlayColorDialog() {
        _showOverlayColorDialog.value = false
    }

    fun showOverlayOpacityDialog() {
        _showOverlayOpacityDialog.value = true
    }

    fun hideOverlayOpacityDialog() {
        _showOverlayOpacityDialog.value = false
    }

    fun showBackgroundThresholdDialog() {
        _showBackgroundThresholdDialog.value = true
    }

    fun hideBackgroundThresholdDialog() {
        _showBackgroundThresholdDialog.value = false
    }

    fun updateRoamingAlert(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.updateRoamingAlert(enabled) }
    }

    fun updateBackgroundDataAlert(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.updateBackgroundDataAlert(enabled) }
    }

    fun updateBackgroundDataThreshold(bytes: Long) {
        viewModelScope.launch {
            preferencesManager.updateBackgroundDataThreshold(bytes)
            hideBackgroundThresholdDialog()
        }
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateOverlayEnabled(enabled)
        }
    }

    fun updateOverlayTextSize(sizeSp: Int) {
        viewModelScope.launch {
            preferencesManager.updateOverlayTextSize(sizeSp)
            hideOverlaySizeDialog()
        }
    }

    fun updateOverlayColor(color: Int) {
        viewModelScope.launch {
            preferencesManager.updateOverlayColor(color)
            hideOverlayColorDialog()
        }
    }

    fun updateOverlayOpacity(percent: Int) {
        viewModelScope.launch {
            preferencesManager.updateOverlayOpacity(percent)
            hideOverlayOpacityDialog()
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateDynamicColor(enabled)
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

    fun updateNotificationStyle(style: NotificationStyle) {
        viewModelScope.launch {
            preferencesManager.updateNotificationStyle(style)
            hideStyleDialog()
        }
    }

    fun updateMonthlyResetDate(date: Int) {
        viewModelScope.launch {
            preferencesManager.updateMonthlyResetDate(date)
            hideDateDialog()
        }
    }

    fun updateSpeedUnits(unit: SpeedUnit) {
        viewModelScope.launch {
            preferencesManager.updateSpeedUnits(unit.prefName)
            hideUnitsDialog()
        }
    }

    fun updateSpeedDisplayMode(mode: SpeedDisplayMode) {
        viewModelScope.launch {
            preferencesManager.updateSpeedDisplayMode(mode)
            hideDisplayModeDialog()
        }
    }

    // Formatting helper functions
}