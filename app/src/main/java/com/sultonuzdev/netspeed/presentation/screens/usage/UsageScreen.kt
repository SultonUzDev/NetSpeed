package com.sultonuzdev.netspeed.presentation.screens.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.presentation.components.UsageCard
import com.sultonuzdev.netspeed.presentation.components.UsageChart
import com.sultonuzdev.netspeed.presentation.theme.PrimaryVariant
import com.sultonuzdev.netspeed.presentation.theme.TextSecondary
import org.koin.androidx.compose.koinViewModel

@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Data Usage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryVariant
            )
            Text(
                text = "Monitor your daily and monthly consumption",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        // Usage Cards
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            UsageCard(
                title = "Today's Usage",
                icon = "📊",
                amount = uiState.todayTotal,
                description = "WiFi: ${uiState.todayWifi} • Mobile: ${uiState.todayMobile}",
                progress = uiState.todayProgress
            )

            UsageCard(
                title = "This Month",
                icon = "📅",
                amount = uiState.monthlyUsage,
                description = uiState.monthlyLimit,
                progress = uiState.monthlyProgress
            )

            UsageCard(
                title = "Session Usage",
                icon = "⏱️",
                amount = "${uiState.sessionUsage} ${uiState.sessionUnit}",
                description = "Current session • ${uiState.sessionTime}"
            )
        }

        // Weekly Chart
        UsageCard(
            title = "Weekly Overview",
            icon = "📈",
            amount = "",
            description = "",
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            UsageChart(
                data = uiState.chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 20.dp)
            )
        }
    }
}