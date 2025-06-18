package com.sultonuzdev.netspeed.presentation.components


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.*

@Composable
fun BottomNavigation(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Speed,
            label = "Speed",
            isSelected = currentPage == 0,
            onClick = { onPageSelected(0) }
        )

        BottomNavItem(
            icon = Icons.Default.BarChart,
            label = "Usage",
            isSelected = currentPage == 1,
            onClick = { onPageSelected(1) }
        )

        BottomNavItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = currentPage == 2,
            onClick = { onPageSelected(2) }
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryVariant else TextSecondary,
        animationSpec = tween(300),
        label = "nav_color"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.9f,
        animationSpec = tween(300),
        label = "nav_scale"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) CardBackground else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 8.dp)
            .scale(animatedScale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedColor,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = animatedColor,
            letterSpacing = 0.5.sp
        )
    }
}