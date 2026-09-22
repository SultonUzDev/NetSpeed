package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Space a screen must leave below its content so the floating bar does not cover it.
 *
 * The bar overlays content rather than occupying a Scaffold slot, so nothing reserves its height
 * automatically: 62dp of content plus 10dp of margin above and below. Screens add the
 * navigation-bar inset on top of this, which the bar also applies to itself.
 */
val BottomNavigationHeight = 82.dp

private data class NavDestination(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

private val destinations = listOf(
    NavDestination(Icons.Outlined.Speed, Icons.Filled.Speed, "Speed"),
    NavDestination(Icons.Outlined.BarChart, Icons.Filled.BarChart, "Usage"),
    NavDestination(Icons.Outlined.History, Icons.Filled.History, "History"),
    NavDestination(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

/**
 * Floating, rounded, translucent bottom navigation, following Telegram's Liquid Glass redesign:
 * a capsule that hovers above the bottom edge rather than sitting welded to it, with the page
 * showing faintly through it, a hairline rim, and the label under each icon.
 *
 * Translucency here comes from alpha over the page background rather than a true backdrop blur --
 * Compose's blur modifier blurs a composable's own content, not what is behind it, and real
 * backdrop blur needs a RenderEffect on the window (API 31+) or a third-party layer.
 */
@Composable
fun BottomNavigation(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        // The rim is what stops a translucent panel reading as a smudge; it gives the glass
        // an edge to catch light on.
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        ),

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                NavItem(
                    destination = destination,
                    isSelected = currentPage == index,
                    onClick = { onPageSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_tint"
    )

    // A slight lift on the active icon, which is the "softer, floating" part of the language.
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "nav_icon_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // M3's active indicator: tint alone was too subtle in light mode, where primary and
        // onSurfaceVariant are close in value.
        val pill by animateColorAsState(
            targetValue = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f)
            },
            animationSpec = tween(durationMillis = 200),
            label = "nav_item_pill"
        )
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                contentDescription = destination.label,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = tint,
            maxLines = 1
        )
    }
}
