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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.presentation.theme.netSpeedColors
import com.sultonuzdev.netspeed.utils.NetworkUtils

/**
 * Mobile usage against the user's cap for the current billing cycle. Mirrors exactly what the
 * alert notification measures, so the bar and the warning can never disagree.
 */
@Composable
fun DataLimitCard(
    status: DataLimitStatus,
    modifier: Modifier = Modifier
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
