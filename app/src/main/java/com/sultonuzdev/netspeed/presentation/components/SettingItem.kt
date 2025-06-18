package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.*

@Composable
fun SettingItem(
    label: String,
    description: String,
    isToggle: Boolean = false,
    isEnabled: Boolean = false,
    value: String = "",
    onToggleChange: ((Boolean) -> Unit)? = null,
    onValueClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (isToggle) {
            ToggleSwitch(
                checked = isEnabled,
                onCheckedChange = { onToggleChange?.invoke(it) }
            )
        } else {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
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
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) PrimaryVariant else Color(0x33FFFFFF),
        animationSpec = tween(300)
    )

    val thumbOffset by animateOffsetAsState(
        targetValue = if (checked) Offset(24f, 0f) else Offset(0f, 0f),
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(thumbOffset.x.dp, thumbOffset.y.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}