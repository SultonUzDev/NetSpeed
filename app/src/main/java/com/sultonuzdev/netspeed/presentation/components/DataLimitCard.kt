package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.domain.models.UsageForecast
import com.sultonuzdev.netspeed.presentation.theme.netSpeedColors
import com.sultonuzdev.netspeed.utils.NetworkUtils

/**
 * Mobile usage against the user's cap for the current billing cycle. Mirrors exactly what the
 * alert notification measures, so the bar and the warning can never disagree.
 */
@Composable
fun DataLimitCard(
    status: DataLimitStatus,
    modifier: Modifier = Modifier,
    /** Projection for the rest of the cycle; omitted when there is not enough of one yet. */
    forecast: UsageForecast? = null
) {
    val barColor: Color = when (status.level) {
        DataLimitLevel.REACHED -> MaterialTheme.colorScheme.error
        DataLimitLevel.WARNING -> MaterialTheme.netSpeedColors.warning
        DataLimitLevel.NONE -> MaterialTheme.colorScheme.primary
    }

    val animatedFraction by animateFloatAsState(
        targetValue = status.fraction,
        animationSpec = tween(durationMillis = 600),
        label = "data_limit_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mobile data this cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${status.percentUsed}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            forecast?.let { ForecastLine(it) }

            Text(
                text = when (status.level) {
                    DataLimitLevel.REACHED ->
                        "${NetworkUtils.formatBytes(status.usedBytes)} of " +
                                "${NetworkUtils.formatBytes(status.limitBytes)} — limit reached"

                    else ->
                        "${NetworkUtils.formatBytes(status.usedBytes)} of " +
                                "${NetworkUtils.formatBytes(status.limitBytes)} · " +
                                "${NetworkUtils.formatBytes(status.remainingBytes)} left"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // One line: the forecast is a glanceable summary, not a paragraph.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The projection, in the terms people actually think in: a total, and how far off the cap it is.
 *
 * Phrased as "at this rate" because that is exactly what the arithmetic supports -- a linear
 * extrapolation of the cycle so far, not a prediction.
 */
@Composable
private fun ForecastLine(forecast: UsageForecast) {
    // Kept to one line. The earlier phrasing ran to ~80 characters and wrapped to three lines
    // inside a status card, which buries the number it exists to deliver.
    val projected = NetworkUtils.formatBytes(forecast.projectedBytes)
    val text = when {
        forecast.willExceed && forecast.daysUntilLimit != null -> {
            val days = forecast.daysUntilLimit
            val whenText = if (days <= 1) "tomorrow" else "in ${days}d"
            "Heading for $projected · over $whenText"
        }

        forecast.willExceed ->
            "Heading for $projected · ${NetworkUtils.formatBytes(forecast.overageBytes)} over"

        else -> "Heading for $projected · within limit"
    }

    Column {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (forecast.willExceed) {
                MaterialTheme.netSpeedColors.warning
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
