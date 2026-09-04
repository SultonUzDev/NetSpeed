package com.sultonuzdev.netspeed.data.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.data.overlay.SpeedOverlayManager
import com.sultonuzdev.netspeed.data.widget.SpeedWidgetProvider
import com.sultonuzdev.netspeed.data.widget.UsageWidgetProvider
import com.sultonuzdev.netspeed.domain.usecases.Alert
import com.sultonuzdev.netspeed.domain.usecases.CheckAlertsUseCase
import com.sultonuzdev.netspeed.domain.usecases.CheckDataLimitUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageForecastUseCase
import com.sultonuzdev.netspeed.domain.usecases.SaveUsageDataUseCase
import com.sultonuzdev.netspeed.presentation.MainActivity
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.ACTION_STOP_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.CHANNEL_ID
import com.sultonuzdev.netspeed.utils.Constants.DEFAULT_UPDATE_INTERVAL
import com.sultonuzdev.netspeed.utils.Constants.LEGACY_CHANNEL_ID
import com.sultonuzdev.netspeed.utils.Constants.NOTIFICATION_ID
import com.sultonuzdev.netspeed.utils.DataLimitNotifier
import com.sultonuzdev.netspeed.utils.FormattedSpeed
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.PingCalculator
import com.sultonuzdev.netspeed.utils.NotificationStyle
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
import com.sultonuzdev.netspeed.utils.SignalStrengthReader
import com.sultonuzdev.netspeed.utils.SpeedFormatter
import com.sultonuzdev.netspeed.utils.SpeedUnit
import com.sultonuzdev.netspeed.utils.UsagePeriods
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

    /**
     * Room writes live on their own scope so a final flush still lands after [serviceScope] is
     * torn down on stop.
     */
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var restartAttempts = 0
    private val maxRestartAttempts = 3

    // Inject PreferencesManager and SaveUsageDataUseCase
    private val preferencesManager: PreferencesManager by inject()
    private val saveUsageDataUseCase: SaveUsageDataUseCase by inject()
    private val checkDataLimitUseCase: CheckDataLimitUseCase by inject()
    private val checkAlertsUseCase: CheckAlertsUseCase by inject()
    private val getUsageForecastUseCase: GetUsageForecastUseCase by inject()
    private var updateFrequency = 1000L

    // Cache for preferences to avoid frequent reads
    private var notificationStyle = NotificationStyle.DETAILED
    private var speedUnit = SpeedUnit.AUTO
    private var displayMode = SpeedDisplayMode.DOWNLOAD

    // Which transports to count usage on, and whether to survive the app being swiped away.
    private var monitorWifi = true
    private var monitorMobile = true
    private var backgroundMonitoring = true

    // Floating overlay
    private var overlayEnabled = false
    private var overlayTextSize = 12
    private var overlayColor = PreferencesManager.OVERLAY_DEFAULT_COLOR
    private var overlayOpacity = 55
    private val overlayManager by lazy {
        SpeedOverlayManager(
            context = this,
            onPositionChanged = { x, y ->
                persistenceScope.launch {
                    try {
                        preferencesManager.updateOverlayPosition(x, y)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onTap = {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    /** Latency shown on the overlay; the notification has no room for it. */
    private var latencyMillis: Int? = null
    private var latencyCounter = 0
    private val latencyInterval = 10

    // Real network monitoring variables
    private var lastTotalRxBytes = 0L
    private var lastTotalTxBytes = 0L
    private var lastUpdateTime = 0L

    // Current speeds (bytes per second)
    private var currentDownloadSpeed = 0.0
    private var currentUploadSpeed = 0.0

    // Session totals, shown in the notification. These are "since monitoring started", not
    // "today" -- the day's real total lives in Room (and, with usage access, in NetworkStats).
    private var mobileDataUsed = 0L // bytes
    private var wifiDataUsed = 0L // bytes

    // Bytes measured but not yet flushed to Room, and the local day they belong to. Flushing
    // deltas keyed by day is what makes the stored total survive restarts and midnight.
    private var pendingWifiBytes = 0L
    private var pendingMobileBytes = 0L
    private var pendingDayKey = UsagePeriods.dayKey()
    private var lastFlushTime = System.currentTimeMillis()

    // Session tracking
    private var sessionStartTime = 0L
    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L

    // Network info. Null signal means the platform would not tell us, which is shown as "--"
    // rather than as a made-up percentage.
    private var signalStrength: Int? = null
    private var networkType = "Unknown"
    private var isWifiConnected = false

    /**
     * Sampling and notification updates are throttled while the screen is off. Byte accounting is
     * unaffected: TrafficStats counters are cumulative, so a slower sample rate still yields exact
     * deltas -- only the live speed reading loses resolution, and nobody is looking at it.
     */
    @Volatile
    private var isScreenOn = true

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> isScreenOn = true
                Intent.ACTION_SCREEN_OFF -> isScreenOn = false
            }
        }
    }

    // Data save counter
    private var saveCounter = 0
    private val saveInterval = 10 // Save every 10 updates (about 10 seconds)

    // Cap checks are cheaper than they look but still hit DataStore and NetworkStats, so they run
    // on a slower cadence than the flush.
    private var alertCounter = 0
    private val alertInterval = 30

    // Roaming, per-app limits and background usage need per-uid queries, so they run far less
    // often than the cap check -- once every few minutes is ample for all three.
    private var extraAlertCounter = 0
    private val extraAlertInterval = 300

    private var widgetCounter = 0
    private val widgetInterval = 5

    private var lastNotificationSignature: String? = null

    private companion object {
        /** Sampling cadence while the screen is off. */
        const val SCREEN_OFF_INTERVAL = 15_000L

        /** Usage is shown to one decimal of a MB, so finer changes need no repost. */
        const val MB_IN_BYTES = 1024L * 1024
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadPreferences()
        registerScreenStateReceiver()
        initializeMonitoring()

    }

    private fun loadPreferences() {
        serviceScope.launch {
            try {
                // Load preferences and cache them
                notificationStyle = preferencesManager.notificationStyle.first()
                speedUnit = preferencesManager.speedUnit.first()
                displayMode = preferencesManager.speedDisplayMode.first()
                monitorWifi = preferencesManager.monitorWifi.first()
                monitorMobile = preferencesManager.monitorMobile.first()
                backgroundMonitoring = preferencesManager.backgroundMonitoring.first()
                overlayEnabled = preferencesManager.overlayEnabled.first()
                overlayTextSize = preferencesManager.overlayTextSize.first()
                overlayColor = preferencesManager.overlayColor.first()
                overlayOpacity = preferencesManager.overlayOpacity.first()
                syncOverlay()
                val frequencySeconds = preferencesManager.updateFrequency.first()
                updateFrequency = (frequencySeconds * 1000L)
            } catch (e: Exception) {
                // Use defaults if preferences can't be loaded
                notificationStyle = NotificationStyle.DETAILED
                speedUnit = SpeedUnit.AUTO
                displayMode = SpeedDisplayMode.DOWNLOAD
                monitorWifi = true
                monitorMobile = true
                backgroundMonitoring = true
                updateFrequency = DEFAULT_UPDATE_INTERVAL
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                restartAttempts = 0 // Reset restart attempts on new start
                setMonitoringEnabled(true)
                startMonitoring()
            }

            ACTION_STOP_MONITORING -> {
                // An explicit stop is a decision to remember: it is what stops us restarting
                // after the next reboot.
                setMonitoringEnabled(false)
                stopMonitoring()
            }

            // START_STICKY revives the service with a null intent. Previously nothing matched, so
            // the service came back, never called startForeground, and monitored nothing until
            // the user reopened the app.
            else -> startMonitoring()
        }
        return START_STICKY // Ensure service restarts if killed
    }

    /** Records whether monitoring should come back on its own; read by the boot receiver. */
    private fun setMonitoringEnabled(enabled: Boolean) {
        persistenceScope.launch {
            try {
                preferencesManager.updateMonitoringEnabled(enabled)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        // The wake lock belongs to the monitoring loop, not to the service object: acquiring it
        // in onCreate held the CPU awake even when nothing was being monitored.
        acquireWakeLock()
        syncOverlay()
        // Anchor the flush window here, not at onCreate: otherwise the first row credits idle
        // time between service creation and the user actually starting monitoring.
        lastFlushTime = System.currentTimeMillis()
        pendingDayKey = UsagePeriods.dayKey()
        startForeground(NOTIFICATION_ID, createSpeedNotification())

        // Start monitoring loop
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    updateNetworkSpeed()
                    updateNetworkInfo()

                    // Roll over first: bytes measured before midnight belong to the old day.
                    val today = UsagePeriods.dayKey()
                    if (today != pendingDayKey) {
                        flushUsageDelta()
                        pendingDayKey = today
                        saveCounter = 0
                    }

                    saveCounter++
                    if (saveCounter >= saveInterval) {
                        flushUsageDelta()
                        saveCounter = 0
                    }

                    alertCounter++
                    if (alertCounter >= alertInterval) {
                        checkDataLimit()
                        alertCounter = 0
                    }

                    extraAlertCounter++
                    if (extraAlertCounter >= extraAlertInterval) {
                        checkExtraAlerts()
                        updateUsageWidget()
                        extraAlertCounter = 0
                    }

                    // Only measured while the overlay is up: it is the only surface that shows
                    // it, and a network round trip every few seconds is not free.
                    if (overlayEnabled && isScreenOn) {
                        latencyCounter++
                        if (latencyCounter >= latencyInterval) {
                            measureLatency()
                            latencyCounter = 0
                        }
                    }

                    // Reload preferences periodically to pick up changes
                    if (System.currentTimeMillis() % 10000 < updateFrequency) {
                        loadPreferences()
                    }

                    // Redrawing a notification nobody can see is pure battery cost. The same
                    // goes for the overlay, which is drawn on the same screen.
                    if (isScreenOn) {
                        updateNotification()
                        updateOverlay()

                        // RemoteViews updates cross a binder and redraw the launcher, so the
                        // widget runs at a slower cadence than the notification.
                        widgetCounter++
                        if (widgetCounter >= widgetInterval) {
                            updateWidget()
                            widgetCounter = 0
                        }
                    }
                    delay(if (isScreenOn) updateFrequency else SCREEN_OFF_INTERVAL)
                } catch (e: Exception) {
                    if (restartAttempts < maxRestartAttempts) {
                        restartAttempts++
                        stopMonitoring()
                        startMonitoring()
                    } else {
                        e.printStackTrace()
                        stopMonitoring()
                    }
                }
            }
        }
    }

    /**
     * Writes the bytes measured since the last flush onto [pendingDayKey]'s row and clears the
     * pending counters.
     *
     * Deltas, not absolutes: the previous version wrote its own in-memory running totals over the
     * day's row, so every service restart reset the stored day to near zero, and a second writer
     * in the UI layer raced it. Accumulating means neither can lose data the other recorded.
     */
    private fun flushUsageDelta() {
        val wifiDelta = pendingWifiBytes
        val mobileDelta = pendingMobileBytes
        val now = System.currentTimeMillis()
        val sessionDelta = ((now - lastFlushTime) / 1000).coerceAtLeast(0L)
        val dayKey = pendingDayKey

        if (wifiDelta <= 0L && mobileDelta <= 0L && sessionDelta <= 0L) return

        pendingWifiBytes = 0L
        pendingMobileBytes = 0L
        lastFlushTime = now

        persistenceScope.launch {
            try {
                saveUsageDataUseCase.addDelta(dayKey, wifiDelta, mobileDelta, sessionDelta)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Notifies if mobile usage has newly crossed the warning threshold or the cap. The use case
     * decides whether anything is actually due, so this can run on a timer without spamming.
     */
    private fun checkDataLimit() {
        persistenceScope.launch {
            try {
                val alert = checkDataLimitUseCase.checkForAlert() ?: return@launch
                DataLimitNotifier.notify(this@SpeedMonitorService, alert)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Brings the overlay into line with the current preference. Must run on the main thread —
     * WindowManager rejects view operations from anywhere else.
     */
    private fun syncOverlay() {
        serviceScope.launch {
            if (overlayEnabled && isMonitoring) {
                if (overlayManager.isShowing) {
                    overlayManager.restyle(overlayTextSize, overlayColor, overlayOpacity)
                } else {
                    val x = preferencesManager.overlayX.first()
                    val y = preferencesManager.overlayY.first()
                    overlayManager.show(x, y, overlayTextSize, overlayColor, overlayOpacity)
                }
            } else if (overlayManager.isShowing) {
                overlayManager.hide()
            }
        }
    }

    private fun updateWidget() {
        SpeedWidgetProvider.updateAll(
            this,
            SpeedFormatter.format(currentDownloadSpeed, speedUnit),
            SpeedFormatter.format(currentUploadSpeed, speedUnit),
            NetworkUtils.formatBytes(wifiDataUsed + mobileDataUsed)
        )
    }

    private fun measureLatency() {
        persistenceScope.launch {
            latencyMillis = try {
                PingCalculator.tcpLatencyMillis()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun updateOverlay() {
        if (!overlayManager.isShowing) return
        overlayManager.update(
            download = SpeedFormatter.format(currentDownloadSpeed, speedUnit),
            upload = SpeedFormatter.format(currentUploadSpeed, speedUnit),
            mode = displayMode,
            detailLine = overlayDetailLine()
        )
    }

    /**
     * The overlay's third line. Deliberately carries what the notification does not: latency,
     * and how much this session has moved.
     */
    private fun overlayDetailLine(): String {
        val sessionBytes = wifiDataUsed + mobileDataUsed
        val parts = buildList {
            latencyMillis?.let { add("${it} ms") }
            add(networkType)
            if (sessionBytes > 0L) add(NetworkUtils.formatBytes(sessionBytes))
        }
        return parts.joinToString(" · ")
    }

    /**
     * Cycle usage for the home-screen widget. Slow cadence deliberately: the figure moves in
     * megabytes over hours, and it costs a NetworkStats query plus a RemoteViews round trip.
     */
    private fun updateUsageWidget() {
        persistenceScope.launch {
            try {
                UsageWidgetProvider.updateAll(
                    this@SpeedMonitorService,
                    getUsageForecastUseCase()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Roaming, per-app allowances and background usage. The use case decides what is due. */
    private fun checkExtraAlerts() {
        persistenceScope.launch {
            try {
                val alerts = checkAlertsUseCase.check()

                alerts.filterIsInstance<Alert.Roaming>().firstOrNull()?.let {
                    DataLimitNotifier.notifyRoaming(
                        this@SpeedMonitorService,
                        it.mobileUsedThisCycle
                    )
                }

                // Batched, because per-app alerts share a notification id.
                DataLimitNotifier.notifyBackgroundData(
                    this@SpeedMonitorService,
                    alerts.filterIsInstance<Alert.BackgroundData>()
                        .map { it.appLabel to it.bytes }
                )

                DataLimitNotifier.notifyAppLimit(
                    this@SpeedMonitorService,
                    alerts.filterIsInstance<Alert.AppLimit>().map {
                        DataLimitNotifier.AppLimitBreach(it.appLabel, it.used, it.limit)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        overlayManager.hide()
        flushUsageDelta()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        releaseWakeLock()
    }

    private fun updateNetworkSpeed() {
        val currentTime = System.currentTimeMillis()
        val currentRxBytes = getTotalRxBytes()
        val currentTxBytes = getTotalTxBytes()

        if (lastUpdateTime > 0) {
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // seconds

            if (timeDiff > 0) {
                // Calculate speed in bytes per second
                // TrafficStats counts from boot, so a reboot mid-session makes the raw diff
                // negative. Treat that as "no traffic" rather than as a huge negative sample.
                val rxDiff = (currentRxBytes - lastTotalRxBytes).coerceAtLeast(0L)
                val txDiff = (currentTxBytes - lastTotalTxBytes).coerceAtLeast(0L)

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
        if (totalBytes <= 0L) return

        // The Monitor Wi-Fi / Monitor mobile data switches decide whether traffic on that
        // transport is counted at all. Live speed is unaffected -- the switches are about usage
        // accounting, which is what their descriptions promise.
        if (isWifiConnected) {
            if (!monitorWifi) return
            wifiDataUsed += totalBytes
            pendingWifiBytes += totalBytes
        } else {
            if (!monitorMobile) return
            mobileDataUsed += totalBytes
            pendingMobileBytes += totalBytes
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
                signalStrength = SignalStrengthReader.percent(this, isWifi = true)
            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                networkType = "Mobile"
                signalStrength = SignalStrengthReader.percent(this, isWifi = false)
            } else {
                networkType = "Unknown"
                signalStrength = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isWifiConnected = false
            networkType = "Unknown"
            signalStrength = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (!isMonitoring) return

        // Re-posting identical content still rebuilds the icon bitmap and crosses a binder every
        // second. While the connection is idle the text does not change at all, so skip it.
        val signature = notificationSignature()
        if (signature == lastNotificationSignature) return

        try {
            val notification = createSpeedNotification()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            lastNotificationSignature = signature
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Everything the notification actually renders; equal signatures mean an identical post. */
    private fun notificationSignature(): String {
        val download = SpeedFormatter.format(currentDownloadSpeed, speedUnit)
        val upload = SpeedFormatter.format(currentUploadSpeed, speedUnit)
        return "$download|$upload|$networkType|$signalStrength|$displayMode|$notificationStyle|" +
                "${mobileDataUsed / MB_IN_BYTES}|${wifiDataUsed / MB_IN_BYTES}"
    }

    private fun createSpeedNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val download = SpeedFormatter.format(currentDownloadSpeed, speedUnit)
        val upload = SpeedFormatter.format(currentUploadSpeed, speedUnit)
        val combined = SpeedFormatter.format(currentDownloadSpeed + currentUploadSpeed, speedUnit)

        // The icon is ~96px wide, so it gets the number on one line and either the unit or the
        // other direction's number on the second -- never both a unit and two speeds.
        val speedIcon = when (displayMode) {
            SpeedDisplayMode.BOTH -> createTextBasedIcon(
                "\u2193${download.value}",
                "\u2191${upload.value}"
            )

            SpeedDisplayMode.UPLOAD -> createTextBasedIcon(upload.value, upload.unit)
            SpeedDisplayMode.COMBINED -> createTextBasedIcon(combined.value, combined.unit)
            SpeedDisplayMode.DOWNLOAD -> createTextBasedIcon(download.value, download.unit)
        }

        val title = when (displayMode) {
            SpeedDisplayMode.DOWNLOAD -> "\u2193 $download"
            SpeedDisplayMode.UPLOAD -> "\u2191 $upload"
            SpeedDisplayMode.COMBINED -> "$combined total"
            SpeedDisplayMode.BOTH -> "\u2193 $download    \u2191 $upload"
        }

        // Anything the title already says would only be repeated here, so the content line carries
        // whatever the chosen mode leaves out.
        val content = when (displayMode) {
            SpeedDisplayMode.DOWNLOAD -> "\u2191$upload | $networkType"
            SpeedDisplayMode.UPLOAD -> "\u2193$download | $networkType"
            SpeedDisplayMode.COMBINED, SpeedDisplayMode.BOTH -> networkType
        }

        return if (notificationStyle == NotificationStyle.COMPACT) {
            buildNotification(pendingIntent, speedIcon, title, content, bigText = null)
        } else {
            buildNotification(
                pendingIntent,
                speedIcon,
                title,
                "$content | Signal: $signalText",
                bigText = detailedBigText(download, upload, combined)
            )
        }
    }

    @SuppressLint("DefaultLocale")
    private fun detailedBigText(
        download: FormattedSpeed,
        upload: FormattedSpeed,
        combined: FormattedSpeed
    ): String {
        val mobileDataMB = (mobileDataUsed / (1024.0 * 1024.0))
        val wifiDataMB = (wifiDataUsed / (1024.0 * 1024.0))

        return buildString {
            when (displayMode) {
                SpeedDisplayMode.COMBINED -> append("Total: $combined\n")
                SpeedDisplayMode.UPLOAD -> append("Upload: $upload\n")
                SpeedDisplayMode.DOWNLOAD -> append("Download: $download\n")
                SpeedDisplayMode.BOTH -> {
                    append("Download: $download\n")
                    append("Upload: $upload\n")
                }
            }
            append("Signal: $signalText ($networkType)\n")
            append("Mobile Data: ${String.format("%.1f", mobileDataMB)} MB\n")
            append("WiFi Data: ${String.format("%.1f", wifiDataMB)} MB")
        }
    }

    /** Signal as text, or an em dash when the platform declined to report it. */
    private val signalText: String
        get() = signalStrength?.let { "$it%" } ?: "\u2014"

    private fun buildNotification(
        pendingIntent: PendingIntent,
        speedIcon: Bitmap,
        title: String,
        content: String,
        bigText: String?
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(IconCompat.createWithBitmap(speedIcon))
            .setContentTitle(title)
            .setContentText(content)
            .apply {
                if (bigText != null) {
                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(bigText)
                            .setBigContentTitle(title)
                            .setSummaryText("Net Speed Monitor")
                    )
                }
            }
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            // Without this, every one-second update is treated as a fresh alert and can pop the
            // notification back up as a heads-up.
            .setOnlyAlertOnce(true)
            // A live readout should sit quietly in the shade, not announce itself.
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF2196F3.toInt())
            .build()
    }


    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // The original channel was created at IMPORTANCE_HIGH, which made each update eligible
        // for a heads-up popup. Importance cannot be lowered on an existing channel, so the old
        // one is removed and replaced.
        try {
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Net Speed Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows real-time internet speed and data usage"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true) // Allow notifications even in Do Not Disturb mode
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // A rotation swaps the display bounds, which can leave the overlay's saved coordinates
        // outside the new ones.
        if (overlayManager.isShowing) overlayManager.ensureOnScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        overlayManager.hide()
        flushUsageDelta()
        serviceScope.cancel()
        releaseWakeLock()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Never registered, or already gone.
        }
    }

    /**
     * Renders the speed as a status-bar icon, since Android gives no way to put live text there.
     *
     * Takes the two lines already formatted rather than a single string to pull apart: the caller
     * knows whether the second line is a unit or the other direction's speed, and re-parsing a
     * formatted string to find out was fragile.
     */
    private fun createTextBasedIcon(primaryText: String, secondaryText: String): Bitmap {
        val statusBarTextSize = getStatusBarTextSize()

        val width = 96
        val height = getStatusBarHeight(this)

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val centerX = width / 2f

        // No shadow layer: the status bar tints a small icon from its alpha channel, so a
        // drop shadow only smears the silhouette it derives. A clean white-on-transparent
        // glyph is also what lets the system invert it on a light status bar.
        val primaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textSize = statusBarTextSize * 0.9f
        }

        val secondaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = statusBarTextSize * 0.6f
        }

        // Two speeds need equal weight; a speed plus its unit does not.
        if (displayMode == SpeedDisplayMode.BOTH) {
            secondaryPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            secondaryPaint.textSize = statusBarTextSize * 0.75f
            primaryPaint.textSize = statusBarTextSize * 0.75f
        }

        // The bitmap is a fixed 96px wide, but the text is not: "1023" or a two-arrow line at
        // a large status-bar text size overruns it and gets cut off. Scale both lines by the
        // same factor so the size hierarchy survives.
        shrinkToFit(primaryPaint, primaryText, secondaryPaint, secondaryText, width * 0.94f)

        val primaryBounds = android.graphics.Rect()
        primaryPaint.getTextBounds(primaryText, 0, primaryText.length, primaryBounds)

        val secondaryBounds = android.graphics.Rect()
        secondaryPaint.getTextBounds(secondaryText, 0, secondaryText.length, secondaryBounds)

        val totalTextHeight = primaryBounds.height() + secondaryBounds.height() + 2
        val startY = (height - totalTextHeight) / 2f + primaryBounds.height()

        canvas.drawText(primaryText, centerX, startY, primaryPaint)
        canvas.drawText(secondaryText, centerX, startY + secondaryBounds.height() + 4, secondaryPaint)

        return bitmap
    }


    /**
     * Scales a pair of paints down together until the wider of the two lines fits [maxWidth].
     * Never scales up: the configured sizes are the intended maximum.
     */
    private fun shrinkToFit(
        primaryPaint: Paint,
        primaryText: String,
        secondaryPaint: Paint,
        secondaryText: String,
        maxWidth: Float
    ) {
        val widest = maxOf(
            primaryPaint.measureText(primaryText),
            secondaryPaint.measureText(secondaryText)
        )
        if (widest <= maxWidth || widest <= 0f) return

        val scale = maxWidth / widest
        primaryPaint.textSize *= scale
        secondaryPaint.textSize *= scale
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


    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpeedMonitorService:WakeLock")
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // "Keep monitoring in background" off means the service goes when the app does.
        if (!backgroundMonitoring) {
            setMonitoringEnabled(false)
            stopMonitoring()
            super.onTaskRemoved(rootIntent)
            return
        }

        // Only revive a service the user still wants running.
        if (isMonitoring) {
            val restartService = Intent(this, SpeedMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            // startService() throws IllegalStateException from the background on Android 8+;
            // we are still a foreground service here, so starting one is permitted.
            ContextCompat.startForegroundService(this, restartService)
        }
        super.onTaskRemoved(rootIntent)
    }
}