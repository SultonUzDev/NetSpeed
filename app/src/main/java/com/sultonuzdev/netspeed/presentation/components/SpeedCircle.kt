package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedCircle(
    speed: String,
    unit: String,
    type: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        val colors= listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.error,
            Color.Transparent
        )
        // Rotating gradient border
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors =colors
                ),
                radius = size.width / 2,
                center = center
            )
        }

        // Inner circle background
        Canvas(
            modifier = Modifier.size(230.dp)
        ) {
            drawCircle(
                color = Color(0xFF1a1a1a),
                radius = size.width / 2,
                center = center
            )
        }

        // Speed text content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
        ) {
            Text(
                text = speed,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = unit,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type.uppercase(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primaryContainer,
                letterSpacing = 1.sp
            )
        }
    }
}
