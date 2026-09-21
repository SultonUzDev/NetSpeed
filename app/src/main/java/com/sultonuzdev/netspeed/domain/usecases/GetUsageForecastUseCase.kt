package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.data.repository.NetworkStatsRepository
import com.sultonuzdev.netspeed.domain.models.UsageForecast
import com.sultonuzdev.netspeed.utils.UsagePeriods
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * Projects mobile usage to the end of the billing cycle from the rate so far.
 *
 * A straight linear extrapolation, deliberately: usage is bursty day to day, and a smarter model
 * would be harder to explain than it is accurate. "At this rate" is a claim the arithmetic can
 * actually support.
 */
class GetUsageForecastUseCase(
    private val preferencesManager: PreferencesManager,
    private val networkStats: NetworkStatsRepository,
    private val getUsageDataUseCase: GetUsageDataUseCase
) {

    suspend operator fun invoke(): UsageForecast? {
        val limit = preferencesManager.dataLimit.first()
        if (limit <= 0L) return null

        val resetDay = preferencesManager.monthlyResetDate.first()
        val bounds = UsagePeriods.billingCycleBounds(resetDay)
        val now = System.currentTimeMillis()

        val cycleDays = UsagePeriods.daysInCycle(resetDay)
        // Part-days count: a cycle three hours old has elapsed 0.125 days, not zero, and dividing
        // by zero would make the projection infinite on the first morning.
        val elapsedDays = ((now - bounds.first).toDouble() /
                TimeUnit.DAYS.toMillis(1)).coerceAtLeast(MIN_ELAPSED_DAYS)
        if (elapsedDays >= cycleDays) return null

        val used = mobileUsedThisCycle(resetDay, bounds)
        val perDay = used / elapsedDays
        val projected = (perDay * cycleDays).toLong()
        val remainingDays = (cycleDays - elapsedDays).coerceAtLeast(0.0)

        // Days until the cap is reached at the current rate, if it will be at all.
        val daysUntilLimit = if (perDay > 0 && used < limit) {
            ceil((limit - used) / perDay).toInt()
        } else {
            null
        }

        return UsageForecast(
            usedBytes = used,
            limitBytes = limit,
            projectedBytes = projected,
            perDayBytes = perDay.toLong(),
            daysRemaining = ceil(remainingDays).toInt(),
            daysUntilLimit = daysUntilLimit?.takeIf { it <= remainingDays }
        )
    }

    private suspend fun mobileUsedThisCycle(resetDay: Int, bounds: LongRange): Long {
        if (networkStats.hasUsageAccess()) {
            networkStats.getCycleUsage(resetDay)?.let { return it.mobileUsage }
        }
        return getUsageDataUseCase.getUsageInRange(
            UsagePeriods.dayKey(bounds.first),
            UsagePeriods.dayKey(bounds.last)
        ).mobileUsage
    }

    private companion object {
        const val MIN_ELAPSED_DAYS = 0.1
    }
}
