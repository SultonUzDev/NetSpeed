package com.sultonuzdev.netspeed.presentation.screens.usage

data class UsageUiState(
    val todayWifi: String = "0 B",
    val todayMobile: String = "0 B",
    val todayTotal: String = "0 B",
    val todayProgress: Float = 0f,
    val monthlyUsage: String = "0 B",
    val monthlyProgress: Float = 0f,
    val monthlyLimit: String = "of 25 GB limit",
    val sessionUsage: String = "0",
    val sessionUnit: String = "B",
    val sessionTime: String = "0s",
    val chartData: List<Float> = emptyList(),
    val isLoading: Boolean = false
)