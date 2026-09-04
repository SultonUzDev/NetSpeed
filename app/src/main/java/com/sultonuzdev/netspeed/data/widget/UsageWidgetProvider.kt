package com.sultonuzdev.netspeed.data.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.domain.models.UsageForecast
import com.sultonuzdev.netspeed.presentation.MainActivity
import com.sultonuzdev.netspeed.utils.NetworkUtils

/**
 * Home-screen widget for data usage against the cap.
 *
 * The speed widget duplicates what the status bar already shows permanently; this one answers the
 * question a glance at the home screen is actually for -- how much of the allowance is left, and
 * whether the current rate will blow through it.
 */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, baseViews(context))
        }
    }

    companion object {

        private fun baseViews(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_usage).apply {
                setOnClickPendingIntent(R.id.usage_widget_root, launchIntent(context))
            }

        private fun launchIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** No-op when no widget is placed, so the service pays nothing for an unused feature. */
        fun updateAll(context: Context, forecast: UsageForecast?) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, UsageWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return

                val views = baseViews(context).apply {
                    if (forecast == null) {
                        setTextViewText(R.id.usage_widget_total, "--")
                        setTextViewText(R.id.usage_widget_caption, "No limit set")
                        setProgressBar(R.id.usage_widget_progress, 100, 0, false)
                    } else {
                        val percent = if (forecast.limitBytes > 0L) {
                            ((forecast.usedBytes * 100) / forecast.limitBytes)
                                .coerceIn(0L, 100L).toInt()
                        } else 0

                        setTextViewText(
                            R.id.usage_widget_total,
                            NetworkUtils.formatBytes(forecast.usedBytes)
                        )
                        setProgressBar(R.id.usage_widget_progress, 100, percent, false)
                        setTextViewText(
                            R.id.usage_widget_caption,
                            captionFor(forecast, percent)
                        )
                    }
                }
                manager.updateAppWidget(ids, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun captionFor(forecast: UsageForecast, percent: Int): String {
            val ofLimit = "$percent% of ${NetworkUtils.formatBytes(forecast.limitBytes)}"
            return when {
                forecast.willExceed && forecast.daysUntilLimit != null ->
                    "$ofLimit · limit in ${forecast.daysUntilLimit}d"

                forecast.willExceed ->
                    "$ofLimit · heading over"

                else -> "$ofLimit · ${forecast.daysRemaining}d left"
            }
        }
    }
}
