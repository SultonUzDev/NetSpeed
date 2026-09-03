package com.sultonuzdev.netspeed.presentation.screens.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sultonuzdev.netspeed.presentation.components.NetworkDetailsSheet
import com.sultonuzdev.netspeed.presentation.components.Sparkline
import com.sultonuzdev.netspeed.presentation.components.SpeedCircle
import com.sultonuzdev.netspeed.presentation.components.StatCard
import com.sultonuzdev.netspeed.presentation.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.sultonuzdev.netspeed.utils.NetworkDetailsReader
import org.koin.androidx.compose.koinViewModel


@Composable
fun SpeedScreen(
    onRunSpeedTest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            // Breathing room only. The host Scaffold already reserves the bottom bar's height
            // (nav-bar inset included) via its content padding, so the 120dp that used to be
            // here was a second reservation of space nothing occupies.
            .padding(bottom = 24.dp)
    ) {
        // Speed Display - FIXED: Proper centering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center // This ensures proper centering
        ) {
            SpeedCircle(
                speed = uiState.heroSpeed,
                unit = uiState.heroUnit,
                type = uiState.heroLabel,
                secondary = uiState.heroSecondary
            )
        }

        // Live trace of the last minute, so a momentary number gains some context.
        Sparkline(
            samples = uiState.recentDownload,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        OutlinedButton(
            onClick = onRunSpeedTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Run speed test")
        }

        // Stats Grid
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "Ping",
                    value = uiState.ping,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Upload",
                    value = "${uiState.uploadSpeed} ${uiState.uploadUnit}",
                    valueColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "Peak Download",
                    value = uiState.peakDownload,
                    modifier = Modifier.weight(1f),
                    // resetPeakValues() existed on the view model with nothing able to call it;
                    // a peak you cannot clear stops being useful after one spike.
                    onClick = viewModel::resetPeakValues
                )
                StatCard(
                    label = "Session Time",
                    value = uiState.sessionTime,
                    modifier = Modifier.weight(1f)
                )
            }
        }

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
                text = if (uiState.isConnected) {
                    "${uiState.networkName} · ${connectionLabel(uiState.networkType)}"
                } else {
                    "No connection"
                },
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

/** Human-readable transport name; [uiState.networkType] carries the raw enum name. */
private fun connectionLabel(networkType: String): String = when (networkType) {
    "WIFI" -> "Wi-Fi"
    "MOBILE" -> "Mobile data"
    else -> "Offline"
}
