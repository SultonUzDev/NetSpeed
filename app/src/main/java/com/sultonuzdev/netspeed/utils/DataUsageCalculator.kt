package com.sultonuzdev.netspeed.utils


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import java.util.*

object DataUsageCalculator {

    private var todayWifiBytes = 0L
    private var todayMobileBytes = 0L
    private var lastTotalBytes = 0L
    private var lastUpdateTime = 0L
    private var todayStartTime = 0L

    fun initializeTracking(context: Context) {
        val now = System.currentTimeMillis()

        if (todayStartTime == 0L || !isSameDay(todayStartTime, now)) {
            resetTodayTracking()
        }

        lastTotalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
        lastUpdateTime = now
    }

    fun updateUsage(context: Context): TodayUsageData {
        val now = System.currentTimeMillis()
        val currentTotalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()

        // Calculate new data used since last update
        val newDataUsed = maxOf(0L, currentTotalBytes - lastTotalBytes)

        // Only add to counter if significant data was used (avoid noise)
        if (newDataUsed > 100) { // 100 bytes threshold
            val currentNetworkType = getCurrentNetworkType(context)

            when (currentNetworkType) {
                "WIFI" -> todayWifiBytes += newDataUsed
                "MOBILE" -> todayMobileBytes += newDataUsed
            }
        }

        // Update tracking variables
        lastTotalBytes = currentTotalBytes
        lastUpdateTime = now

        return TodayUsageData(
            totalBytes = todayWifiBytes + todayMobileBytes,
            wifiBytes = todayWifiBytes,
            mobileBytes = todayMobileBytes
        )
    }

    private fun getCurrentNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "MOBILE"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun resetTodayTracking() {
        todayStartTime = getStartOfDayTimestamp()
        todayWifiBytes = 0L
        todayMobileBytes = 0L
        lastTotalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

data class TodayUsageData(
    val totalBytes: Long,
    val wifiBytes: Long,
    val mobileBytes: Long
)
