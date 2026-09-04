package com.sultonuzdev.netspeed.utils

object Constants {
    const val DB_NAME = "net_speed_db"


    /**
     * Bumped from "speed_monitor_channel": a channel's importance is fixed once created, so the
     * only way to demote the old HIGH-importance channel on existing installs is to move to a new
     * one and delete the old.
     */
    const val CHANNEL_ID = "speed_monitor_channel_v2"
    const val LEGACY_CHANNEL_ID = "speed_monitor_channel"
    const val NOTIFICATION_ID = 1001

    // Data-cap alerts get their own channel: the monitor notification is silent and ongoing,
    // while an alert is a one-off the user is meant to notice and dismiss.
    const val ALERT_CHANNEL_ID = "data_limit_alert_channel"

    // Distinct ids so one alert never replaces another: a roaming warning and a cap warning are
    // about different things and may both be relevant at once.
    const val ALERT_NOTIFICATION_ID = 1002
    const val ROAMING_NOTIFICATION_ID = 1003
    const val BACKGROUND_DATA_NOTIFICATION_ID = 1004
    const val APP_LIMIT_NOTIFICATION_ID = 1005
    const val ACTION_START_MONITORING = "START_MONITORING"
    const val ACTION_STOP_MONITORING = "STOP_MONITORING"

    // Update interval in milliseconds
    const val DEFAULT_UPDATE_INTERVAL = 1000L
}
enum class NotificationStyle(val styleName: String) {
    COMPACT("Compact"),
    DETAILED("Detailed")

}