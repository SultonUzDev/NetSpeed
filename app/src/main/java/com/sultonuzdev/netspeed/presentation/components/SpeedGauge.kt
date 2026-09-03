package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.log10

/**
 * Dial for the live speed during a test.
 *
 * The sweep is logarithmic. Throughput spans several orders of magnitude across real connections,
 * so a linear dial would leave everything below a few tens of Mbps bunched at the start and
 * unreadable; a log sweep gives slow and fast connections comparable amounts of travel.
 */
@Composable
fun SpeedGauge(
    valueText: String,
    unitText: String,
    bytesPerSecond: Double,
    label: String,
    modifier: Modifier = Modifier,
    maxBytesPerSecond: Double = 125_000_000.0 // ~1 Gbps
) {
    val fraction = remember(bytesPerSecond, maxBytesPerSecond) {
        logFraction(bytesPerSecond, maxBytesPerSecond)
    }
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 350),
        label = "gauge_sweep"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sweepColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = sweepColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unitText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Maps a speed onto 0..1 logarithmically, with a floor so silence sits at zero. */
private fun logFraction(bytesPerSecond: Double, maxBytesPerSecond: Double): Float {
    if (bytesPerSecond <= FLOOR_BPS) return 0f
    if (bytesPerSecond >= maxBytesPerSecond) return 1f
    val span = log10(maxBytesPerSecond / FLOOR_BPS)
    if (span <= 0.0) return 0f
    return (log10(bytesPerSecond / FLOOR_BPS) / span).toFloat().coerceIn(0f, 1f)
}

private const val FLOOR_BPS = 1_000.0
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
