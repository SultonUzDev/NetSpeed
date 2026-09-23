package com.sultonuzdev.netspeed.data.datastore


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sultonuzdev.netspeed.utils.NotificationStyle
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import com.sultonuzdev.netspeed.utils.SpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        /** White; the overlay sits on arbitrary content, so a neutral default reads anywhere. */
        const val OVERLAY_DEFAULT_COLOR = 0xFFFFFFFF.toInt()

        val UPDATE_FREQUENCY = intPreferencesKey("update_frequency")
        val NOTIFICATION_STYLE = stringPreferencesKey("notification_style")
        val MONITOR_WIFI = booleanPreferencesKey("monitor_wifi")
        val MONITOR_MOBILE = booleanPreferencesKey("monitor_mobile")
        val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val MONTHLY_RESET_DATE = intPreferencesKey("monthly_reset_date")
        val DATA_LIMIT_ALERT = booleanPreferencesKey("data_limit_alert")
        val DATA_LIMIT = longPreferencesKey("data_limit")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATION_PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
        val SPEED_UNITS = stringPreferencesKey("speed_units")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val WARNING_THRESHOLD = intPreferencesKey("warning_threshold")
        val ALERT_CYCLE_KEY = stringPreferencesKey("alert_cycle_key")
        val ALERT_LEVEL = intPreferencesKey("alert_level")
        val SPEED_DISPLAY_MODE = stringPreferencesKey("speed_display_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // Alerts
        val ROAMING_ALERT = booleanPreferencesKey("roaming_alert")
        val BACKGROUND_DATA_ALERT = booleanPreferencesKey("background_data_alert")
        val BACKGROUND_DATA_THRESHOLD = longPreferencesKey("background_data_threshold")
        val ALERTED_ROAMING = booleanPreferencesKey("alerted_roaming")
        /** "uid:bytes" entries; DataStore has no map type. */
        val APP_LIMITS = stringSetPreferencesKey("app_limits")
        /** Uids already warned about this cycle, prefixed with the cycle key. */
        val ALERTED_APPS = stringSetPreferencesKey("alerted_apps")
        val ALERTED_BACKGROUND = stringSetPreferencesKey("alerted_background")

        // Floating overlay
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
        val OVERLAY_TEXT_SIZE = intPreferencesKey("overlay_text_size")
        val OVERLAY_COLOR = intPreferencesKey("overlay_color")
        val OVERLAY_OPACITY = intPreferencesKey("overlay_opacity")
    }

    val updateFrequency: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[UPDATE_FREQUENCY] ?: 1 }

    val notificationStyle: Flow<NotificationStyle> = context.dataStore.data
        .map { preferences ->
                val result = preferences[NOTIFICATION_STYLE] ?: NotificationStyle.DETAILED.styleName

            val notificationStyle = if (result == NotificationStyle.COMPACT.styleName) {
                NotificationStyle.COMPACT
            } else {
                NotificationStyle.DETAILED

            }
            notificationStyle
        }

    val monitorWifi: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MONITOR_WIFI] != false }

    val monitorMobile: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MONITOR_MOBILE] != false }

    val backgroundMonitoring: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BACKGROUND_MONITORING] != false }

    val monthlyResetDate: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MONTHLY_RESET_DATE] ?: 1 }

    val dataLimitAlert: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DATA_LIMIT_ALERT] == true }

    val dataLimit: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[DATA_LIMIT] ?: (25L * 1024 * 1024 * 1024) } // 25GB

    val darkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_THEME] != false }

    /**
     * Whether the offer to put the speed in the status bar has been turned down.
     *
     * Persisted rather than held in the composition: an offer that returns on every launch is
     * nagging, and the same switch is always available in Settings.
     */
    val notificationPromptDismissed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_PROMPT_DISMISSED] == true }

    /** Warn as soon as the device starts roaming. On by default: the cost is the point. */
    val roamingAlert: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ROAMING_ALERT] != false }

    /** Whether roaming has already been announced for the current roaming session. */
    val alertedRoaming: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALERTED_ROAMING] == true }

    val backgroundDataAlert: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BACKGROUND_DATA_ALERT] == true }

    /** Background bytes in a day that warrant telling the user. Defaults to 200 MB. */
    val backgroundDataThreshold: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[BACKGROUND_DATA_THRESHOLD] ?: (200L * 1024 * 1024) }

    /** Per-app cycle allowances, keyed by uid. */
    val appLimits: Flow<Map<Int, Long>> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LIMITS].orEmpty().mapNotNull { entry ->
                val parts = entry.split(':')
                val uid = parts.getOrNull(0)?.toIntOrNull()
                val bytes = parts.getOrNull(1)?.toLongOrNull()
                if (uid != null && bytes != null) uid to bytes else null
            }.toMap()
        }

    val alertedApps: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[ALERTED_APPS].orEmpty() }

    val alertedBackground: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[ALERTED_BACKGROUND].orEmpty() }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_ENABLED] == true }

    /** Last dragged position, in raw window pixels. */
    val overlayX: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_X] ?: 0 }

    val overlayY: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_Y] ?: 200 }

    /** Text size in sp. */
    val overlayTextSize: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_TEXT_SIZE] ?: 12 }

    /** Fully opaque ARGB; the alpha channel is applied separately from [overlayOpacity]. */
    val overlayColor: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_COLOR] ?: OVERLAY_DEFAULT_COLOR }

    /** Background opacity as a percentage, 0..100. */
    val overlayOpacity: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[OVERLAY_OPACITY] ?: 55 }

    /** Material You wallpaper colours. On by default where the platform supports them. */
    val dynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLOR] != false }

    val speedUnits: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SPEED_UNITS] ?: SpeedUnit.AUTO.prefName }

    /** Typed view of [speedUnits], for callers that need to actually convert a number. */
    val speedUnit: Flow<SpeedUnit> = context.dataStore.data
        .map { preferences -> SpeedUnit.fromPrefName(preferences[SPEED_UNITS]) }

    /** Which speeds the notification shows. */
    val speedDisplayMode: Flow<SpeedDisplayMode> = context.dataStore.data
        .map { preferences -> SpeedDisplayMode.fromPrefName(preferences[SPEED_DISPLAY_MODE]) }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_FIRST_LAUNCH] != false }

    /** User preference: bring monitoring back automatically after a reboot. Defaults to on. */
    val startOnBoot: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[START_ON_BOOT] != false }

    /**
     * Whether monitoring was running when we last had a say. Written by the service, read after a
     * reboot so we only restart for users who had it on -- not for someone who deliberately
     * stopped it before powering off.
     */
    val monitoringEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MONITORING_ENABLED] == true }

    /** Percentage of the cap at which to warn, before the cap itself is reached. */
    val warningThreshold: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[WARNING_THRESHOLD] ?: 80 }

    /**
     * Which billing cycle the last alert belonged to, and how far we escalated in it. Together
     * these make each alert fire once per cycle instead of on every check.
     */
    val alertCycleKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[ALERT_CYCLE_KEY] ?: "" }

    val alertLevel: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[ALERT_LEVEL] ?: 0 }

    suspend fun updateUpdateFrequency(frequency: Int) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_FREQUENCY] = frequency
        }
    }

    suspend fun updateNotificationStyle(style: NotificationStyle) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_STYLE] = style.styleName
        }
    }

    suspend fun updateMonitorWifi(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MONITOR_WIFI] = enabled
        }
    }

    suspend fun updateMonitorMobile(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MONITOR_MOBILE] = enabled
        }
    }

    suspend fun updateBackgroundMonitoring(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_MONITORING] = enabled
        }
    }

    suspend fun updateMonthlyResetDate(date: Int) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_RESET_DATE] = date
        }
    }

    suspend fun updateDataLimitAlert(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DATA_LIMIT_ALERT] = enabled
        }
    }

    suspend fun updateDataLimit(limit: Long) {
        context.dataStore.edit { preferences ->
            preferences[DATA_LIMIT] = limit
        }
    }

    suspend fun updateRoamingAlert(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[ROAMING_ALERT] = enabled }
    }

    suspend fun updateAlertedRoaming(alerted: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALERTED_ROAMING] = alerted }
    }

    suspend fun updateBackgroundDataAlert(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[BACKGROUND_DATA_ALERT] = enabled }
    }

    suspend fun updateBackgroundDataThreshold(bytes: Long) {
        context.dataStore.edit { preferences -> preferences[BACKGROUND_DATA_THRESHOLD] = bytes }
    }

    /** Sets or clears one app's allowance; zero removes it. */
    suspend fun updateAppLimit(uid: Int, bytes: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[APP_LIMITS].orEmpty()
                .filterNot { it.startsWith("$uid:") }
                .toMutableSet()
            if (bytes > 0L) current += "$uid:$bytes"
            preferences[APP_LIMITS] = current
        }
    }

    suspend fun updateAlertedApps(entries: Set<String>) {
        context.dataStore.edit { preferences -> preferences[ALERTED_APPS] = entries }
    }

    suspend fun updateAlertedBackground(entries: Set<String>) {
        context.dataStore.edit { preferences -> preferences[ALERTED_BACKGROUND] = entries }
    }

    suspend fun updateOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun updateOverlayPosition(x: Int, y: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_X] = x
            preferences[OVERLAY_Y] = y
        }
    }

    suspend fun updateOverlayTextSize(sizeSp: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_TEXT_SIZE] = sizeSp
        }
    }

    suspend fun updateOverlayColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_COLOR] = color
        }
    }

    suspend fun updateOverlayOpacity(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_OPACITY] = percent.coerceIn(0, 100)
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun dismissNotificationPrompt() {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_PROMPT_DISMISSED] = true
        }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME] = enabled
        }
    }

    suspend fun updateSpeedUnits(units: String) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_UNITS] = units
        }
    }

    suspend fun updateSpeedDisplayMode(mode: SpeedDisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_DISPLAY_MODE] = mode.prefName
        }
    }

    suspend fun updateFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun updateStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[START_ON_BOOT] = enabled
        }
    }

    suspend fun updateMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MONITORING_ENABLED] = enabled
        }
    }

    suspend fun updateWarningThreshold(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[WARNING_THRESHOLD] = percent
        }
    }

    suspend fun updateAlertState(cycleKey: String, level: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_CYCLE_KEY] = cycleKey
            preferences[ALERT_LEVEL] = level
        }
    }
}