package com.sultonuzdev.netspeed.presentation.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * The one line of explanation that goes in front of a system permission dialog.
 *
 * The system prompt says what is being asked for but never why, and a prompt with no reason is
 * the one people decline. This says why in the app's own words, and only appears at the moment
 * the feature is used -- never on launch, and never stacked with another.
 */
@Composable
fun PermissionRationale(
    title: String,
    body: String,
    confirmLabel: String = "Continue",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

/** True when [permission] has already been granted, so nothing needs to be asked. */
fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/**
 * True once the system will no longer show a prompt for [permission] -- the user has declined it
 * twice, or declined with "don't ask again". Asking again from here does nothing at all, so the
 * only way forward is the app's own settings page.
 */
fun isPermanentlyDenied(context: Context, permission: String): Boolean {
    val activity = context as? Activity ?: return false
    return !hasPermission(context, permission) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/** Opens this app's page in system settings, for a permission the prompt can no longer offer. */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Opens the system's notification settings for this app, where the toggle can be turned back on. */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(intent) }
    } else {
        openAppSettings(context)
    }
}
