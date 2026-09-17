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

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

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
