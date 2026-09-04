package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.screens.usage.AppUsageRow

/**
 * An app's launcher icon, loaded off the main thread and cached.
 *
 * Seeds from the cache so an already-loaded icon appears on the first frame instead of flashing
 * a placeholder while a coroutine confirms what is already known.
 */
/**
 * An app's icon, or a letter/glyph fallback when it cannot be resolved. Shared by the usage list
 * and the per-app detail dialog so both degrade the same way.
 */
@Composable
fun AppIcon(
    packageName: String,
    fallbackLabel: String,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    val icon = appIcon(packageName)
    if (icon != null) {
        androidx.compose.foundation.Image(
            bitmap = icon,
            contentDescription = null,
            // Adaptive icons rasterise as full squares; the launcher masks them, so without a
            // clip here they sit noticeably boxier than expected.
            modifier = modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        return
    }

    // A shared Android glyph made every unresolvable row look identical. The first letter of the
    // label at least keeps them apart.
    val initial = fallbackLabel.firstOrNull { it.isLetterOrDigit() }?.uppercase()
    if (initial != null) {
        Text(
            text = initial,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size((sizeDp * 0.7).dp)
        )
    }
}

@Composable
private fun appIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState(
        initialValue = AppIconCache.peek(packageName),
        key1 = packageName
    ) {
        if (!AppIconCache.isLoaded(packageName)) {
            value = AppIconCache.load(context, packageName)
        }
    }
    return icon
}

@Composable
fun AppUsageListItem(
    row: AppUsageRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    packageName = row.packageName,
                    fallbackLabel = row.appLabel,
                    sizeDp = 32
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.appLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mobile ${row.mobileUsage}  ·  Wi-Fi ${row.wifiUsage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Relative bar: each app against the heaviest consumer in the window.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(row.shareOfMax)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = row.totalUsage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Shown in place of the per-app list when usage access has not been granted. It cannot be
 * requested at runtime, so the only thing to offer is a trip to system settings.
 */
@Composable
fun UsageAccessCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Turn on usage access",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Shows which apps use your data, and makes totals match Android " +
                        "Settings. Nothing leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Button(onClick = onGrantClick) {
                Text("Open settings")
            }
        }
    }
}
