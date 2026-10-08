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
import com.sultonuzdev.netspeed.utils.Constants.APP_LIMIT_NOTIFICATION_ID
import com.sultonuzdev.netspeed.utils.Constants.BACKGROUND_DATA_NOTIFICATION_ID
import com.sultonuzdev.netspeed.utils.Constants.ROAMING_NOTIFICATION_ID

/**
 * Posts one-off warnings on their own channel, separate from the ongoing speed notification.
 *
 * Each kind of alert gets its own notification id, so a roaming warning does not replace a cap
 * warning -- they are about different things and can both matter at the same moment.
 */
object DataLimitNotifier {

    /** Roaming has started: the most expensive thing that can happen without the user acting. */
    fun notifyRoaming(context: Context, mobileUsedThisCycle: Long) {
        post(
            context = context,
            id = ROAMING_NOTIFICATION_ID,
            title = context.getString(R.string.alert_roaming_title),
            text = context.getString(
                R.string.alert_roaming_body,
                NetworkUtils.formatBytes(mobileUsedThisCycle)
            )
        )
    }

    /**
     * Apps that moved data while the user was not in them.
     *
     * Takes the whole batch rather than one app at a time: they share a notification id, so
     * posting them individually meant each overwrote the last and only the final app was ever
     * seen. One summary is also less noisy than three separate warnings.
     */
    fun notifyBackgroundData(context: Context, apps: List<Pair<String, Long>>) {
        if (apps.isEmpty()) return

        // Singular names the app; plural counts them. Two different sentences, so the single
        // case is its own string rather than a plural form that would have to name an app it has
        // no slot for.
        val title = if (apps.size == 1) {
            context.getString(R.string.alert_background_one_title, apps.first().first)
        } else {
            context.resources.getQuantityString(
                R.plurals.alert_background_title, apps.size, apps.size
            )
        }
        val text = apps.joinToString("\n") { (label, bytes) ->
            context.getString(R.string.alert_app_line, label, NetworkUtils.formatBytes(bytes))
        }

        post(context, BACKGROUND_DATA_NOTIFICATION_ID, title, text)
    }

    /** Per-app allowances that have been passed; batched for the same reason as above. */
    fun notifyAppLimit(context: Context, apps: List<AppLimitBreach>) {
        if (apps.isEmpty()) return

        val title = if (apps.size == 1) {
            context.getString(R.string.alert_app_limit_one_title, apps.first().appLabel)
        } else {
            context.resources.getQuantityString(
                R.plurals.alert_app_limit_title, apps.size, apps.size
            )
        }
        val text = apps.joinToString("\n") { breach ->
            context.getString(
                R.string.alert_app_line_of_limit,
                breach.appLabel,
                NetworkUtils.formatBytes(breach.used),
                NetworkUtils.formatBytes(breach.limit)
            )
        }

        post(context, APP_LIMIT_NOTIFICATION_ID, title, text)
    }

    data class AppLimitBreach(val appLabel: String, val used: Long, val limit: Long)

    private fun post(context: Context, id: Int, title: String, text: String) {
        createChannel(context)

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_usage)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(context, id))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+; nothing to do but skip the alert.
        }
    }

    private fun contentIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
                title = context.getString(R.string.alert_limit_reached_title)
                text = context.getString(R.string.alert_limit_reached_body, used, limit)
            }

            else -> {
                title = context.getString(R.string.alert_approaching_title, status.percentUsed)
                text = context.getString(
                    R.string.alert_approaching_body,
                    used,
                    limit,
                    NetworkUtils.formatBytes(status.remainingBytes)
                )
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
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alert_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
