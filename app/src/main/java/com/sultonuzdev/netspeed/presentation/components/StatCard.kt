package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.*

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PrimaryVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}