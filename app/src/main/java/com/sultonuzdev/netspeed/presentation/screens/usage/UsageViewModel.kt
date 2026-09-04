package com.sultonuzdev.netspeed.presentation.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.DailyUsageData
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DayUsageDetail
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.usecases.CheckDataLimitUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetAccurateUsageUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetAppUsageUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageForecastUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageDataUseCase
import com.sultonuzdev.netspeed.presentation.components.UsageBar
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.UsagePeriods
import com.sultonuzdev.netspeed.utils.getCurrentMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageViewModel(
    private val getUsageDataUseCase: GetUsageDataUseCase,
    private val getAccurateUsageUseCase: GetAccurateUsageUseCase,
    private val getAppUsageUseCase: GetAppUsageUseCase,
    private val checkDataLimitUseCase: CheckDataLimitUseCase,
    private val getUsageForecastUseCase: GetUsageForecastUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    private var dailyMobileBudgetBytes = 0L
    private var refreshJob: Job? = null
    private var todayJob: Job? = null
    private var historyJob: Job? = null
    private var appUsageJob: Job? = null
    private var appDetailJob: Job? = null
    private var dayDetailJob: Job? = null

    /** Percentage of the daily budget that counts as a warning; mirrors the alert threshold. */
    private var warningThresholdPercent = 80

    /**
     * Re-reads everything, re-checking usage access first.
     *
     * Worth calling whenever the screen is resumed: usage access is granted in system settings, so
     * the user can come back having enabled it without any callback firing.
     */
    fun refresh() {
        // Both screens call this on resume, and it used to also run from init, so two refreshes
        // could overlap -- the second cancelling the first's per-app query mid-flight.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val limit = preferencesManager.dataLimit.first()
            val resetDay = preferencesManager.monthlyResetDate.first()
            dailyMobileBudgetBytes =
                if (limit <= 0L) 0L else limit / UsagePeriods.daysInCycle(resetDay)
            warningThresholdPercent = preferencesManager.warningThreshold.first().coerceIn(1, 100)

            val hasAccess = getAppUsageUseCase.hasUsageAccess()
            _uiState.update { it.copy(hasUsageAccess = hasAccess, isUsageAccurate = hasAccess) }

            // Ordered cheapest-and-most-visible first. Platform queries are serialised, and
            // the 30-day history sweep is 60 of them, so starting it first would make the
            // per-app list wait behind it on every entry.
            loadAppUsage()
            loadDataLimitStatus()
            loadForecast()
            observeTodayUsage()
            loadCycleTotals(hasAccess)
            loadDailyHistory(hasAccess)
        }
    }

    /** Opens the detail view for one app over the currently selected period. */
    fun selectApp(uid: Int) {
        appDetailJob?.cancel()
        appDetailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAppDetail = true) }

            val period = _uiState.value.appUsagePeriod
            val detail = try {
                when (period) {
                    AppUsagePeriod.TODAY -> getAppUsageUseCase.detailForToday(uid)
                    AppUsagePeriod.CYCLE -> getAppUsageUseCase.detailForCycle(
                        uid,
                        preferencesManager.monthlyResetDate.first()
                    )
                }
            } catch (e: Exception) {
                null
            }

            // Share is derived from the list already on screen rather than re-queried, so the
            // percentage always agrees with the rows the user is looking at.
            val periodTotal = _uiState.value.appUsage.sumOf { it.totalBytes }
            _uiState.update { state ->
                state.copy(
                    selectedApp = detail?.copy(
                        shareOfPeriod = if (periodTotal > 0L) {
                            (detail.totalBytes.toFloat() / periodTotal).coerceIn(0f, 1f)
                        } else 0f,
                        periodLabel = period.label
                    ),
                    isLoadingAppDetail = false
                )
            }
        }
    }

    fun clearSelectedApp() {
        appDetailJob?.cancel()
        _uiState.update { it.copy(selectedApp = null, isLoadingAppDetail = false) }
    }

    fun selectAppUsagePeriod(period: AppUsagePeriod) {
        if (_uiState.value.appUsagePeriod == period) return
        // The open detail belongs to the old period, so it would be stale.
        _uiState.update { it.copy(appUsagePeriod = period, selectedApp = null) }
        loadAppUsage()
    }

    // --- today -------------------------------------------------------------------------------

    private fun observeTodayUsage() {
        todayJob?.cancel()
        todayJob = viewModelScope.launch {
            getUsageDataUseCase.getTodayUsage().collect { usageData ->
                // With usage access the platform figure wins; the Room row is the fallback.
                val usage = if (_uiState.value.hasUsageAccess) {
                    getAccurateUsageUseCase.today() ?: usageData
                } else {
                    usageData
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        todayWifi = NetworkUtils.formatBytes(usage.wifiUsage),
                        todayMobile = NetworkUtils.formatBytes(usage.mobileUsage),
                        todayTotal = NetworkUtils.formatBytes(usage.totalUsage),
                        todayProgress = dailyProgress(usage.mobileUsage),
                        sessionTime = NetworkUtils.formatTime(usageData.sessionTime)
                    )
                }
            }
        }
    }

    // --- day-by-day history ------------------------------------------------------------------

    private fun loadDailyHistory(hasAccess: Boolean) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (hasAccess) {
                    val history = getAccurateUsageUseCase.dailyHistory(HISTORY_DAYS)
                    publishHistory(history)
                } else {
                    getUsageDataUseCase.getMonthlyUsage(getCurrentMonth()).collect { monthlyData ->
                        publishHistory(fillMissingDaysOfMonth(monthlyData))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        dailyUsageHistory = fillMissingDaysOfMonth(emptyList())
                            .map { day -> day.toRow() },
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun publishHistory(history: List<UsageData>) {
        val todayKey = UsagePeriods.dayKey()
        val newestFirst = history.sortedByDescending { it.date }
        val rows = newestFirst.map { usage -> usage.toRow(isToday = usage.date == todayKey) }

        _uiState.update { currentState ->
            currentState.copy(
                dailyUsageHistory = rows,
                last7DaysUsage = summarise("Last 7 days", newestFirst.take(7)),
                last30DaysUsage = summarise("Last 30 days", newestFirst.take(30)),
                dailyChart = newestFirst.take(7).reversed().map { usage ->
                    UsageBar(
                        label = shortDayLabel(usage.date),
                        totalBytes = usage.totalUsage,
                        isToday = usage.date == todayKey
                    )
                },
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadCycleTotals(hasAccess: Boolean) {
        viewModelScope.launch {
            val resetDay = preferencesManager.monthlyResetDate.first()

            val cycle = if (hasAccess) {
                getAccurateUsageUseCase.cycleTotal(resetDay)
            } else {
                val bounds = UsagePeriods.billingCycleBounds(resetDay)
                val start = UsagePeriods.dayKey(bounds.first)
                val end = UsagePeriods.dayKey(bounds.last)
                getUsageDataUseCase.getUsageInRange(start, end)
            }

            val totals = cycle ?: UsageData(date = "")
            _uiState.update { currentState ->
                currentState.copy(
                    cycleTotals = DailyUsageData(
                        title = "This cycle",
                        mobileUsage = NetworkUtils.formatBytes(totals.mobileUsage),
                        wifiUsage = NetworkUtils.formatBytes(totals.wifiUsage),
                        totalUsage = NetworkUtils.formatBytes(totals.totalUsage)
                    ),
                )
            }
        }
    }

    /** Standing against the mobile-data cap, for the progress card above the table. */
    private fun loadDataLimitStatus() {
        viewModelScope.launch {
            val enabled = preferencesManager.dataLimitAlert.first()
            val status = try {
                checkDataLimitUseCase.status()
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(dataLimitStatus = status, dataLimitAlertEnabled = enabled)
            }
        }
    }

    /** Projection for the cap card; recomputed on refresh rather than polled. */
    private fun loadForecast() {
        viewModelScope.launch {
            val forecast = runCatching { getUsageForecastUseCase() }.getOrNull()
            val limits = runCatching { preferencesManager.appLimits.first() }
                .getOrDefault(emptyMap())
            _uiState.update { it.copy(forecast = forecast, appLimits = limits) }
        }
    }

    /** Sets or clears one app's cycle allowance; zero removes it. */
    fun setAppLimit(uid: Int, bytes: Long) {
        viewModelScope.launch {
            preferencesManager.updateAppLimit(uid, bytes)
            _uiState.update { state ->
                val limits = state.appLimits.toMutableMap()
                if (bytes > 0L) limits[uid] = bytes else limits.remove(uid)
                state.copy(appLimits = limits)
            }
        }
    }

    // --- per-app -----------------------------------------------------------------------------

    private fun loadAppUsage() {
        appUsageJob?.cancel()
        appUsageJob = viewModelScope.launch {
            if (!getAppUsageUseCase.hasUsageAccess()) {
                _uiState.update {
                    it.copy(appUsage = emptyList(), isLoadingApps = false, hasUsageAccess = false)
                }
                return@launch
            }

            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = try {
                when (_uiState.value.appUsagePeriod) {
                    AppUsagePeriod.TODAY -> getAppUsageUseCase.forToday()
                    AppUsagePeriod.CYCLE ->
                        getAppUsageUseCase.forCycle(preferencesManager.monthlyResetDate.first())
                }
            } catch (e: Exception) {
                emptyList()
            }

            val max = apps.maxOfOrNull { it.totalBytes } ?: 0L
            _uiState.update { currentState ->
                currentState.copy(
                    appUsage = apps.map { it.toRow(max) },
                    isLoadingApps = false
                )
            }
        }
    }

    // --- mapping helpers ---------------------------------------------------------------------

    private fun AppUsage.toRow(maxBytes: Long) = AppUsageRow(
        uid = uid,
        packageName = packageName,
        appLabel = appLabel,
        mobileUsage = NetworkUtils.formatBytes(mobileBytes),
        wifiUsage = NetworkUtils.formatBytes(wifiBytes),
        totalUsage = NetworkUtils.formatBytes(totalBytes),
        totalBytes = totalBytes,
        shareOfMax = if (maxBytes > 0L) (totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f
    )

    private fun UsageData.toRow(isToday: Boolean = false) = DailyUsageData(
        title = if (isToday) "Today" else formatDisplayDate(date),
        mobileUsage = NetworkUtils.formatBytes(mobileUsage),
        wifiUsage = NetworkUtils.formatBytes(wifiUsage),
        totalUsage = NetworkUtils.formatBytes(totalUsage),
        isToday = isToday,
        dateKey = date,
        mobileBytes = mobileUsage,
        limitLevel = levelForDay(mobileUsage)
    )

    /**
     * A day's standing against its share of the cap. Compared per-day rather than per-cycle so a
     * single heavy day stands out even while the month as a whole is still within budget.
     */
    private fun levelForDay(mobileBytes: Long): DataLimitLevel {
        val budget = dailyMobileBudgetBytes
        if (budget <= 0L) return DataLimitLevel.NONE
        val warningBytes = (budget.toDouble() * warningThresholdPercent / 100).toLong()
        return when {
            mobileBytes >= budget -> DataLimitLevel.REACHED
            mobileBytes >= warningBytes -> DataLimitLevel.WARNING
            else -> DataLimitLevel.NONE
        }
    }

    /** Opens one day from the History table: its totals, then its per-app breakdown. */
    fun selectDay(dateKey: String) {
        val dayMillis = UsagePeriods.millisForDayKey(dateKey) ?: return

        dayDetailJob?.cancel()
        dayDetailJob = viewModelScope.launch {
            val label = if (dateKey == UsagePeriods.dayKey()) {
                "Today"
            } else {
                formatDisplayDate(dateKey)
            }

            val totals = if (getAccurateUsageUseCase.hasUsageAccess()) {
                getAccurateUsageUseCase.forDay(dayMillis)
            } else {
                null
            } ?: getUsageDataUseCase.getUsageInRange(dateKey, dateKey)

            // Totals first so the dialog opens immediately; the per-app query is the slow part.
            _uiState.update {
                it.copy(
                    selectedDay = DayUsageDetail(
                        dateKey = dateKey,
                        dateLabel = label,
                        mobileBytes = totals.mobileUsage,
                        wifiBytes = totals.wifiUsage,
                        budgetBytes = dailyMobileBudgetBytes,
                        level = levelForDay(totals.mobileUsage),
                        isLoadingApps = getAppUsageUseCase.hasUsageAccess()
                    )
                )
            }

            if (!getAppUsageUseCase.hasUsageAccess()) return@launch

            val apps = try {
                getAppUsageUseCase.forDay(dayMillis).take(TOP_APPS_PER_DAY)
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update { state ->
                // Guard against a different day having been opened while this was in flight.
                val current = state.selectedDay ?: return@update state
                if (current.dateKey != dateKey) return@update state
                state.copy(selectedDay = current.copy(topApps = apps, isLoadingApps = false))
            }
        }
    }

    fun clearSelectedDay() {
        dayDetailJob?.cancel()
        _uiState.update { it.copy(selectedDay = null) }
    }

    private fun summarise(title: String, data: List<UsageData>) = DailyUsageData(
        title = title,
        mobileUsage = NetworkUtils.formatBytes(data.sumOf { it.mobileUsage }),
        wifiUsage = NetworkUtils.formatBytes(data.sumOf { it.wifiUsage }),
        totalUsage = NetworkUtils.formatBytes(data.sumOf { it.totalUsage })
    )

    /** Fallback path: pads the current month's stored rows out to one row per elapsed day. */
    private fun fillMissingDaysOfMonth(existing: List<UsageData>): List<UsageData> {
        val byDate = existing.associateBy { it.date }
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return (1..today).map { day ->
            calendar.set(year, month, day)
            val key = formatter.format(calendar.time)
            byDate[key] ?: UsageData(date = key)
        }
    }

    /** Weekday initial-ish label for a chart column, e.g. "Mon". */
    private fun shortDayLabel(dateString: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("EEE", Locale.getDefault())
            output.format(input.parse(dateString) ?: Date())
        } catch (e: Exception) {
            dateString.takeLast(2)
        }
    }

    private fun formatDisplayDate(dateString: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("MMM d", Locale.getDefault())
            output.format(input.parse(dateString) ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Today's mobile usage against its share of the cap. Replaces a hardcoded 5 GB that ignored
     * whatever limit the user had actually configured.
     */
    private fun dailyProgress(mobileBytes: Long): Float {
        val budget = dailyMobileBudgetBytes
        return if (budget > 0L) (mobileBytes.toFloat() / budget).coerceIn(0f, 1f) else 0f
    }

    private companion object {
        const val HISTORY_DAYS = 30
        const val TOP_APPS_PER_DAY = 8
    }
}
