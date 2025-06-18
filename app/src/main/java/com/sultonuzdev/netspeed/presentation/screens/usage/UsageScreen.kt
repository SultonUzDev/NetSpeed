package com.sultonuzdev.netspeed.presentation.screens.usage

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.domain.models.DailyUsageData
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.utils.NetworkUtils.formatBytes
import org.koin.androidx.compose.koinViewModel

@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Calculate monthly totals from daily data
    val monthlyTotals = remember(uiState.dailyUsageHistory) {
        calculateMonthlyTotals(uiState.dailyUsageHistory)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Internet Speed Meter Lite",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Table Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TableHeaderCell("Date", weight = 2f)
                TableHeaderCell("Mobile", weight = 1.5f)
                TableHeaderCell("WiFi", weight = 1.5f)
                TableHeaderCell("Total", weight = 1.5f)
            }
        }


        // Show loading or data
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Scrollable Data Rows
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f) // Takes remaining space
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.dailyUsageHistory) { dailyData ->
                    UsageDataRow(dailyData = dailyData)
                }
                item { UsageDataRow(dailyData = uiState.last7DaysUsage) }
                item { UsageDataRow(dailyData = uiState.last30DaysUsage) }
            }
        }

        // Fixed bottom section (always visible)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Monthly Total Row (always visible)
            MonthlyTotalRow(monthlyData = monthlyTotals)
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.weight(weight),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun UsageDataScreenPreview() {
    NetSpeedTheme(darkTheme = false) {
        UsageDataRow(
            dailyData = DailyUsageData(
                title = "Today",
                mobileUsage = "100 MB",
                wifiUsage = "50 MB",
                totalUsage = "150 MB",
                isToday = true
            )
        )
    }

}

@Composable
private fun UsageDataRow(
    dailyData: DailyUsageData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (dailyData.isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = dailyData.title,
                modifier = Modifier.weight(2f),
                fontSize = 13.sp,
                fontWeight = if (dailyData.isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (dailyData.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Start
            )

            // Mobile Usage
            Text(
                text = dailyData.mobileUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // WiFi Usage
            Text(
                text = dailyData.wifiUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Total Usage
            Text(
                text = dailyData.totalUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonthlyTotalRow(
    monthlyData: DailyUsageData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthlyData.title,
                modifier = Modifier.weight(2f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Start
            )

            Text(
                text = monthlyData.mobileUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = monthlyData.wifiUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = monthlyData.totalUsage,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper function to calculate monthly totals
private fun calculateMonthlyTotals(dailyData: List<DailyUsageData>): DailyUsageData {
    var totalMobile = 0L
    var totalWifi = 0L
    var totalUsage = 0L

    dailyData.forEach { day ->
        totalMobile += parseDataString(day.mobileUsage)
        totalWifi += parseDataString(day.wifiUsage)
        totalUsage += parseDataString(day.totalUsage)
    }

    return DailyUsageData(
        title = "This Month",
        mobileUsage = formatBytes(totalMobile),
        wifiUsage = formatBytes(totalWifi),
        totalUsage = formatBytes(totalUsage)
    )
}

// Helper function to parse data strings like "1.5 GB" back to bytes
private fun parseDataString(dataString: String): Long {
    val parts = dataString.trim().split(" ")
    if (parts.size != 2) return 0L

    val value = parts[0].toDoubleOrNull() ?: return 0L
    val unit = parts[1].uppercase()

    return when (unit) {
        "B" -> value.toLong()
        "KB" -> (value * 1024).toLong()
        "MB" -> (value * 1024 * 1024).toLong()
        "GB" -> (value * 1024 * 1024 * 1024).toLong()
        "TB" -> (value * 1024 * 1024 * 1024 * 1024).toLong()
        else -> 0L
    }
}


// Helper function to format bytes back to readable string
private fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()

    if (digitGroups >= units.size) return "0 B"

    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())

    return if (value >= 100) {
        "${value.toInt()} ${units[digitGroups]}"
    } else {
        String.format("%.1f %s", value, units[digitGroups])
    }
}

