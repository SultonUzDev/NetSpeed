package com.sultonuzdev.netspeed.data.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.presentation.MainActivity
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.ACTION_STOP_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.CHANNEL_ID
import com.sultonuzdev.netspeed.utils.Constants.DEFAULT_UPDATE_INTERVAL
import com.sultonuzdev.netspeed.utils.Constants.NOTIFICATION_ID
import com.sultonuzdev.netspeed.utils.NetworkUtils.formatSpeedImproved
import com.sultonuzdev.netspeed.utils.NotificationStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SpeedMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isMonitoring = false

    // Inject PreferencesManager
    private val preferencesManager: PreferencesManager by inject()
    private var updateFrequency = 1000L


    // Cache for preferences to avoid frequent reads
    private var notificationStyle = NotificationStyle.DETAILED

    // Real network monitoring variables
    private var lastTotalRxBytes = 0L
    private var lastTotalTxBytes = 0L
    private var lastUpdateTime = 0L

    // Current speeds (bytes per second)
    private var currentDownloadSpeed = 0.0
    private var currentUploadSpeed = 0.0

    // Data usage tracking
    private var mobileDataUsed = 0L // bytes
    private var wifiDataUsed = 0L // bytes

    // Session tracking
    private var sessionStartTime = 0L
    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L

    // Network info
    private var signalStrength = 0
    private var networkType = "Unknown"
    private var isWifiConnected = false


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadPreferences()
        initializeMonitoring()

    }

    private fun loadPreferences() {
        serviceScope.launch {
            try {
                // Load preferences and cache them
                notificationStyle = preferencesManager.notificationStyle.first()
                val frequencySeconds = preferencesManager.updateFrequency.first()
                updateFrequency = (frequencySeconds * 1000L)
            } catch (e: Exception) {
                // Use defaults if preferences can't be loaded
                notificationStyle = NotificationStyle.DETAILED
                updateFrequency = DEFAULT_UPDATE_INTERVAL
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun initializeMonitoring() {
        sessionStartTime = System.currentTimeMillis()

        // Initialize baseline values
        lastTotalRxBytes = getTotalRxBytes()
        lastTotalTxBytes = getTotalTxBytes()
        sessionStartRxBytes = lastTotalRxBytes
        sessionStartTxBytes = lastTotalTxBytes
        lastUpdateTime = System.currentTimeMillis()
    }

    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        startForeground(NOTIFICATION_ID, createSpeedNotification())

        // Start monitoring loop
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    updateNetworkSpeed()
                    updateNetworkInfo()

                    // Reload preferences periodically to pick up changes
                    if (System.currentTimeMillis() % 10000 < updateFrequency) {
                        loadPreferences()
                    }

                    updateNotification()
                    delay(updateFrequency)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNetworkSpeed() {
        val currentTime = System.currentTimeMillis()
        val currentRxBytes = getTotalRxBytes()
        val currentTxBytes = getTotalTxBytes()

        if (lastUpdateTime > 0) {
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // seconds

            if (timeDiff > 0) {
                // Calculate speed in bytes per second
                val rxDiff = currentRxBytes - lastTotalRxBytes
                val txDiff = currentTxBytes - lastTotalTxBytes

                currentDownloadSpeed = rxDiff / timeDiff
                currentUploadSpeed = txDiff / timeDiff

                // Update data usage based on current network type
                updateDataUsage(rxDiff, txDiff)
            }
        }

        // Update baseline for next calculation
        lastTotalRxBytes = currentRxBytes
        lastTotalTxBytes = currentTxBytes
        lastUpdateTime = currentTime
    }

    private fun getTotalRxBytes(): Long {
        return try {
            android.net.TrafficStats.getTotalRxBytes()
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTotalTxBytes(): Long {
        return try {
            android.net.TrafficStats.getTotalTxBytes()
        } catch (e: Exception) {
            0L
        }
    }

    private fun updateDataUsage(rxBytes: Long, txBytes: Long) {
        val totalBytes = rxBytes + txBytes

        if (isWifiConnected) {
            wifiDataUsed += totalBytes
        } else {
            mobileDataUsed += totalBytes
        }
    }

    private fun updateNetworkInfo() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (isWifiConnected) {
                networkType = "WiFi"
                signalStrength = getWiFiSignalStrength()
            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                networkType = "Mobile"
                signalStrength = getMobileSignalStrength()
            } else {
                networkType = "Unknown"
                signalStrength = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isWifiConnected = false
            networkType = "Unknown"
            signalStrength = 0
        }
    }

    private fun getWiFiSignalStrength(): Int {
        return try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val rssi = wifiInfo.rssi

            // Convert RSSI to percentage (typical WiFi range: -100 to -30 dBm)
            when {
                rssi >= -50 -> 100
                rssi >= -60 -> 75
                rssi >= -70 -> 50
                rssi >= -80 -> 25
                else -> 10
            }
        } catch (e: Exception) {
            50 // Default value
        }
    }

    private fun getMobileSignalStrength(): Int {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                75
            } else {
                75
            }
        } catch (e: Exception) {
            50 // Default value
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (!isMonitoring) return

        try {
            val notification = createSpeedNotification()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun createSpeedNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format current speed
        val speedText = formatSpeedImproved(currentDownloadSpeed)

        // Create dynamic speed icon for status bar that actually shows as text
        val speedIcon = createTextBasedIcon(speedText)


        // Build notification based on style preference
        return if (notificationStyle == NotificationStyle.COMPACT) {
            createCompactNotification(
                pendingIntent,
                speedIcon,
                speedText,
                formatSpeedImproved(currentUploadSpeed)
            )
        } else {
            createDetailedNotification(
                pendingIntent,
                speedIcon,
                speedText,
                formatSpeedImproved(currentUploadSpeed)
            )
        }
    }

    @SuppressLint("DefaultLocale")
    private fun createDetailedNotification(
        pendingIntent: PendingIntent,
        speedIcon: Bitmap,
        speedText: String,
        uploadText: String
    ): Notification {
        // Format data usage
        val mobileDataMB = (mobileDataUsed / (1024.0 * 1024.0))
        val wifiDataMB = (wifiDataUsed / (1024.0 * 1024.0))

        val title = "Net Speed: $speedText"
        val content = "↑$uploadText | Signal: $signalStrength% | $networkType"
        val bigText = buildString {
            append("Download: $speedText\n")
            append("Upload: $uploadText\n")
            append("Signal: $signalStrength% ($networkType)\n")
            append("Mobile Data: ${String.format("%.1f", mobileDataMB)} MB\n")
            append("WiFi Data: ${String.format("%.1f", wifiDataMB)} MB")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(IconCompat.createWithBitmap(speedIcon))
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle(title)
                    .setSummaryText("Net Speed Monitor")
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF2196F3.toInt())
            .build()
    }


    private fun createCompactNotification(
        pendingIntent: PendingIntent,
        speedIcon: Bitmap,
        speedText: String,
        uploadText: String
    ): Notification {
        val title = "Net Speed: $speedText"
        val content = "↑$uploadText | $networkType"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(IconCompat.createWithBitmap(speedIcon))
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF2196F3.toInt())
            .build()
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Net Speed Monitor",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows real-time internet speed and data usage"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceScope.cancel()
    }

    private fun createTextBasedIcon(speedText: String): Bitmap {
        // Get status bar text size dynamically
        val statusBarTextSize = getStatusBarTextSize()

        // Optimized dimensions for status bar compatibility
        val width = 96  // Reduced width for better status bar fit
        val height = getStatusBarHeight(this) // Reduced height to match status bar

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clear background (completely transparent)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Parse speed text into number and unit
        val (speedNumber, speedUnit) = parseSpeedForTwoLines(speedText)
        val centerX = width / 2f

        // Paint for the main speed number (matches status bar clock size)
        val numberPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD) // System UI font
            textSize = statusBarTextSize * 0.9f // Slightly smaller than clock
            setShadowLayer(2f, 0.5f, 0.5f, Color.parseColor("#80000000")) // Subtle shadow
        }

        // Paint for the unit (smaller, like status bar indicators)
        val unitPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL) // Regular weight
            textSize = statusBarTextSize * 0.6f // Much smaller for unit
            setShadowLayer(1f, 0.5f, 0.5f, Color.parseColor("#80000000"))
        }

        // Calculate text metrics for perfect positioning
        val numberBounds = android.graphics.Rect()
        numberPaint.getTextBounds(speedNumber, 0, speedNumber.length, numberBounds)

        val unitBounds = android.graphics.Rect()
        unitPaint.getTextBounds(speedUnit, 0, speedUnit.length, unitBounds)

        // Position text to center vertically in the available space
        val totalTextHeight = numberBounds.height() + unitBounds.height() + 2 // 2px spacing
        val startY = (height - totalTextHeight) / 2f + numberBounds.height()

        // Draw the speed number (main text)
        canvas.drawText(speedNumber, centerX, startY, numberPaint)

        // Draw the unit (smaller text below)
        canvas.drawText(speedUnit, centerX, startY + unitBounds.height() + 4, unitPaint)

        return bitmap
    }


    private fun getStatusBarTextSize(): Float {
        return try {
            // Method 1: Try to get from system resources
            val context = this // Your context
            val resourceId = context.resources.getIdentifier(
                "status_bar_clock_size", "dimen", "android"
            )

            if (resourceId != 0) {
                context.resources.getDimension(resourceId)
            } else {
                // Method 2: Calculate based on status bar height
                getStatusBarClockSizeFromHeight(context)
            }
        } catch (e: Exception) {
            // Fallback to reasonable default (14sp converted to px)
            14 * this.resources.displayMetrics.scaledDensity
        }
    }

    private fun getStatusBarClockSizeFromHeight(context: Context): Float {
        val statusBarHeight = getStatusBarHeight(context)

        // Status bar clock is typically 70-80% of status bar height
        return when {
            statusBarHeight <= 0 -> 14 * context.resources.displayMetrics.scaledDensity // Fallback
            statusBarHeight < 60 -> statusBarHeight * 0.6f  // Compact
            statusBarHeight < 80 -> statusBarHeight * 0.65f // Normal
            else -> statusBarHeight * 0.7f // Large
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            // Fallback calculation based on density
            (24 * context.resources.displayMetrics.density).toInt()
        }
    }


    /**
     * Parses speed text into number and unit for two-line display
     * Returns (number, unit) pair
     * Examples: "5.2 MB/s" -> ("5.2", "MB/s"), "125 KB/s" -> ("125", "KB/s")
     */
    private fun parseSpeedForTwoLines(speedText: String): Pair<String, String> {
        return try {
            val clean = speedText.trim()

            // Try to match number and unit pattern
            val regex = Regex("([0-9.]+)\\s*([A-Za-z/]+)")
            val matchResult = regex.find(clean)

            if (matchResult != null) {
                val number = matchResult.groupValues[1]
                val unit = matchResult.groupValues[2].uppercase()

                // Format the number (remove unnecessary decimals)
                val formattedNumber = formatNumber(number.toDoubleOrNull() ?: 0.0)

                return Pair(formattedNumber, unit)
            } else {
                // Fallback: try to split by space
                val parts = clean.split(" ")
                if (parts.size >= 2) {
                    val number = formatNumber(extractNumber(parts[0]))
                    val unit = parts[1].uppercase()
                    return Pair(number, unit)
                } else {
                    // If can't parse, return as single number
                    return Pair(clean.take(4), "")
                }
            }
        } catch (e: Exception) {
            Pair("0", "KB/s")
        }
    }

    /**
     * Extracts numeric value from speed string
     */
    private fun extractNumber(text: String): Double {
        return try {
            val numberString = text.replace(Regex("[^0-9.]"), "")
            numberString.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Formats number for display (removes unnecessary decimals)
     */
    @SuppressLint("DefaultLocale")
    private fun formatNumber(value: Double): String {
        return when {
            value >= 100 -> value.toInt().toString()
            value >= 10 -> String.format("%.1f", value)
            value >= 1 -> String.format("%.1f", value)
            else -> String.format("%.2f", value)
        }.let { result ->
            // Remove trailing zeros and decimal point if not needed
            if (result.contains(".")) {
                result.trimEnd('0').trimEnd('.')
            } else {
                result
            }
        }
    }
}