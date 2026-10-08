package com.sultonuzdev.netspeed.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingItemPreview() {
    NetSpeedTheme {
        Column {
            SettingItem(
                label = "Notification Style",
                value = "Compact",
                isToggle = true,
                isEnabled = true,
                onToggleChange = {},
                onValueClick = {}
            )
            SettingItem(
                label = "Allow unrestricted battery",
                value = "Settings",
                showAttention = true,
                onValueClick = {}
            )
        }
    }
}

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    label: String,
    /** Optional. Most rows read fine from the label plus the value beside it. */
    isToggle: Boolean = false,
    isEnabled: Boolean = false,
    value: String = "",
    /**
     * Lifts the row out of the list: a breathing tinted container, an accented label and a
     * pulsing dot. For a row the user has a reason to act on now, and never more than one or
     * two on a screen -- its whole value is being the only thing moving.
     */
    showAttention: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onValueClick: (() -> Unit)? = null
) {
    // A dot alone was too quiet to find without reading every label, so the whole row is what
    // moves. Slow and low-amplitude on purpose: it has to be findable at a glance across a long
    // settings list without turning into a flashing advert.
    val pulse by rememberInfiniteTransition(label = "settingAttention").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val accent = MaterialTheme.colorScheme.primary
    val highlightShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (showAttention) {
                    Modifier
                        .clip(highlightShape)
                        .background(accent.copy(alpha = 0.06f + 0.12f * pulse))
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.30f + 0.35f * pulse),
                            shape = highlightShape
                        )
                } else {
                    Modifier
                }
            )
            // Value rows open a picker, so the whole row is the target; a boxed value beside a
            // switch gave the two row kinds different heights and a ragged right edge.
            .clickable(enabled = onValueClick != null) { onValueClick?.invoke() }
            .heightIn(min = 56.dp)
            .padding(end = if (showAttention) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (showAttention) FontWeight.SemiBold else FontWeight.Medium,
                color = if (showAttention) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = if (showAttention) 12.dp else 8.dp)
            )
            if (showAttention) {
                AttentionDot(modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (isToggle) {
            ToggleSwitch(
                label = label,
                checked = isEnabled,
                onCheckedChange = { onToggleChange?.invoke(it) }
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Two lines, not one. "Download and upload" is already long in English; in
                // Spanish and Portuguese "Personalizado" and "Automático" push past the 160dp cap
                // on their own. The row is heightIn(min = 56.dp), so it grows rather than clips.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ToggleSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Material's own Switch rather than a hand-drawn one. The custom version had two faults:
    //
    //  - Off, its track was surfaceVariant with no border, which is all but the same colour as
    //    the Settings background in both themes, so a switch that was off simply vanished. The
    //    real Switch draws an outline border on the unchecked track for exactly this reason.
    //  - Its thumb animated 24dp inside a 46dp usable track, overshooting the right edge by 2dp.
    //
    // It also brings the 48dp touch target, state semantics for accessibility, and the thumb
    // resize on press.
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        // The default `outline` border sinks into a near-black background in dark mode.
        colors = SwitchDefaults.colors(
            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.semantics { contentDescription = label }
    )
}
/**
 * A settings row that is an offer rather than a setting.
 *
 * Filled with its own gradient instead of borrowing the theme's accent, because the point is to
 * not look like the rows above it -- a promotion styled as a preference either gets missed or
 * gets resented for pretending. One per screen; a list of these is an advert, not a settings page.
 *
 * The colours are fixed rather than theme-derived: white on this gradient reads in both themes,
 * and a card that changed hue with the wallpaper would stop being recognisable.
 */
@Composable
fun GradientSettingItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFFFF7A45), Color(0xFFF5316F)),
    onClick: () -> Unit = {}
) {
    // A slow sheen rather than a blink: the gradient already carries the attention, this only
    // keeps it from reading as a static banner the eye learns to skip.
    val sheen by rememberInfiniteTransition(label = "gradientRow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheen"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .background(Color.White.copy(alpha = 0.10f * sheen))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        // The reference's badge: the icon floated on the gradient on its own reads as debris,
        // the disc behind it makes it an element.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientSettingItemPreview() {
    NetSpeedTheme(darkTheme = true) {
        GradientSettingItem(
            label = "More apps",
            value = "From Sulton UzDev",
            icon = Icons.Default.Apps
        )
    }
}
