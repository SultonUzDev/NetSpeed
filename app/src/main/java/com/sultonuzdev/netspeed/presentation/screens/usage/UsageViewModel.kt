package com.sultonuzdev.netspeed.presentation.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.domain.models.DailyUsageData
import com.sultonuzdev.netspeed.domain.usecases.GetUsageDataUseCase
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.getCurrentMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UsageViewModel(
    private val getUsageDataUseCase: GetUsageDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    init {
        observeTodayUsage()
        observeWeeklyUsage()
        getMonthlyTotal()
        loadDailyUsageHistory()
        loadUsageStatistics()
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
                        todayProgress = (usageData.totalUsage.toFloat() / (5L * 1024 * 1024 * 1024)).coerceAtMost(
                            1f
                        ), // 5GB daily limit
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
                val chartData =
                    weeklyData.map { it.totalUsage.toFloat() / (1024 * 1024 * 1024) } // Convert to GB
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

    private fun loadDailyUsageHistory() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Get current month data - this will show the full current month
                val currentMonth = getCurrentMonth()
                getUsageDataUseCase.getMonthlyUsage(currentMonth).collect { monthlyData ->
                    // Generate complete month data including missing dates with 0 values
                    val completeMonthData = generateCompleteMonthData(monthlyData)

                    _uiState.update { currentState ->
                        currentState.copy(
                            dailyUsageHistory = completeMonthData,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error - show current month with 0 values
                _uiState.update { currentState ->
                    currentState.copy(
                        dailyUsageHistory = generateCurrentMonthWithZeros(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadUsageStatistics() {
        viewModelScope.launch {
            try {
                // Load Last 7 Days statistics
                launch {
                    getUsageDataUseCase.getDailyUsageHistory(7).collect { last7DaysData ->
                        val last7Stats = calculateUsageStatistics(
                            "Last 7 days",
                            last7DaysData)

                        _uiState.update { currentState ->
                            currentState.copy(last7DaysUsage = last7Stats)
                        }
                    }
                }

                // Load Last 30 Days statistics
                launch {
                    getUsageDataUseCase.getDailyUsageHistory(30).collect { last30DaysData ->
                        val last30Stats = calculateUsageStatistics(
                            "Last 30 days",
                            last30DaysData)

                        _uiState.update { currentState ->
                            currentState.copy(last30DaysUsage = last30Stats)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error - show 0 values
                _uiState.update { currentState ->
                    currentState.copy(
                        last7DaysUsage = DailyUsageData("Last 7 days"),
                        last30DaysUsage = DailyUsageData("Last 30 days")
                    )
                }
            }
        }
    }

    private fun calculateUsageStatistics(
        title: String,
        usageDataList: List<com.sultonuzdev.netspeed.domain.models.UsageData>
    ): DailyUsageData {
        var totalMobile = 0L
        var totalWifi = 0L
        var totalUsage = 0L

        usageDataList.forEach { usage ->
            totalMobile += usage.mobileUsage
            totalWifi += usage.wifiUsage
            totalUsage += usage.totalUsage
        }

        val (mobileValue, mobileUnit) = NetworkUtils.formatDataUsage(totalMobile)
        val (wifiValue, wifiUnit) = NetworkUtils.formatDataUsage(totalWifi)
        val (totalValue, totalUnit) = NetworkUtils.formatDataUsage(totalUsage)

        return DailyUsageData(
            title = title,
            mobileUsage = "$mobileValue $mobileUnit",
            wifiUsage = "$wifiValue $wifiUnit",
            totalUsage = "$totalValue $totalUnit",
            isToday = false,
            isCurrentMonth = false
        )
    }

    private fun generateCompleteMonthData(existingData: List<com.sultonuzdev.netspeed.domain.models.UsageData>): List<DailyUsageData> {
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val todayDate = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a map of existing data for quick lookup
        val dataMap = existingData.associateBy { it.date }

        val result = mutableListOf<DailyUsageData>()

        // Generate data for each day from 1st to today
        for (day in 1..todayDate) {
            calendar.set(currentYear, currentMonth, day)
            val dateString =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val isToday = dateString == today

            val usageData = dataMap[dateString]

            if (usageData != null) {
                // Use existing data
                val (mobileValue, mobileUnit) = NetworkUtils.formatDataUsage(usageData.mobileUsage)
                val (wifiValue, wifiUnit) = NetworkUtils.formatDataUsage(usageData.wifiUsage)
                val (totalValue, totalUnit) = NetworkUtils.formatDataUsage(usageData.totalUsage)

                result.add(
                    DailyUsageData(
                        title = if (isToday) "Today" else formatDisplayDate(dateString),
                        mobileUsage = "$mobileValue $mobileUnit",
                        wifiUsage = "$wifiValue $wifiUnit",
                        totalUsage = "$totalValue $totalUnit",
                        isToday = isToday
                    )
                )
            } else {
                // Use 0 values for missing data
                result.add(
                    DailyUsageData(
                        title = if (isToday) "Today" else formatDisplayDate(dateString),
                        mobileUsage = "0 B",
                        wifiUsage = "0 B",
                        totalUsage = "0 B",
                        isToday = isToday
                    )
                )
            }
        }

        // Sort with Today first, then descending by date
        return result.sortedWith { a, b ->
            when {
                a.isToday -> -1  // Today first
                b.isToday -> 1
                else -> {
                    // Convert display dates back to comparable format for sorting
                    val dateA = if (a.isToday) today else convertDisplayDateToSortable(a.title)
                    val dateB = if (b.isToday) today else convertDisplayDateToSortable(b.title)
                    dateB.compareTo(dateA)  // Descending order (newest first)
                }
            }
        }
    }

    private fun generateCurrentMonthWithZeros(): List<DailyUsageData> {
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val todayDate = calendar.get(Calendar.DAY_OF_MONTH)

        val result = mutableListOf<DailyUsageData>()

        // Generate data for each day from 1st to today with 0 values
        for (day in 1..todayDate) {
            calendar.set(currentYear, currentMonth, day)
            val dateString =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val isToday = dateString == today

            result.add(
                DailyUsageData(
                    title = if (isToday) "Today" else formatDisplayDate(dateString),
                    mobileUsage = "0 B",
                    wifiUsage = "0 B",
                    totalUsage = "0 B",
                    isToday = isToday
                )
            )
        }

        // Sort with Today first, then descending by date
        return result.sortedWith { a, b ->
            when {
                a.isToday -> -1  // Today first
                b.isToday -> 1
                else -> {
                    val dateA = if (a.isToday) today else convertDisplayDateToSortable(a.title)
                    val dateB = if (b.isToday) today else convertDisplayDateToSortable(b.title)
                    dateB.compareTo(dateA)  // Descending order (newest first)
                }
            }
        }
    }

    private fun formatDisplayDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun convertDisplayDateToSortable(displayDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(displayDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            displayDate
        }
    }

    fun refreshUsageData() {
        observeTodayUsage()
        observeWeeklyUsage()
        getMonthlyTotal()
        loadDailyUsageHistory()
        loadUsageStatistics()
    }
}