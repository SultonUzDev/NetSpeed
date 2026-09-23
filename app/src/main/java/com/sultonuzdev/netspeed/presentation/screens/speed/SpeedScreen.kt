package com.sultonuzdev.netspeed.presentation.screens.speed

import android.annotation.SuppressLint
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sultonuzdev.netspeed.presentation.components.PermissionRationale
import com.sultonuzdev.netspeed.presentation.components.hasPermission
import com.sultonuzdev.netspeed.presentation.components.isPermanentlyDenied
import com.sultonuzdev.netspeed.presentation.components.openNotificationSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.NetworkDetailsSheet
import com.sultonuzdev.netspeed.presentation.components.Sparkline
import com.sultonuzdev.netspeed.presentation.components.SpeedCircle
import com.sultonuzdev.netspeed.presentation.components.SpeedTestSection
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedTestUiState
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedUiState
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.presentation.theme.netSpeedColors
import org.koin.androidx.compose.koinViewModel


@Composable
fun SpeedScreen(
    viewModel: SpeedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speedTestUiState by viewModel.speedTestState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Nothing is asked for on launch. The two permissions this screen can use are offered here,
    // each next to the thing it unlocks: the status-bar figure at the top, and the mobile signal
    // reading behind the network row.
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    val promptDismissed by viewModel.notificationPromptDismissed.collectAsStateWithLifecycle()
    var askNotifications by remember { mutableStateOf(false) }
    var askPhoneState by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (!granted) {
            viewModel.dismissNotificationPrompt()
            // Android shows its dialog at most twice. Once it has stopped appearing, asking
            // again does nothing at all, so that -- and only that -- is when the app hands over
            // to the settings page. Checked after the prompt, never before it: beforehand the
            // same signal cannot tell "never asked" from "asked and refused for good".
            if (isPermanentlyDenied(context, Manifest.permission.POST_NOTIFICATIONS)) {
                openNotificationSettings(context)
            }
        }
    }

    // Whatever the answer, the sheet opens: the reader reports "no reading" for signal strength
    // rather than failing, so a decline costs one field and nothing else.
    val phoneStateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.readNetworkDetails() }

    if (askNotifications) {
        PermissionRationale(
            title = "Show the speed in your status bar",
            body = "NetSpeed posts one ongoing notification carrying the live figure, so you " +
                    "can see your speed without opening the app. Android needs your permission " +
                    "to show it.",
            onConfirm = {
                askNotifications = false
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                askNotifications = false
                viewModel.dismissNotificationPrompt()
            }
        )
    }

    if (askPhoneState) {
        PermissionRationale(
            title = "Read your mobile signal",
            body = "Signal strength for a mobile connection comes from the phone's radio, " +
                    "which Android keeps behind a permission. The rest of the network details " +
                    "are shown either way.",
            onConfirm = {
                askPhoneState = false
                phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            },
            onDismiss = {
                askPhoneState = false
                viewModel.readNetworkDetails()
            }
        )
    }

    SpeedScreenContent(
        uiState = uiState,
        speedTestUiState = speedTestUiState,
        onStart = { viewModel.startTest() },
        onCancel = { viewModel.cancelTest() },
        onReadNetworkDetails = {
            // Only mobile connections have a signal reading to unlock, so Wi-Fi never sees
            // this prompt at all.
            if (uiState.networkType == "MOBILE" &&
                !hasPermission(context, Manifest.permission.READ_PHONE_STATE)
            ) {
                askPhoneState = true
            } else {
                viewModel.readNetworkDetails()
            }
        },
        onDismissNetworkDetails = { viewModel.clearNetworkDetails() },
        showNotificationPrompt = !notificationsGranted && !promptDismissed,
        onEnableNotifications = { askNotifications = true },
        onDismissNotificationPrompt = { viewModel.dismissNotificationPrompt() }
    )
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SpeedScreenContent(
    uiState: SpeedUiState,
    speedTestUiState: SpeedTestUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onReadNetworkDetails: () -> Unit,
    onDismissNetworkDetails: () -> Unit,
    showNotificationPrompt: Boolean = false,
    onEnableNotifications: () -> Unit = {},
    onDismissNotificationPrompt: () -> Unit = {},
) {
    uiState.networkDetails?.let { details ->
        NetworkDetailsSheet(details = details, onDismiss = onDismissNetworkDetails)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            // The bar floats over the content instead of occupying a Scaffold slot, so the
            // screen has to leave its height free itself.
            .padding(bottom = BottomNavigationHeight + 16.dp)
    )
    {
        // Sits above the live figures because that is exactly what it offers to put in the
        // status bar. It appears only while the permission is missing, and goes for good once
        // dismissed or granted.
        if (showNotificationPrompt) {
            NotificationPrompt(
                onEnable = onEnableNotifications,
                onDismiss = onDismissNotificationPrompt
            )
        }

        // Live throughput is already the notification's whole job, so it does not need the
        // largest element on the screen -- a compact strip with its trace is enough.
        LiveSpeedCard(uiState = uiState)

        // The dial is the speed test's: idle it invites one, running it tracks it, and afterwards
        // it holds the result. It takes whatever height the fixed rows leave, so the screen has
        // no dead band under the network row; on a short screen it shrinks instead of scrolling.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // ponytail: below ~150dp the labels crowd; a scroll fallback if that ever shows up.
            val dial = dialContent(speedTestUiState)
            // 280dp fills a phone; on a tablet the same dial left a third of the page empty
            // around it. Only a screen with the height to spare takes the larger cap, so phone
            // layouts are untouched.
            val cap = if (maxHeight >= 600.dp) 400.dp else 280.dp
            SpeedCircle(
                diameter = minOf(maxHeight, maxWidth, cap).coerceAtLeast(150.dp),
                speed = dial.value,
                unit = dial.unit,
                animating = speedTestUiState.isRunning
            )
        }

        // The test lives here rather than on a screen of its own: it is an action taken from
        // the Speed screen, and its results belong beside the live figures they contextualise.
        SpeedTestSection(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            speedTestUiState = speedTestUiState,
            onCancel = onCancel,
            onStart = onStart
        )

        // Network Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onReadNetworkDetails() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A Material icon rather than the 📶 emoji: an emoji renders from the system
            // emoji font at a fixed colour, ignores the theme, and always showed signal bars
            // regardless of whether the connection was Wi-Fi or mobile.
            Icon(
                imageVector = when (uiState.networkType) {
                    "WIFI" -> Icons.Default.Wifi
                    "MOBILE" -> Icons.Default.SignalCellularAlt
                    else -> Icons.Default.SignalWifiOff
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                // Without location permission the SSID is redacted and the name falls back to
                // the transport, so appending the transport again read "Wi-Fi · Wi-Fi".
                text = networkLabel(uiState.networkName, uiState.networkType, uiState.isConnected),
                // Weighted and clipped: an unbounded name pushed the signal bars and the chevron
                // off the end of the row.
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(10.dp))
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((6 + index * 4).dp)
                        .background(
                            if (index < uiState.signalStrength) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            ),
                            RoundedCornerShape(1.dp)
                        )
                )
                if (index < 3) Spacer(modifier = Modifier.width(2.dp))
            }
            // Signals that the row opens something, rather than leaving the tap undiscoverable.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Network details",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeedScreenContentPreview() {
    NetSpeedTheme(darkTheme = true) {
        SpeedScreenContent(
            uiState = SpeedUiState(
                downloadSpeed = "9.9",
                downloadUnit = "KB/s",
                uploadSpeed = "2.9",
                uploadUnit = "KB/s",
                isConnected = true,
                networkType = "MOBILE",
                networkName = "Mobiuz",
                signalStrength = 3,
                recentDownload = listOf(2f, 8f, 3f, 12f, 6f, 9f, 4f, 15f, 7f, 5f)
            ),
            speedTestUiState = SpeedTestUiState(
                phase = SpeedTestPhase.DONE,
                liveValue = "93.6",
                liveUnit = "Mbps",
                downloadResult = "93.6 Mbps",
                uploadResult = "12.4 Mbps",
                pingResult = "365 ms",
                jitterResult = "192 ms"
            ),
            onStart = {},
            onCancel = {},
            onReadNetworkDetails = {},
            onDismissNetworkDetails = {}
        )
    }
}

/**
 * The offer to put the live speed in the status bar.
 *
 * A card the user can act on or dismiss, rather than a dialog on launch: by the time it is read
 * the speed is already moving in the strip below it, so the offer describes something the user
 * has just seen work.
 */
@Composable
private fun NotificationPrompt(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.netSpeedColors.cardBackground)
            .border(1.dp, MaterialTheme.netSpeedColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Keep this speed in your status bar",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "See it without opening the app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onEnable) { Text("Turn on") }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** What the dial shows for one phase: the figure, its unit, and the caption naming it. */
private data class DialContent(val value: String, val unit: String, val caption: String)

/**
 * The dial's figure and caption follow the phase rather than [SpeedTestUiState.liveValue] alone.
 * Live throughput is only meaningful during download and upload; during the ping phase it used to
 * fall through as "0 Mbps" under a "Latency" caption -- a speed that had not been measured, in
 * the wrong unit. Done, the ring holds the download result, so the caption says so -- and only
 * that: a carrier name ("Download · Mobiuz · Mobile") overran the ring, and the network row
 * below already names it.
 */
private fun dialContent(state: SpeedTestUiState): DialContent = when (state.phase) {
    SpeedTestPhase.IDLE -> DialContent("—", "", "Speed test")
    SpeedTestPhase.PINGING -> DialContent("—", "ms", "Latency")
    SpeedTestPhase.DOWNLOADING -> DialContent(state.liveValue, state.liveUnit, "Download")
    SpeedTestPhase.UPLOADING -> DialContent(state.liveValue, state.liveUnit, "Upload")
    SpeedTestPhase.DONE -> DialContent(state.liveValue, state.liveUnit, "Download")
    SpeedTestPhase.FAILED -> DialContent("—", "", "Test failed")
}

/** Name plus transport, collapsed to one when the name is only the transport. */
private fun networkLabel(name: String, networkType: String, isConnected: Boolean): String {
    if (!isConnected) return "No connection"
    val transport = connectionLabel(networkType)
    return if (name.equals(transport, ignoreCase = true) || name.isBlank()) {
        transport
    } else {
        "$name · $transport"
    }
}

/** Human-readable transport name; [uiState.networkType] carries the raw enum name. */
private fun connectionLabel(networkType: String): String = when (networkType) {
    "WIFI" -> "Wi-Fi"
    "MOBILE" -> "Mobile data"
    else -> "Offline"
}

/**
 * Live download and upload in a strip, with the last minute traced underneath.
 *
 * This used to be the big dial. It moved here because the same figure is already permanently in
 * the notification and status bar -- repeating it as the largest thing on screen bought nothing,
 * and the dial is more useful showing a speed test that has no other home.
 */
@Composable
private fun LiveSpeedCard(uiState: SpeedUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.netSpeedColors.cardBackground)
            .border(1.dp, MaterialTheme.netSpeedColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.downloadSpeed} ${uiState.downloadUnit}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.uploadSpeed} ${uiState.uploadUnit}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Sparkline(samples = uiState.recentDownload, height = 36.dp)
    }
}
