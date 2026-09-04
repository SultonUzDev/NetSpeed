package com.sultonuzdev.netspeed.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Deep links into an OEM's own "autostart" allow-list.
 *
 * Several Chinese OEMs kill background services regardless of Android's own rules, and no manifest
 * flag or foreground service exempts an app -- the user has to allow it in the manufacturer's own
 * security app. Being stopped anyway is the single most common complaint against monitoring apps,
 * and the screen is undiscoverable unless you know it exists.
 *
 * Every candidate is resolved before it is offered, so a screen that has moved or been removed in
 * a newer build is never surfaced as a dead end.
 */
object AutoStartHelper {

    private data class Candidate(val packageName: String, val className: String)

    private val candidatesByManufacturer: Map<String, List<Candidate>> = mapOf(
        "xiaomi" to listOf(
            Candidate(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        "redmi" to listOf(
            Candidate(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        "poco" to listOf(
            Candidate(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        "oppo" to listOf(
            Candidate("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            Candidate("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            Candidate("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        ),
        "realme" to listOf(
            Candidate("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
        ),
        "vivo" to listOf(
            Candidate("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            Candidate("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        ),
        "huawei" to listOf(
            Candidate("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            Candidate("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        ),
        "honor" to listOf(
            Candidate("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        ),
        "letv" to listOf(
            Candidate("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
        ),
        "asus" to listOf(
            Candidate("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")
        )
    )

    /** Whether this device has a reachable autostart screen worth offering. */
    fun hasAutoStartSettings(context: Context): Boolean = resolveIntent(context) != null

    /** Intent for the OEM's autostart screen, or null when there is none on this device. */
    fun resolveIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidates = candidatesByManufacturer.entries
            .firstOrNull { manufacturer.contains(it.key) }
            ?.value
            ?: return null

        return candidates.asSequence()
            .map { Intent().setComponent(ComponentName(it.packageName, it.className)) }
            .firstOrNull { intent ->
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
            }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
