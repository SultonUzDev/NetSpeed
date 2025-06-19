package com.sultonuzdev.netspeed.utils

object Constants {
    const val DB_NAME = "net_speed_db"


    const val CHANNEL_ID = "speed_monitor_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_START_MONITORING = "START_MONITORING"
    const val ACTION_STOP_MONITORING = "STOP_MONITORING"

    // Update interval in milliseconds
    const val DEFAULT_UPDATE_INTERVAL = 1000L
}
enum class NotificationStyle(val styleName: String) {
    COMPACT("Compact"),
    DETAILED("Detailed")

}