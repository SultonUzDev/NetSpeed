package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedTestUiState
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.presentation.theme.netSpeedColors

/**
 * The speed test, inline on the Speed screen.
 *
 * Idle, this is just a button -- a test is something you start occasionally, so its results should
 * not occupy the screen before one has been run. Everything else appears once it is running and
 * stays afterwards.
 */
@Composable
fun SpeedTestSection(
    modifier: Modifier = Modifier,
    speedTestUiState: SpeedTestUiState,
    onCancel:() -> Unit,
    onStart:() -> Unit,
) {

    val hasRun = speedTestUiState.phase != SpeedTestPhase.IDLE

    Column(modifier = modifier.fillMaxWidth()) {
        if (speedTestUiState.isRunning) {
            OutlinedButton(
                onClick =onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        } else {
            Button(
                onClick =onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasRun) "Test again" else "Run speed test")
            }
        }

        AnimatedVisibility(visible = hasRun) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                PhaseIndicator(phase = speedTestUiState.phase)
                Spacer(modifier = Modifier.height(10.dp))

                // Two cards side by side, two lines each, instead of a 2x2 grid of one-figure
                // cards: half the height, so the dial above keeps its size, and each value gets a
                // full half-width to sit in. Throughput pairs on the left, timing on the right.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResultPairCard(
                        first = "Download" to speedTestUiState.downloadResult,
                        second = "Upload" to speedTestUiState.uploadResult,
                        modifier = Modifier.weight(1f)
                    )
                    ResultPairCard(
                        first = "Ping" to speedTestUiState.pingResult,
                        second = "Jitter" to speedTestUiState.jitterResult,
                        modifier = Modifier.weight(1f)
                    )
                }

                speedTestUiState.error?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tested via Cloudflare · results stay on your device",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** A card holding two label/value lines, label at the start and value at the end of each. */
@Composable
private fun ResultPairCard(
    first: Pair<String, String>,
    second: Pair<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.netSpeedColors.cardBackground)
            .border(1.dp, MaterialTheme.netSpeedColors.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ResultLine(label = first.first, value = first.second)
        ResultLine(label = second.first, value = second.second)
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            // Tabular digits keep the two lines' figures aligned as values land one by one.
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * The three stages a test moves through, and which one is running. The live figure alone reports
 * a speed but not progress -- a slow download and an upload already under way look the same.
 */
@Composable
private fun PhaseIndicator(phase: SpeedTestPhase) {
    val stages = listOf(
        SpeedTestPhase.PINGING to "Latency",
        SpeedTestPhase.DOWNLOADING to "Download",
        SpeedTestPhase.UPLOADING to "Upload"
    )
    val currentIndex = stages.indexOfFirst { it.first == phase }
    val finished = phase == SpeedTestPhase.DONE

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, (_, label) ->
            val isDone = finished || (currentIndex >= 0 && index < currentIndex)
            val isActive = currentIndex == index

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive || isDone) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (index < stages.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(16.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }
    }
}


@Preview
@Composable
private fun SpeedTestSectionPreview() {
    NetSpeedTheme {
        SpeedTestSection(
            modifier = Modifier,
            speedTestUiState = SpeedTestUiState(
                phase = SpeedTestPhase.DONE,

                ),
            onCancel = {},
            onStart = {  }
        )
    }
}
