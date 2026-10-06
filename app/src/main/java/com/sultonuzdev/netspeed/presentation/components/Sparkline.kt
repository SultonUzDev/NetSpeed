package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme

/**
 * A compact trace of the most recent speed samples, oldest on the left.
 *
 * Scaled to the window's own peak, so the line always uses the full height: at these timescales
 * the useful question is "how is it moving right now", not "how does it compare to a fixed
 * ceiling" -- a throughput that never rises above a few percent of some absolute maximum would
 * otherwise render as a flat line along the bottom.
 */
@Composable
fun Sparkline(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (samples.size < 2) return@Box

        val peak = samples.max()
        // A silent window has no shape worth drawing; a flat baseline reads better than noise
        // amplified from nothing.
        val normalised = if (peak <= 0f) List(samples.size) { 0f } else samples.map { it / peak }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / (normalised.size - 1)
            val points = normalised.mapIndexed { index, value ->
                Offset(
                    x = index * stepX,
                    y = size.height - (value * size.height)
                )
            }

            val linePath = smoothPath(points, size.height)

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * A Catmull-Rom spline through every sample, as cubic Beziers.
 *
 * Straight segments made a one-second cadence look like a seismograph -- every sample a corner.
 * The curve passes through each point exactly (it interpolates, it does not approximate), so the
 * trace still reports the real figures; only the travel between them is eased.
 *
 * Tangents are slackened to half the classic Catmull-Rom length and the control points are
 * clamped to the band, because the textbook spline overshoots after a spike and would draw a
 * throughput below zero or above the window's own peak.
 */
private fun smoothPath(points: List<Offset>, height: Float): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (index in 0 until points.size - 1) {
        val previous = points[(index - 1).coerceAtLeast(0)]
        val start = points[index]
        val end = points[index + 1]
        val next = points[(index + 2).coerceAtMost(points.size - 1)]

        val firstControl = Offset(
            x = start.x + (end.x - previous.x) * SMOOTHING,
            y = (start.y + (end.y - previous.y) * SMOOTHING).coerceIn(0f, height)
        )
        val secondControl = Offset(
            x = end.x - (next.x - start.x) * SMOOTHING,
            y = (end.y - (next.y - start.y) * SMOOTHING).coerceIn(0f, height)
        )
        cubicTo(
            firstControl.x, firstControl.y,
            secondControl.x, secondControl.y,
            end.x, end.y
        )
    }
}

/** Half of Catmull-Rom's 1/6: enough to round the corners, not enough to invent a bounce. */
private const val SMOOTHING = 1f / 12f

@Preview
@Composable
private fun SparklinePreview() {
    NetSpeedTheme() {
        val list =listOf<Float>(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
        Sparkline(
            samples = list,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
