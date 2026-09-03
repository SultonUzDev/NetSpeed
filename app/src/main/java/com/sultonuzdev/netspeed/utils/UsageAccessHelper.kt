package com.sultonuzdev.netspeed.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Usage-access ([android.Manifest.permission.PACKAGE_USAGE_STATS]) is a special permission: it
 * cannot be requested at runtime, the user has to grant it from system settings. It is what
 * unlocks [android.app.usage.NetworkStatsManager], and therefore per-app usage and carrier-accurate
 * daily totals.
 */
object UsageAccessHelper {

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Settings screen where the user grants usage access. Some OEM builds do not honour the
     * per-package extra, so the caller lands on the app list instead of our own row.
     */
    fun usageAccessSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    fun usageAccessSettingsFallbackIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
