package com.sultonuzdev.netspeed.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.presentation.MainActivity
import com.sultonuzdev.netspeed.utils.Constants.ALERT_CHANNEL_ID
import com.sultonuzdev.netspeed.utils.Constants.ALERT_NOTIFICATION_ID

/** Posts data-cap warnings on their own channel, separate from the ongoing speed notification. */
object DataLimitNotifier {

    fun notify(context: Context, status: DataLimitStatus) {
        if (status.level == DataLimitLevel.NONE) return

        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            ALERT_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val used = NetworkUtils.formatBytes(status.usedBytes)
        val limit = NetworkUtils.formatBytes(status.limitBytes)

        val title: String
        val text: String
        when (status.level) {
            DataLimitLevel.REACHED -> {
                title = "Mobile data limit reached"
                text = "$used of $limit used this cycle."
            }

            else -> {
                title = "${status.percentUsed}% of mobile data used"
                text = "$used of $limit used. ${NetworkUtils.formatBytes(status.remainingBytes)} left this cycle."
            }
        }

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_usage)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(ALERT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+; nothing to do but skip the alert.
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Data limit alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warns when mobile data approaches or passes your limit"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
