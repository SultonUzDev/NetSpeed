package com.sultonuzdev.netspeed.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sultonuzdev.netspeed.presentation.theme.NetSpeedTheme

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingItemPreview() {
    NetSpeedTheme {
        SettingItem(
            label = "Notification Style",
            value = "Compact",
            isToggle = true,
            isEnabled = true,
            onToggleChange = {},
            onValueClick = {}
        )
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
    onToggleChange: ((Boolean) -> Unit)? = null,
    onValueClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )

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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // Values like "Download and upload" otherwise take half the row and force
                    // the description beside them to wrap to three lines.
                    .widthIn(max = 140.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onValueClick?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
        modifier = modifier.semantics { contentDescription = label }
    )
}