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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme

@Composable
fun SpeedCircle(
    speed: String,
    unit: String,
    type: String,
    modifier: Modifier = Modifier,
    /** Optional second line inside the circle; null hides it. */
    secondary: String? = null
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
        val surfaceColor = MaterialTheme.colorScheme.surface
        val colors= listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.primary,
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

        // Inner circle background.
        //
        // This was a hardcoded near-black (#1a1a1a). In dark mode that matched the surface by
        // coincidence; in light mode it put a black disc on a white page and every label on it
        // -- the speed, the unit, the caption -- was dark-on-dark and unreadable. Taking the
        // surface colour keeps the dark appearance identical and makes light mode work.
        Canvas(
            modifier = Modifier.size(230.dp)
        ) {
            drawCircle(
                color = surfaceColor,
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
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                // The stack is centred inside a fixed circle; a wrap would push the unit and
                // caption out of it rather than shrinking the text.
                maxLines = 1
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            // Only populated in the download-and-upload mode, where one number cannot carry both.
            if (secondary != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}


@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpeedCirclePreview() {
    NetSpeedTheme {
        SpeedCircle(
            speed = "100",
            unit = "Mbps",
            type = "Download",
            secondary = "Upload",
            modifier = Modifier.fillMaxSize()
        )
    }

}
