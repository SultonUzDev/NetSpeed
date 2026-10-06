package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme
import kotlin.math.cos
import kotlin.math.sin

/** Bottom-left origin, bottom gap. The gap is where the caption sits. */
private const val GAUGE_START = 135f
private const val GAUGE_SWEEP = 270f

/** Discrete ticks read as a scale; a continuous ring reads as a spinner. */
private const val TICK_COUNT = 52

/** One spoke every few degrees is what gives the swept band its mesh. */
private const val MESH_STEP_DEGREES = 3f

/**
 * The speed test's dial.
 *
 * A gauge, not a decoration: [progress] fills it, so the ring says something the number does not.
 * Built from four rings that all share one origin -- a segmented tick scale at the rim, a mesh
 * band swept to the current value, a hairline track carrying on through the part not yet reached,
 * and a lit core holding the figure.
 *
 * Everything is drawn from due east and then turned to [GAUGE_START] as a whole, so the sweep
 * gradients line up with the arcs they fill without any per-stop angle arithmetic.
 */
@Composable
fun SpeedCircle(
    speed: String,
    unit: String,
    modifier: Modifier = Modifier,
    caption: String = "",
    /** 0..1 of the dial's range. Drives the fill; the figure stays the source of truth. */
    progress: Float = 0f,
    /** Sweeps a highlight round the rim while true. Only a running test should set it. */
    animating: Boolean = false,
    diameter: Dp = 250.dp
) {
    // Motion means exactly one thing: a test is running. Idle, the dial is still -- a resting
    // ring that keeps moving reads as work in progress and devalues the real signal.
    //
    // The core breathes rather than something travelling the rim: the band is already moving
    // with every live sample, and a second arc running round the scale only crossed the ticks
    // and read as a smudge.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(animating) {
        if (animating) {
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    // Throughput jumps around sample to sample; the fill is eased so the band glides rather than
    // flickering, while the printed figure keeps updating immediately.
    val fill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "gaugeFill"
    )

    val accentStart = MaterialTheme.colorScheme.primary
    val accentEnd = MaterialTheme.colorScheme.tertiary
    // The unreached part of the scale has to stay visible on white as well as on near-black,
    // so it is drawn from onSurface rather than a fixed grey.
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(degrees = GAUGE_START, pivot = center) {
                drawGauge(
                    fill = fill,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    idle = idle
                )
            }
            drawCore(accentStart = accentStart, surface = surface, pulse = pulse.value)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The figure tracks the ring rather than sitting at a fixed size: the dial shrinks
            // on a short screen once results appear below it, and grows on a tablet. The ceiling
            // is above 1 so the larger dial actually gets a larger figure instead of a correctly
            // sized one marooned in the middle of it.
            val scale = (diameter / 280.dp).coerceIn(0.7f, 1.35f)
            val base = MaterialTheme.typography.displayLarge
            Text(
                text = speed,
                style = base.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 50.sp * scale,
                    lineHeight = 54.sp * scale,
                    letterSpacing = base.letterSpacing * scale
                ),
                color = MaterialTheme.colorScheme.onSurface,
                // The stack is centred inside a fixed circle; a wrap would push the unit and
                // caption out of it rather than shrinking the text.
                maxLines = 1
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (caption.isNotEmpty()) {
            // Inside the gauge's own gap, not under the whole dial. The scale ends at 45 degrees
            // either side of due south, so the ring's lowest point is about 0.71r below centre
            // and everything past that is free.
            Text(
                text = caption,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = diameter * 0.07f)
            )
        }
    }
}

/** Rim scale, mesh band, hairline track and the running highlight, in that order. */
private fun DrawScope.drawGauge(
    fill: Float,
    accentStart: Color,
    accentEnd: Color,
    idle: Color
) {
    val radius = size.minDimension / 2f
    val lit = GAUGE_SWEEP * fill

    val meshInner = radius * 0.64f
    val meshOuter = radius * 0.8f
    val tickInner = radius * 0.9f
    // Mid-band, so the hairline reads as the mesh carrying on rather than as a ring of its own
    // sitting just outside the core.
    val trackRadius = (meshInner + meshOuter) / 2f

    // Spans the gauge's whole range, so a stop at a given fraction always lands on the same
    // angle no matter how far the fill has got.
    val ramp = Brush.sweepGradient(
        0f to accentStart,
        GAUGE_SWEEP / 360f to accentEnd,
        1f to accentEnd,
        center = center
    )

    // Rim scale. Lit ticks carry the gradient; the rest state the range that is left.
    repeat(TICK_COUNT) { index ->
        val t = index / (TICK_COUNT - 1f)
        val angle = GAUGE_SWEEP * t
        drawLine(
            // `t <= fill` alone lit the first tick at rest, leaving one blue mark on an
            // otherwise empty dial.
            color = if (fill > 0f && t <= fill) lerp(accentStart, accentEnd, t) else idle,
            start = polar(angle, tickInner),
            end = polar(angle, radius),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    if (lit > 0f) {
        // Wash first, then the grid over it: one wide stroke is far cheaper than filling the
        // band with a path, and the concentric arcs read as a mesh on top of it.
        val bandCentre = (meshInner + meshOuter) / 2f
        drawArc(
            brush = ramp,
            startAngle = 0f,
            sweepAngle = lit,
            useCenter = false,
            topLeft = Offset(center.x - bandCentre, center.y - bandCentre),
            size = Size(bandCentre * 2, bandCentre * 2),
            alpha = 0.20f,
            style = Stroke(width = meshOuter - meshInner)
        )

        repeat(4) { index ->
            val r = meshInner + (meshOuter - meshInner) * (index + 1) / 5f
            drawArc(
                brush = ramp,
                startAngle = 0f,
                sweepAngle = lit,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                alpha = 0.45f,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        var angle = 0f
        while (angle <= lit) {
            val t = angle / GAUGE_SWEEP
            drawLine(
                color = lerp(accentStart, accentEnd, t).copy(alpha = 0.5f),
                start = polar(angle, meshInner),
                end = polar(angle, meshOuter),
                strokeWidth = 1.dp.toPx()
            )
            angle += MESH_STEP_DEGREES
        }
    }

    // Hairline track, drawn only where the mesh has not reached. Running it under the band as
    // well put a bright seam through the middle of the grid.
    drawArc(
        color = idle,
        startAngle = lit,
        sweepAngle = GAUGE_SWEEP - lit,
        useCenter = false,
        topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
        size = Size(trackRadius * 2, trackRadius * 2),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
    if (lit > 0f) {
        // The band's inner boundary, which is what gives the sweep a defined edge against the
        // core instead of fading out into it.
        drawArc(
            brush = ramp,
            startAngle = 0f,
            sweepAngle = lit,
            useCenter = false,
            topLeft = Offset(center.x - meshInner, center.y - meshInner),
            size = Size(meshInner * 2, meshInner * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Leading edge. Three passes, widest and faintest first: a real blur needs a native
        // mask filter and a software layer, and at this size stacked strokes are indistinguishable
        // from one.
        val edge = lerp(accentStart, accentEnd, fill)
        listOf(10.dp to 0.12f, 6.dp to 0.26f, 2.dp to 0.95f).forEach { (width, alpha) ->
            drawLine(
                color = edge.copy(alpha = alpha),
                start = polar(lit, meshInner),
                end = polar(lit, radius),
                strokeWidth = width.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

}

/**
 * The lit disc the figure sits on.
 *
 * Drawn in surface rather than the reference's near-black: on a light theme a black disc put
 * every label on it -- figure, unit, caption -- dark on dark. The glow around it carries the
 * same effect in both themes because it is the accent at low alpha, not a fixed colour.
 */
private fun DrawScope.drawCore(accentStart: Color, surface: Color, pulse: Float) {
    val radius = size.minDimension / 2f
    val discRadius = radius * 0.5f

    drawCircle(
        brush = Brush.radialGradient(
            0f to accentStart.copy(alpha = 0.26f + 0.20f * pulse),
            0.72f to accentStart.copy(alpha = 0.10f + 0.10f * pulse),
            1f to Color.Transparent,
            center = center,
            radius = radius * 0.57f
        ),
        radius = radius * 0.57f,
        center = center
    )
    drawCircle(color = surface, radius = discRadius, center = center)
    drawCircle(
        color = accentStart.copy(alpha = 0.55f),
        radius = discRadius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

/** A point [r] out from the centre at [angleDegrees], measured from due east. */
private fun DrawScope.polar(angleDegrees: Float, r: Float): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    return Offset(
        x = center.x + (cos(radians) * r).toFloat(),
        y = center.y + (sin(radians) * r).toFloat()
    )
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpeedCircleDarkPreview() {
    NetSpeedTheme(darkTheme = true) {
        SpeedCircle(speed = "120", unit = "Mbps", caption = "Download", progress = 0.62f)
    }
}

@Preview
@Composable
private fun SpeedCircleLightPreview() {
    NetSpeedTheme(darkTheme = false) {
        SpeedCircle(speed = "120", unit = "Mbps", caption = "Download", progress = 0.62f)
    }
}
