package com.sultonuzdev.netspeed.data.datastore


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sultonuzdev.netspeed.utils.NotificationStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val SPEED_NOTIFICATION_ENABLED = booleanPreferencesKey("speed_notification_enabled")
        val UPDATE_FREQUENCY = intPreferencesKey("update_frequency")
        val NOTIFICATION_STYLE = stringPreferencesKey("notification_style")
        val MONITOR_WIFI = booleanPreferencesKey("monitor_wifi")
        val MONITOR_MOBILE = booleanPreferencesKey("monitor_mobile")
        val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val MONTHLY_RESET_DATE = intPreferencesKey("monthly_reset_date")
        val DATA_LIMIT_ALERT = booleanPreferencesKey("data_limit_alert")
        val DATA_LIMIT = longPreferencesKey("data_limit")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SPEED_UNITS = stringPreferencesKey("speed_units")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    val speedNotificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SPEED_NOTIFICATION_ENABLED] == true }

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

    val speedUnits: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SPEED_UNITS] ?: "auto" }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_FIRST_LAUNCH] != false }

    suspend fun updateSpeedNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_NOTIFICATION_ENABLED] = enabled
        }
    }

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

    suspend fun updateFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = isFirst
        }
    }
}