package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.models.DataLimitLevel
import com.sultonuzdev.netspeed.domain.models.DataLimitStatus
import com.sultonuzdev.netspeed.utils.UsagePeriods
import kotlinx.coroutines.flow.first

/**
 * Works out where mobile usage sits against the user's cap, and decides whether that warrants a
 * notification the user has not already been shown this cycle.
 */
class CheckDataLimitUseCase(
    private val preferencesManager: PreferencesManager,
    private val getAccurateUsageUseCase: GetAccurateUsageUseCase,
    private val getUsageDataUseCase: GetUsageDataUseCase
) {

    /** Current standing against the cap, regardless of whether an alert is due. */
    suspend fun status(): DataLimitStatus {
        val limit = preferencesManager.dataLimit.first()
        val resetDay = preferencesManager.monthlyResetDate.first()
        val threshold = preferencesManager.warningThreshold.first().coerceIn(1, 100)

        val bounds = UsagePeriods.billingCycleBounds(resetDay)
        val cycleKey = UsagePeriods.dayKey(bounds.first)
        val used = mobileUsageForCycle(resetDay, bounds)

        return DataLimitStatus(
            usedBytes = used,
            limitBytes = limit,
            level = levelFor(used, limit, threshold),
            cycleKey = cycleKey,
            warningThresholdPercent = threshold
        )
    }

    /**
     * Returns a status to notify about, or null when there is nothing new to say.
     *
     * Escalation is one-way within a cycle: crossing the warning threshold notifies once, reaching
     * the cap notifies once more, and neither repeats until the next cycle begins. Without that,
     * a check running every few seconds would notify continuously for the rest of the month.
     */
    suspend fun checkForAlert(): DataLimitStatus? {
        if (!preferencesManager.dataLimitAlert.first()) return null

        val status = status()
        if (status.limitBytes <= 0L) return null

        val lastCycle = preferencesManager.alertCycleKey.first()
        val lastLevel = if (lastCycle == status.cycleKey) {
            preferencesManager.alertLevel.first()
        } else {
            // New cycle: forget what we alerted about in the old one.
            DataLimitLevel.NONE.ordinal
        }

        if (status.level.ordinal <= lastLevel) {
            // Still record the cycle roll-over so the reset happens once, not on every check.
            if (lastCycle != status.cycleKey) {
                preferencesManager.updateAlertState(status.cycleKey, status.level.ordinal)
            }
            return null
        }

        preferencesManager.updateAlertState(status.cycleKey, status.level.ordinal)
        return status
    }

    private suspend fun mobileUsageForCycle(resetDay: Int, bounds: LongRange): Long {
        if (getAccurateUsageUseCase.hasUsageAccess()) {
            getAccurateUsageUseCase.cycleTotal(resetDay)?.let { return it.mobileUsage }
        }
        // Fallback to the sampled rows in Room.
        return getUsageDataUseCase.getUsageInRange(
            UsagePeriods.dayKey(bounds.first),
            UsagePeriods.dayKey(bounds.last)
        ).mobileUsage
    }

    private fun levelFor(used: Long, limit: Long, thresholdPercent: Int): DataLimitLevel {
        if (limit <= 0L) return DataLimitLevel.NONE
        val warningBytes = (limit.toDouble() * thresholdPercent / 100).toLong()
        return when {
            used >= limit -> DataLimitLevel.REACHED
            used >= warningBytes -> DataLimitLevel.WARNING
            else -> DataLimitLevel.NONE
        }
    }
}
