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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.presentation.components.AppUsageDetailDialog
import com.sultonuzdev.netspeed.presentation.components.AppUsageListItem
import com.sultonuzdev.netspeed.presentation.components.DataLimitCard
import com.sultonuzdev.netspeed.presentation.components.StatCard
import com.sultonuzdev.netspeed.presentation.components.UsageAccessCard
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

    uiState.selectedApp?.let { detail ->
        AppUsageDetailDialog(detail = detail, onDismiss = viewModel::clearSelectedApp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UsageHeader(isAccurate = uiState.isUsageAccurate)

        uiState.dataLimitStatus
            ?.takeIf { it.limitBytes > 0L }
            ?.let { DataLimitCard(status = it) }

        TodayTotals(uiState = uiState)

        Spacer(modifier = Modifier.height(8.dp))

        AppUsageSection(
            uiState = uiState,
            modifier = Modifier.weight(1f),
            onPeriodSelected = viewModel::selectAppUsagePeriod,
            onAppClick = viewModel::selectApp,
            onGrantClick = { openUsageAccessSettings(context) }
        )
    }
}

@Composable
private fun UsageHeader(isAccurate: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The bottom bar already names this destination; only the accuracy note earns space.
            Text(
                text = if (isAccurate) {
                    "Matching Android's own measurements"
                } else {
                    "Estimated — turn on usage access for exact figures"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TodayTotals(uiState: UsageUiState) {
    Column {
        // "Today" said once as a heading, rather than repeated inside all three card labels
        // where it crowded out the part that actually differs.
        Text(
            text = "Today",
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
