package com.sultonuzdev.netspeed.presentation.screens.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.presentation.components.SpeedCircle
import com.sultonuzdev.netspeed.presentation.components.StatCard
import com.sultonuzdev.netspeed.presentation.theme.*
import org.koin.androidx.compose.koinViewModel


@Composable
fun SpeedScreen(
    modifier: Modifier = Modifier,
    viewModel: SpeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Fixed: Add proper bottom padding to avoid navigation overlap
            .padding(bottom = 120.dp) // Increased from 100dp to 120dp
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.1f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
                .padding(top = 60.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Net Speed",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Text(
                text = "Real-time Internet Monitor",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Light
            )
        }

        // Speed Display - FIXED: Proper centering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp),
            contentAlignment = Alignment.Center // This ensures proper centering
        ) {
            SpeedCircle(
                speed = uiState.downloadSpeed,
                unit = uiState.downloadUnit,
                type = "Download"
            )
        }

        // Stats Grid
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
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

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                StatCard(
                    label = "Peak Download",
                    value = uiState.peakDownload,
                    modifier = Modifier.weight(1f)
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
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f))
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📶",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${uiState.networkName} Connected",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((6 + index * 4).dp)
                        .background(
                            if (index < uiState.signalStrength) Primary else Primary.copy(alpha = 0.3f),
                            RoundedCornerShape(1.dp)
                        )
                )
                if (index < 3) Spacer(modifier = Modifier.width(2.dp))
            }
        }

        // Today's Usage Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Error.copy(alpha = 0.1f))
                .border(1.dp, Error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "DATA USAGE TODAY",
                    fontSize = 16.sp,
                    color = Error,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Usage Items
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    UsageItem(
                        label = "WiFi",
                        amount = uiState.todayWifiUsage,
                        progress = uiState.wifiProgress
                    )
                    UsageItem(
                        label = "Mobile",
                        amount = uiState.todayMobileUsage,
                        progress = uiState.mobileProgress
                    )
                    UsageItem(
                        label = "Total",
                        amount = uiState.todayTotalUsage,
                        progress = uiState.totalProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageItem(
    label: String,
    amount: String,
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = amount,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Error
            )
        }

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Error.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Error, Error.copy(alpha = 0.8f))
                        )
                    )
            )
        }
    }
}

