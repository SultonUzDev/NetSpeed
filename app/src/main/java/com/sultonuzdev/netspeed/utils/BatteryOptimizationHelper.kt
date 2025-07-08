package com.sultonuzdev.netspeed.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryOptimizationPermission(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        onResult: () -> Unit
    ) {
        if (!isIgnoringBatteryOptimizations(context)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
            }

            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                // If the specific intent fails, try the general settings
                val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                try {
                    launcher.launch(settingsIntent)
                } catch (e2: Exception) {
                    // If both fail, just proceed
                    onResult()
                }
            }
        } else {
            onResult()
        }
    }

    fun getRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getIgnoreBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}