package com.sultonuzdev.netspeed.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Reads real signal strength, replacing the placeholder constants that used to be returned for
 * mobile. Everything is expressed on the platform's own 0..4 scale.
 *
 * Returns null rather than a plausible-looking number whenever the platform will not say — an
 * invented value is worse than an honest blank, because the user cannot tell the two apart.
 */
object SignalStrengthReader {

    const val MAX_LEVEL = 4

    /** Signal on a 0..[MAX_LEVEL] scale, or null if unavailable. */
    fun level(context: Context, isWifi: Boolean): Int? =
        if (isWifi) wifiLevel(context) else mobileLevel(context)

    /** Signal as a rough percentage, or null if unavailable. */
    fun percent(context: Context, isWifi: Boolean): Int? =
        level(context, isWifi)?.let { it * 100 / MAX_LEVEL }

    private fun wifiLevel(context: Context): Int? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val rssi = wifiManager.connectionInfo?.rssi ?: return null

            // RSSI of 0 or the sentinel minimum means "not associated", not "perfect signal".
            if (rssi == 0 || rssi <= -127) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // The platform knows its own bucket count, which is not always 5.
                val maxLevel = wifiManager.maxSignalLevel
                val level = wifiManager.calculateSignalLevel(rssi)
                if (maxLevel <= 0) null else level * MAX_LEVEL / maxLevel
            } else {
                @Suppress("DEPRECATION")
                WifiManager.calculateSignalLevel(rssi, MAX_LEVEL + 1)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mobileLevel(context: Context): Int? {
        // getSignalStrength() arrived in API 28; below that there is no non-listener way to ask.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        return try {
            val telephonyManager = context
                .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.signalStrength?.level?.coerceIn(0, MAX_LEVEL)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
