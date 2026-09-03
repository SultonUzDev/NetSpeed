package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.UsageRepository


class SaveUsageDataUseCase(private val repository: UsageRepository) {
    suspend operator fun invoke(usageData: UsageData) {
        repository.saveUsageData(usageData)
    }

    suspend fun updateUsage(usageData: UsageData) {
        repository.updateUsageData(usageData)
    }

    /** Accumulating write used by the monitoring service; see [UsageRepository.addUsageDelta]. */
    suspend fun addDelta(
        date: String,
        wifiDelta: Long,
        mobileDelta: Long,
        sessionDelta: Long
    ) {
        repository.addUsageDelta(date, wifiDelta, mobileDelta, sessionDelta)
    }
}