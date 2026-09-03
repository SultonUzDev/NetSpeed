package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult
import com.sultonuzdev.netspeed.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow

class RunSpeedTestUseCase(private val repository: SpeedTestRepository) {

    suspend operator fun invoke(
        onProgress: (SpeedTestPhase, Double) -> Unit
    ): SpeedTestResult = repository.runTest(onProgress)

    fun history(limit: Int = 20): Flow<List<SpeedTestResult>> = repository.recentResults(limit)

    suspend fun clearHistory() = repository.clearHistory()
}
