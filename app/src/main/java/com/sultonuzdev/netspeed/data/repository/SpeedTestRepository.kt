package com.sultonuzdev.netspeed.data.repository

import com.sultonuzdev.netspeed.data.datasource.SpeedTestRunner
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult

/**
 * Runs a speed test and returns the result.
 *
 * Nothing is persisted: a reading is only meaningful at the moment it was taken, so a log of past
 * runs was not worth a table, a schema version and the writes to keep it.
 */
class SpeedTestRepository(
    private val runner: SpeedTestRunner
) {

    suspend fun runTest(onProgress: (SpeedTestPhase, Double) -> Unit): SpeedTestResult {
        val measurements = runner.run { phase, bytesPerSecond ->
            onProgress(phase, bytesPerSecond)
        }

        return SpeedTestResult(
            downloadBytesPerSecond = measurements.downloadBytesPerSecond,
            uploadBytesPerSecond = measurements.uploadBytesPerSecond,
            pingMillis = measurements.pingMillis,
            jitterMillis = measurements.jitterMillis
        )
    }
}
