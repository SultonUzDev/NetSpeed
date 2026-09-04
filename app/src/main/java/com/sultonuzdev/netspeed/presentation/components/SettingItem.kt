package com.sultonuzdev.netspeed.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.*

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingItemPreview() {
    NetSpeedTheme {
        SettingItem(
            label = "Notification Style",
            description = "Choose compact or detailed view",
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
    description: String = "",
    isToggle: Boolean = false,
    isEnabled: Boolean = false,
    value: String = "",
    onToggleChange: ((Boolean) -> Unit)? = null,
    onValueClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A uniform row height. A Material Switch carries its own 48dp touch target while a
            // value chip does not, so equal padding produced toggle rows half again as tall as
            // the rest and a visibly uneven list.
            .heightIn(min = 60.dp)
            .padding(vertical = 4.dp),
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // A description is a hint, not a paragraph; two lines is the budget.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (isToggle) {
            ToggleSwitch(
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
        modifier = modifier
    )
}