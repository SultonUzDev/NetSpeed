package com.sultonuzdev.netspeed.presentation.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.sultonuzdev.netspeed.domain.models.DailyUsageData
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.presentation.components.BottomNavigationHeight
import com.sultonuzdev.netspeed.presentation.components.DayUsageDetailDialog
import com.sultonuzdev.netspeed.presentation.theme.netSpeedColors
import com.sultonuzdev.netspeed.presentation.components.UsageBarChart
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * The day-by-day record: a week of columns, then every day of the window as a table, with the
 * billing-cycle total pinned at the bottom.
 *
 * Shares [UsageViewModel] with the Usage tab -- both read the same window, so loading it twice
 * would mean two sets of NetworkStats queries for identical numbers.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    uiState.selectedDay?.let { day ->
        DayUsageDetailDialog(detail = day, onDismiss = viewModel::clearSelectedDay)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            // The cycle total is pinned to the bottom and would sit under the floating bar.
            .padding(bottom = BottomNavigationHeight)
    ) {
        UsageBarChart(bars = uiState.dailyChart)

        // Labels the table beneath it. Floating at the top of the screen it described nothing in
        // particular, and sat oddly above a chart that covers a different range.
        Text(
            text = "Last 30 days",
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                TableHeaderCell("Date", weight = 2f)
                TableHeaderCell("Mobile", weight = 1.5f)
                TableHeaderCell("Wi-Fi", weight = 1.5f)
                TableHeaderCell("Total", weight = 1.5f)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.dailyUsageHistory.isEmpty()) {
            // Otherwise a fresh install shows a bare table header above two all-zero summary
            // rows, which reads as broken rather than as "nothing recorded yet".
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No usage recorded yet.\nHistory builds up as monitoring runs.",
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.dailyUsageHistory, key = { it.title }) { dailyData ->
                    UsageDataRow(
                        dailyData = dailyData,
                        onClick = { viewModel.selectDay(dailyData.dateKey) }
                    )
                }
                item { UsageDataRow(dailyData = uiState.last7DaysUsage, isSummary = true) }
                item { UsageDataRow(dailyData = uiState.last30DaysUsage, isSummary = true) }
            }
        }

        CycleTotalRow(cycleData = uiState.cycleTotals)
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
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun UsageDataRow(
    dailyData: DailyUsageData,
    modifier: Modifier = Modifier,
    /** Aggregate rows sit in the same list as daily ones and need to read as a different kind. */
    isSummary: Boolean = false,
    /** Null for aggregate rows, which have no single day to open. */
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && dailyData.dateKey.isNotEmpty()) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSummary) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 16dp all round made each of 30 rows ~52dp tall; a data table wants to be denser
                // than a card.
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A dot rather than a coloured row: the table is dense, and tinting whole rows
            // would fight the today/summary emphasis already in use.
            LimitDot(level = dailyData.limitLevel)

            Text(
                text = dailyData.title,
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (dailyData.isToday || isSummary) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
                color = if (dailyData.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Text(
                text = dailyData.mobileUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = dailyData.wifiUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = dailyData.totalUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Amber at the warning threshold, red past the day's share of the cap, absent otherwise. */
@Composable
private fun LimitDot(level: DataLimitLevel) {
    if (level == DataLimitLevel.NONE) {
        Spacer(modifier = Modifier.width(14.dp))
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (level == DataLimitLevel.REACHED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.netSpeedColors.warning
                    }
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
    }
}

@Composable
private fun CycleTotalRow(
    cycleData: DailyUsageData,
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cycleData.title,
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Start
            )
            Text(
                text = cycleData.mobileUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = cycleData.wifiUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = cycleData.totalUsage,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                // The other three cells already use onSecondaryContainer; `primary` here was
                // cyan-on-blue in dark mode, and is not a guaranteed pairing under Material You.
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
