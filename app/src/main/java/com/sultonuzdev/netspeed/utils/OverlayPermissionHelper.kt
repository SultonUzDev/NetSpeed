package com.sultonuzdev.netspeed.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * "Draw over other apps" is a special permission: it cannot be requested at runtime, only granted
 * by the user in system settings, and it can be revoked at any time — so it is checked before
 * every attempt to show the overlay, never cached.
 */
object OverlayPermissionHelper {

    fun canDrawOverlays(context: Context): Boolean =
        try {
            Settings.canDrawOverlays(context)
        } catch (e: Exception) {
            false
        }

    fun settingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )

    fun settingsFallbackIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
}
