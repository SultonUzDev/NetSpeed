package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * A small pulsing dot, for the one row on a screen that is worth looking at.
 *
 * A ring that grows out of the dot and fades, rather than a dot that blinks: blinking reads as a
 * rendering fault, while an outward pulse reads as a pointer. Deliberately quiet -- it sits next
 * to a label it must not outshout, so the core stays small and only the halo moves.
 *
 * Use it sparingly. Two of these on one screen cancel each other out.
 */
@Composable
fun AttentionDot(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "attentionDot")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )

    Canvas(modifier = modifier.size(14.dp)) {
        val coreRadius = size.minDimension * 0.21f
        val reach = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = 0.40f * (1f - halo)),
            radius = coreRadius + (reach - coreRadius) * halo
        )
        drawCircle(color = color, radius = coreRadius)
    }
}

@Preview
@Composable
private fun AttentionDotPreview() {
    NetSpeedTheme(darkTheme = true) {
        AttentionDot()
    }
}
