package com.sultonuzdev.netspeed.domain.repository

import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult

interface SpeedTestRepository {
    /** Runs a test and returns it. [onProgress] is called on a background thread. */
    suspend fun runTest(onProgress: (SpeedTestPhase, Double) -> Unit): SpeedTestResult
}
