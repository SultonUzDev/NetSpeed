package com.sultonuzdev.netspeed.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sultonuzdev.netspeed.presentation.theme.*

@Composable
fun UsageCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: String,
    amount: String,
    description: String,
    progress: Float = 0f,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        val animation = tween<Float>(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
        animate(
            initialValue = animatedProgress,
            targetValue = progress,
            animationSpec = animation
        ) { value, _ ->
            animatedProgress = value
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.netSpeedColors.cardBackground)
            .border(1.dp, MaterialTheme.netSpeedColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(25.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }

            if (amount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = amount,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

                if (progress > 0f) {
                    Spacer(modifier = Modifier.height(15.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }

            // Custom content
            content?.invoke(this)
        }
    }
}