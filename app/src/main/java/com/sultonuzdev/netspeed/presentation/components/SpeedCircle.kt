package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme

@Composable
fun SpeedCircle(
    speed: String,
    unit: String,
    modifier: Modifier = Modifier,
    /** Sweeps the ring while true; false lets it settle. Only a running test should set it. */
    animating: Boolean = false,
    diameter: Dp = 250.dp
) {
    // The ring used to spin (and the text pulse) unconditionally, so an idle "--" and a finished
    // result both looked like work in progress. Motion now means exactly one thing: a test is
    // running. When it stops, the sweep glides to rest instead of freezing mid-turn.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(animating) {
        if (animating) {
            rotation.snapTo(rotation.value % 360f)
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            rotation.snapTo(0f)
        }
    }

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val colors= listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.primary,
            // Not `error`: a red sweep around an idle dial read as a fault.
            MaterialTheme.colorScheme.tertiary,
            Color.Transparent
        )
        // Rotating gradient border
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation.value)
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
            modifier = Modifier.size(diameter - 20.dp)
        ) {
            drawCircle(
                color = surfaceColor,
                radius = size.width / 2,
                center = center
            )
        }

        // Speed text content
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Sized for the full 280dp dial. Once results appear below, the dial gives up height
            // and a fixed figure ran edge to edge, so the value scales with the ring instead.
            val scale = (diameter / 280.dp).coerceIn(0.7f, 1f)
            val base = MaterialTheme.typography.displayLarge
            Text(
                text = speed,
                style = base.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 54.sp * scale,
                    lineHeight = 58.sp * scale,
                    letterSpacing = base.letterSpacing * scale
                ),
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
            animating = true,
            modifier = Modifier.fillMaxSize()
        )
    }

}
