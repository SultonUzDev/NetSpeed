package com.sultonuzdev.netspeed.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Brings monitoring back after the device restarts or the app is updated.
 *
 * Two conditions have to hold: the user has not turned the preference off, and monitoring was
 * actually running beforehand. Restarting unconditionally would resurrect a service the user had
 * deliberately stopped.
 *
 * `BOOT_COMPLETED` is one of the exemptions that still permits starting a foreground service from
 * the background on Android 12+, so this is allowed to call [ContextCompat.startForegroundService].
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val preferencesManager: PreferencesManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        // Reading DataStore is suspending, and onReceive must not block; goAsync keeps the
        // receiver alive for the short window we need.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val shouldRestart = preferencesManager.startOnBoot.first() &&
                        preferencesManager.monitoringEnabled.first()
                if (shouldRestart) {
                    val serviceIntent = Intent(context, SpeedMonitorService::class.java).apply {
                        action = ACTION_START_MONITORING
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            // Fired instead of BOOT_COMPLETED by some OEM quick-boot implementations.
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            // An app update stops our service; bring it back without the user reopening the app.
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
