package com.sultonuzdev.netspeed.domain.repository

import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    /** Most recent results, newest first. */
    fun recentResults(limit: Int = 20): Flow<List<SpeedTestResult>>

    /** Runs a test, saves it, and returns it. [onProgress] is called on a background thread. */
    suspend fun runTest(onProgress: (SpeedTestPhase, Double) -> Unit): SpeedTestResult

    suspend fun clearHistory()
}
