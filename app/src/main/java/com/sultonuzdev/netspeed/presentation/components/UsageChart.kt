package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import com.sultonuzdev.netspeed.presentation.theme.Primary
import com.sultonuzdev.netspeed.presentation.theme.Secondary
import com.sultonuzdev.netspeed.presentation.theme.PrimaryVariant
import com.sultonuzdev.netspeed.presentation.theme.TextSecondary

@Preview
@Composable
private fun UsageChartPreview() {
    NetSpeedTheme {
        UsageChart(
            data = listOf(0.6f, 0.8f, 0.45f, 0.9f, 0.7f, 0.85f, 0.95f),
            modifier = Modifier
        )
    }
}

@Composable
fun UsageChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    var animatedValues by remember { mutableStateOf(List(7) { 0f }) }

    // Default data for demo if no data provided
    val chartData = if (data.isEmpty()) {
        listOf(0.6f, 0.8f, 0.45f, 0.9f, 0.7f, 0.85f, 0.95f)
    } else {
        data.take(7).let { list ->
            if (list.size < 7) {
                list + List(7 - list.size) { 0.3f }
            } else list
        }
    }

    LaunchedEffect(chartData) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        ) { progress, _ ->
            animatedValues = chartData.map { it * progress }
        }
    }

    Column(modifier = modifier) {
        // Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0A000000))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val barWidth = size.width / (animatedValues.size + 1)
                val maxHeight = size.height * 0.8f

                animatedValues.forEachIndexed { index, value ->
                    val barHeight = maxHeight * value.coerceIn(0f, 1f)
                    val x = barWidth * (index + 0.5f) - (barWidth * 0.15f)
                    val y = size.height - barHeight

                    // Draw bar with gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimaryVariant,
                                Secondary
                            ),
                            startY = y,
                            endY = size.height
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth * 0.3f, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Days labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Alternative implementation with more detailed chart
@Composable
fun DetailedUsageChart(
    data: List<Float>,
    labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    maxValue: Float = 5f, // 5GB max
    modifier: Modifier = Modifier
) {
    var animatedValues by remember { mutableStateOf(List(7) { 0f }) }

    // Default data for demo
    val chartData = if (data.isEmpty()) {
        listOf(2.4f, 3.1f, 1.8f, 4.2f, 2.9f, 3.8f, 4.5f)
    } else {
        data.take(7).let { list ->
            if (list.size < 7) {
                list + List(7 - list.size) { 1.0f }
            } else list
        }
    }

    LaunchedEffect(chartData) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        ) { progress, _ ->
            animatedValues = chartData.map { it * progress }
        }
    }

    Column(modifier = modifier) {
        // Chart container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0FFFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val barWidth = size.width / (animatedValues.size + 1)
                val maxHeight = size.height * 0.85f
                val spacing = barWidth * 0.2f

                animatedValues.forEachIndexed { index, value ->
                    val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                    val barHeight = maxHeight * normalizedValue
                    val x = spacing + (barWidth * index)
                    val y = size.height - barHeight

                    // Draw shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.1f),
                        topLeft = Offset(x + 2.dp.toPx(), y + 2.dp.toPx()),
                        size = Size(barWidth * 0.6f, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )

                    // Draw main bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimaryVariant.copy(alpha = 0.9f),
                                Primary.copy(alpha = 0.7f),
                                Secondary.copy(alpha = 0.5f)
                            ),
                            startY = y,
                            endY = size.height
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth * 0.6f, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )

                    // Draw highlight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth * 0.6f, barHeight * 0.3f),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }

                // Draw grid lines
                repeat(5) { index ->
                    val y = size.height * (index + 1) / 6f
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Labels and values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.take(7).forEachIndexed { index, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (index < animatedValues.size) {
                            String.format("%.1f", animatedValues[index])
                        } else "0.0",
                        fontSize = 10.sp,
                        color = PrimaryVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}