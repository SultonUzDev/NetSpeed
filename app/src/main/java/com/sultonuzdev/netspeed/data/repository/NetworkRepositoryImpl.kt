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
import com.sultonuzdev.netspeed.utils.PingCalculator
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
    private var currentPing = "N/A"


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

        // Calculate ping in background (less frequently)
        scope.launch {
            while (isMonitoring) {
                try {
                    currentPing = PingCalculator.simplePing()
                    delay(5000) // Update ping every 5 seconds
                } catch (e: Exception) {
                    currentPing = "N/A"
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
                    ping = currentPing, // You can implement ping calculation
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
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val isWifi = isConnectedToWiFi()
        val signalStrength = if (isWifi) getWiFiSignalStrength() else getMobileSignalStrength()
        val networkName = getNetworkName()

        val networkInfo = NetworkInfo(
            isConnected = true,
            networkName = networkName,
            signalStrength = (signalStrength / 25).coerceAtMost(4), // Convert to 1-4 scale
            networkType = if (isWifi) NetworkType.WIFI else NetworkType.NONE

        )

        _networkInfo.emit(networkInfo)
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

    private fun getWiFiSignalStrength(): Int {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val rssi = wifiInfo.rssi

            when {
                rssi >= -50 -> 100
                rssi >= -60 -> 75
                rssi >= -70 -> 50
                rssi >= -80 -> 25
                else -> 10
            }
        } catch (e: Exception) {
            50
        }
    }

    private fun getMobileSignalStrength(): Int {
        return try {
            75 // Placeholder - implement based on your needs
        } catch (e: Exception) {
            50
        }
    }

    private fun getNetworkName(): String {
        try {
            if (isConnectedToWiFi()) {
                // Check if location permission is granted (required for SSID since Android 8.1/O)
                val fineLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val isLocationPermissionGranted = fineLocationGranted || coarseLocationGranted

                // Some devices require that location services be enabled too
                val isLocationEnabled = try {
                    val locationManager =
                        context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                    locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                } catch (e: Exception) {
                    true // Default to true if unable to check
                }

                val ssid =
                    if (isLocationPermissionGranted && isLocationEnabled && wifiInfo != null && wifiInfo.supplicantState == android.net.wifi.SupplicantState.COMPLETED) {
                        // Sometimes SSID might be "<unknown ssid>" if permissions are lacking or location is off
                        val rawSsid = wifiInfo.ssid ?: ""
                        val valid = rawSsid.isNotBlank() && rawSsid != "<unknown ssid>"
                        Log.d(
                            "mlog",
                            "SSID: $rawSsid  hidden: ${wifiInfo.hiddenSSID} (perm=$isLocationPermissionGranted, location=$isLocationEnabled)"
                        )
                        if (valid) rawSsid.replace("\"", "") else "WiFi"
                    } else if (!isLocationPermissionGranted) {
                        "Enable Location Permission"
                    } else if (!isLocationEnabled) {
                        "Enable Location Services"
                    } else {
                        "WiFi"
                    }
                return ssid
            } else {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return telephonyManager.networkOperatorName ?: "Mobile"
            }
        } catch (e: Exception) {
            Log.e("mlog", "getNetworkName: ${e.message}")
            return "Unknown"
        }
    }


    override fun getNetworkSpeed(): Flow<NetworkSpeed> = _networkSpeed

    override fun getNetworkInfo(): Flow<NetworkInfo> = _networkInfo
}