package com.sultonuzdev.netspeed.presentation.screens.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.domain.models.NetworkDetails
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.NetworkDetailsSheet
import com.sultonuzdev.netspeed.presentation.components.Sparkline
import com.sultonuzdev.netspeed.presentation.components.SpeedTestSection
import com.sultonuzdev.netspeed.presentation.components.SpeedCircle
import com.sultonuzdev.netspeed.presentation.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.presentation.screens.speedtest.SpeedTestViewModel
import com.sultonuzdev.netspeed.utils.NetworkDetailsReader
import org.koin.androidx.compose.koinViewModel


@Composable
fun SpeedScreen(
    modifier: Modifier = Modifier,
    viewModel: SpeedViewModel = koinViewModel(),
    // Same instance the inline section below resolves, so the dial and the results agree.
    speedTestViewModel: SpeedTestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val testState by speedTestViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Read on open rather than polled: these values barely change, and a dialog that is not
    // showing should not be querying system services every second.
    var networkDetails by remember { mutableStateOf<NetworkDetails?>(null) }

    networkDetails?.let { details ->
        NetworkDetailsSheet(details = details, onDismiss = { networkDetails = null })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            // The bar floats over the content instead of occupying a Scaffold slot, so the
            // screen has to leave its height free itself.
            .padding(bottom = BottomNavigationHeight + 16.dp)
    ) {
        // Live throughput is already the notification's whole job, so it does not need the
        // largest element on the screen -- a compact strip with its trace is enough.
        LiveSpeedCard(uiState = uiState)

        // The dial is the speed test's: idle it invites one, running it tracks it, and afterwards
        // it holds the result.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            SpeedCircle(
                speed = if (testState.phase == SpeedTestPhase.IDLE) "--" else testState.liveValue,
                unit = if (testState.phase == SpeedTestPhase.IDLE) "" else testState.liveUnit,
                type = if (testState.phase == SpeedTestPhase.IDLE) {
                    "Speed test"
                } else {
                    testState.phaseLabel
                }
            )
        }

        // The test lives here rather than on a screen of its own: it is an action taken from
        // the Speed screen, and its results belong beside the live figures they contextualise.
        SpeedTestSection(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            viewModel = speedTestViewModel
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
                .clickable { networkDetails = NetworkDetailsReader.read(context) }
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
                            if (index < uiState.signalStrength) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
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
                    text = "DOWNLOAD",
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
                    text = "UPLOAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.uploadSpeed} ${uiState.uploadUnit}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Warning,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Sparkline(samples = uiState.recentDownload, height = 36.dp)
    }
}
