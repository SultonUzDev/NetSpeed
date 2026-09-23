package com.sultonuzdev.netspeed.utils

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system's battery-optimisation list, from which the user can exempt this app.
     *
     * Deliberately not ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: that dialog requires the
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission, which Play restricts to a short list of
     * app types and will flag on review. The settings list needs no permission and reaches the
     * same switch. Nothing calls this on launch -- exemption is only worth asking about once
     * monitoring has been running.
     */
    fun openBatteryOptimizationSettings(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        onUnavailable: () -> Unit = {}
    ) {
        if (isIgnoringBatteryOptimizations(context)) return
        runCatching { launcher.launch(getIgnoreBatteryOptimizationSettingsIntent()) }
            .onFailure { onUnavailable() }
    }

    fun getIgnoreBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}