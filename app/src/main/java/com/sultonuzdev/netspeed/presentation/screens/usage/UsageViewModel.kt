package com.sultonuzdev.netspeed.presentation.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.domain.usecases.GetUsageDataUseCase
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.getCurrentMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsageViewModel(
    private val getUsageDataUseCase: GetUsageDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    init {
        observeTodayUsage()
        observeWeeklyUsage()
        getMonthlyTotal()
    }

    private fun observeTodayUsage() {
        viewModelScope.launch {
            getUsageDataUseCase.getTodayUsage().collect { usageData ->
                val (wifiValue, wifiUnit) = NetworkUtils.formatDataUsage(usageData.wifiUsage)
                val (mobileValue, mobileUnit) = NetworkUtils.formatDataUsage(usageData.mobileUsage)
                val (totalValue, totalUnit) = NetworkUtils.formatDataUsage(usageData.totalUsage)

                _uiState.update { currentState ->
                    currentState.copy(
                        todayWifi = "$wifiValue $wifiUnit",
                        todayMobile = "$mobileValue $mobileUnit",
                        todayTotal = "$totalValue $totalUnit",
                        todayProgress = (usageData.totalUsage.toFloat() / (5L * 1024 * 1024 * 1024)).coerceAtMost(1f), // 5GB daily limit
                        sessionUsage = totalValue,
                        sessionUnit = totalUnit,
                        sessionTime = NetworkUtils.formatTime(usageData.sessionTime)
                    )
                }
            }
        }
    }

    private fun observeWeeklyUsage() {
        viewModelScope.launch {
            getUsageDataUseCase.getWeeklyUsage().collect { weeklyData ->
                val chartData = weeklyData.map { it.totalUsage.toFloat() / (1024 * 1024 * 1024) } // Convert to GB
                _uiState.update { currentState ->
                    currentState.copy(chartData = chartData)
                }
            }
        }
    }

    private fun getMonthlyTotal() {
        viewModelScope.launch {
            val monthlyTotal = getUsageDataUseCase.getMonthlyTotal(getCurrentMonth())
            val (monthlyValue, monthlyUnit) = NetworkUtils.formatDataUsage(monthlyTotal)
            val monthlyLimit = 25L * 1024 * 1024 * 1024 // 25GB
            val monthlyProgress = (monthlyTotal.toFloat() / monthlyLimit).coerceAtMost(1f)

            _uiState.update { currentState ->
                currentState.copy(
                    monthlyUsage = "$monthlyValue $monthlyUnit",
                    monthlyProgress = monthlyProgress,
                    monthlyLimit = "of 25 GB limit"
                )
            }
        }
    }

    fun refreshUsageData() {
        observeTodayUsage()
        observeWeeklyUsage()
        getMonthlyTotal()
    }
}

