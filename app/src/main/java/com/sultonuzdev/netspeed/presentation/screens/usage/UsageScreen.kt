package com.sultonuzdev.netspeed.presentation.screens.usage

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.domain.models.UsageForecast
import com.sultonuzdev.netspeed.presentation.components.AppUsageDetailDialog
import com.sultonuzdev.netspeed.presentation.components.AppUsageListItem
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.DataLimitCard
import com.sultonuzdev.netspeed.presentation.components.StatCard
import com.sultonuzdev.netspeed.presentation.components.UsageAccessCard
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.utils.UsageAccessHelper
import org.koin.androidx.compose.koinViewModel

/**
 * Where the data is going right now: today's totals, standing against the cap, and which apps are
 * responsible. The day-by-day record lives on the History tab.
 */
@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Usage access is granted in system settings, with no callback back to us, so re-check on
    // every resume rather than only at construction.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    UsageScreenContent(
        uiState = uiState,
        onPeriodSelected = viewModel::selectAppUsagePeriod,
        onAppClick = viewModel::selectApp,
        onDismissApp = viewModel::clearSelectedApp,
        onSetAppLimit = viewModel::setAppLimit,
        onGrantClick = { openUsageAccessSettings(context) },
        modifier = modifier
    )
}

@Composable
private fun UsageScreenContent(
    uiState: UsageUiState,
    onPeriodSelected: (AppUsagePeriod) -> Unit,
    onAppClick: (Int) -> Unit,
    onDismissApp: () -> Unit,
    onSetAppLimit: (uid: Int, bytes: Long) -> Unit,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    uiState.selectedApp?.let { detail ->
        AppUsageDetailDialog(
            detail = detail,
            onDismiss = onDismissApp,
            limitBytes = uiState.appLimits[detail.uid],
            onSetLimit = { bytes -> onSetAppLimit(detail.uid, bytes) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            // Leaves room for the floating bar, which overlays rather than displaces content.
            .padding(bottom = BottomNavigationHeight)
    ) {
        uiState.dataLimitStatus
            ?.takeIf { it.limitBytes > 0L }
            ?.let { DataLimitCard(status = it, forecast = uiState.forecast) }

        TodayTotals(uiState = uiState)

        Spacer(modifier = Modifier.height(8.dp))

        AppUsageSection(
            uiState = uiState,
            modifier = Modifier.weight(1f),
            onPeriodSelected = onPeriodSelected,
            onAppClick = onAppClick,
            onGrantClick = onGrantClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageScreenContentPreview() {
    val gb = 1024L * 1024 * 1024
    val mb = 1024L * 1024
    NetSpeedTheme(darkTheme = true) {
        UsageScreenContent(
            uiState = UsageUiState(
                todayMobile = "412 MB",
                todayWifi = "2.1 GB",
                todayTotal = "2.5 GB",
                hasUsageAccess = true,
                isUsageAccurate = true,
                dataLimitStatus = DataLimitStatus(
                    usedBytes = 9 * gb,
                    limitBytes = 25 * gb,
                    level = DataLimitLevel.NONE,
                    cycleKey = "2026-09",
                    warningThresholdPercent = 80
                ),
                forecast = UsageForecast(
                    usedBytes = 9 * gb,
                    limitBytes = 25 * gb,
                    projectedBytes = 21 * gb,
                    perDayBytes = 700 * mb,
                    daysRemaining = 17,
                    daysUntilLimit = null
                ),
                appUsage = listOf(
                    AppUsageRow(
                        uid = 10001, packageName = "com.example.video", appLabel = "Video",
                        mobileUsage = "310 MB", wifiUsage = "1.4 GB", totalUsage = "1.7 GB",
                        totalBytes = 1700 * mb, shareOfMax = 1f
                    ),
                    AppUsageRow(
                        uid = 10002, packageName = "com.example.chat", appLabel = "Chat",
                        mobileUsage = "80 MB", wifiUsage = "420 MB", totalUsage = "500 MB",
                        totalBytes = 500 * mb, shareOfMax = 0.3f
                    ),
                    AppUsageRow(
                        uid = 10003, packageName = "com.example.browser", appLabel = "Browser",
                        mobileUsage = "22 MB", wifiUsage = "260 MB", totalUsage = "282 MB",
                        totalBytes = 282 * mb, shareOfMax = 0.17f
                    )
                )
            ),
            onPeriodSelected = {},
            onAppClick = {},
            onDismissApp = {},
            onSetAppLimit = { _, _ -> },
            onGrantClick = {}
        )
    }
}

@Composable
private fun TodayTotals(uiState: UsageUiState) {
    Column {
        // "Today" said once as a heading, rather than repeated inside all three card labels
        // where it crowded out the part that actually differs. The accuracy note sits opposite
        // it: it qualifies these figures, and floating alone at the top of the screen it read as
        // a caption belonging to nothing.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (uiState.isUsageAccurate) "Exact" else "Estimated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Mobile",
                value = uiState.todayMobile,
                modifier = Modifier.weight(1f),
                compact = true
            )
            StatCard(
                label = "Wi-Fi",
                value = uiState.todayWifi,
                modifier = Modifier.weight(1f),
                compact = true
            )
            StatCard(
                label = "Total",
                value = uiState.todayTotal,
                modifier = Modifier.weight(1f),
                compact = true
            )
        }
    }
}

@Composable
private fun AppUsageSection(
    uiState: UsageUiState,
    onPeriodSelected: (AppUsagePeriod) -> Unit,
    onAppClick: (Int) -> Unit,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!uiState.hasUsageAccess) {
            UsageAccessCard(onGrantClick = onGrantClick)
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "By app",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppUsagePeriod.entries.forEach { period ->
                FilterChip(
                    selected = uiState.appUsagePeriod == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(period.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        when {
            uiState.isLoadingApps -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            uiState.appUsage.isEmpty() -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No app traffic recorded for this period yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.appUsage, key = { it.uid }) { row ->
                    AppUsageListItem(row = row, onClick = { onAppClick(row.uid) })
                }
            }
        }
    }
}

private fun openUsageAccessSettings(context: Context) {
    val launch = { intent: Intent ->
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
    // Some OEM builds reject the per-package extra; fall back to the plain list.
    if (!launch(UsageAccessHelper.usageAccessSettingsIntent(context)) &&
        !launch(UsageAccessHelper.usageAccessSettingsFallbackIntent())
    ) {
        Toast.makeText(
            context,
            "Could not open usage access settings on this device",
            Toast.LENGTH_LONG
        ).show()
    }
}
