package com.sultonuzdev.netspeed.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.sultonuzdev.netspeed.domain.models.NetworkInfo
import com.sultonuzdev.netspeed.domain.models.NetworkSpeed
import com.sultonuzdev.netspeed.domain.models.NetworkType
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.utils.SignalStrengthReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class NetworkRepositoryImpl(
    private val context: Context
) : NetworkRepository {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Real network monitoring variables
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastUpdateTime = 0L
    private var isMonitoring = false

    // Current speeds
    private var currentDownloadSpeed = 0.0
    private var currentUploadSpeed = 0.0


    // Flows for real-time data
    private val _networkSpeed = MutableSharedFlow<NetworkSpeed>(replay = 1)
    private val _networkInfo = MutableSharedFlow<NetworkInfo>(replay = 1)

    override suspend fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true

        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastUpdateTime = System.currentTimeMillis()

        scope.launch {
            while (isMonitoring) {
                try {
                    updateNetworkSpeed()
                    updateNetworkInfo()
                    delay(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    override suspend fun stopMonitoring() {
        isMonitoring = false
    }

    private suspend fun updateNetworkSpeed() {
        val currentTime = System.currentTimeMillis()
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()

        if (lastUpdateTime > 0) {
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // seconds

            if (timeDiff > 0) {
                val rxDiff = currentRxBytes - lastRxBytes
                val txDiff = currentTxBytes - lastTxBytes

                currentDownloadSpeed = rxDiff / timeDiff
                currentUploadSpeed = txDiff / timeDiff

                // Emit the same NetworkSpeed that service uses
                val networkSpeed = NetworkSpeed(
                    downloadSpeed = currentDownloadSpeed,
                    uploadSpeed = currentUploadSpeed,
                    ping = "",
                    timestamp = currentTime
                )

                _networkSpeed.emit(networkSpeed)
            }
        }

        lastRxBytes = currentRxBytes
        lastTxBytes = currentTxBytes
        lastUpdateTime = currentTime
    }

    private suspend fun updateNetworkInfo() {
        val type = currentNetworkType()
        val isWifi = type == NetworkType.WIFI

        val networkInfo = NetworkInfo(
            isConnected = type != NetworkType.NONE,
            networkName = if (type == NetworkType.NONE) "No Connection" else getNetworkName(),
            // 0..4, matching the four bars the Speed screen draws.
            signalStrength = if (type == NetworkType.NONE) {
                0
            } else {
                SignalStrengthReader.level(context, isWifi) ?: 0
            },
            networkType = type
        )

        _networkInfo.emit(networkInfo)
    }

    /**
     * The active transport.
     *
     * This previously read `if (isWifi) WIFI else NONE`, so a device on mobile data reported NONE
     * and the Speed screen claimed there was no connection.
     */
    private fun currentNetworkType(): NetworkType {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    ?: return NetworkType.NONE

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                else -> NetworkType.NONE
            }
        } catch (e: Exception) {
            NetworkType.NONE
        }
    }

    private fun isConnectedToWiFi(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * A display name for the active connection.
     *
     * The SSID needs location permission, which this app never requests. Earlier versions put
     * "Enable Location Permission" / "Enable Location Services" in this field -- a diagnostic
     * message rendered where a network name belongs, and one the user could not act on without
     * a permission prompt the app does not show. It falls back to the plain transport name now.
     */
    private fun getNetworkName(): String {
        return try {
            if (isConnectedToWiFi()) {
                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager

                @Suppress("DEPRECATION")
                val rawSsid = wifiManager.connectionInfo?.ssid.orEmpty()
                val readable = rawSsid.replace("\"", "").trim()

                // Redacted without location permission; the platform returns this placeholder.
                if (readable.isNotBlank() && readable != "<unknown ssid>") readable else "Wi-Fi"
            } else {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Mobile"
            }
        } catch (e: Exception) {
            Log.e("mlog", "getNetworkName: ${e.message}")
            "Unknown"
        }
    }

    override fun getNetworkSpeed(): Flow<NetworkSpeed> = _networkSpeed

    override fun getNetworkInfo(): Flow<NetworkInfo> = _networkInfo
}