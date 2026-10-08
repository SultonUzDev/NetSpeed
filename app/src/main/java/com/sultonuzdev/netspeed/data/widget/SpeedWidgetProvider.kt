package com.sultonuzdev.netspeed.data.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import androidx.core.text.BidiFormatter
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.presentation.MainActivity
import com.sultonuzdev.netspeed.utils.FormattedSpeed

/**
 * Home-screen widget showing live speed.
 *
 * `updatePeriodMillis` is 0 in the provider info: the platform's own update cycle is capped at
 * 30 minutes, which is useless for a live speed readout. The monitoring service pushes updates
 * instead, via [updateAll], and only while it is running.
 */
class SpeedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Placeholder content until the service pushes real numbers; without this a freshly
        // placed widget would sit blank until the next sample.
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, baseViews(context))
        }
    }

    companion object {

        private fun baseViews(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_speed).apply {
                setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
            }

        private fun launchIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Pushes the current figures to every placed widget. A no-op when none exist, so the
         * service pays nothing for a feature the user has not used.
         */
        fun updateAll(
            context: Context,
            download: FormattedSpeed,
            upload: FormattedSpeed,
            todayTotal: String
        ) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, SpeedWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return

                val views = baseViews(context).apply {
                    setTextViewText(
                        R.id.widget_download,
                        context.getString(R.string.notif_down_only, figure(download))
                    )
                    setTextViewText(
                        R.id.widget_upload,
                        context.getString(R.string.notif_up_only, figure(upload))
                    )
                    setTextViewText(
                        R.id.widget_today,
                        context.getString(R.string.widget_today_total, figure(todayTotal))
                    )
                }
                manager.updateAppWidget(ids, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Isolates a measurement so bidirectional reordering cannot take it apart: "12.4 Mbps" is a
 * left-to-right run, and without isolate marks the bidi algorithm may move its pieces relative
 * to surrounding right-to-left text.
 */
private fun figure(value: Any): String =
    BidiFormatter.getInstance().unicodeWrap(value.toString())
