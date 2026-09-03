package com.sultonuzdev.netspeed.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sultonuzdev.netspeed.data.database.dao.SpeedTestDao
import com.sultonuzdev.netspeed.data.database.entities.SpeedTestEntity
import com.sultonuzdev.netspeed.data.datasource.SpeedTestRunner
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.models.SpeedTestResult
import com.sultonuzdev.netspeed.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpeedTestRepositoryImpl(
    private val runner: SpeedTestRunner,
    private val speedTestDao: SpeedTestDao,
    private val context: Context
) : SpeedTestRepository {

    override fun recentResults(limit: Int): Flow<List<SpeedTestResult>> =
        speedTestDao.getRecent(limit).map { entities ->
            entities.map { entity ->
                SpeedTestResult(
                    id = entity.id,
                    downloadBytesPerSecond = entity.downloadBps,
                    uploadBytesPerSecond = entity.uploadBps,
                    pingMillis = entity.pingMillis,
                    jitterMillis = entity.jitterMillis,
                    timestamp = entity.timestamp,
                    networkType = entity.networkType
                )
            }
        }

    override suspend fun runTest(onProgress: (SpeedTestPhase, Double) -> Unit): SpeedTestResult {
        val measurements = runner.run { phase, bytesPerSecond ->
            onProgress(phase, bytesPerSecond)
        }

        // Recorded so a slow result can later be attributed to the connection it was taken on.
        val entity = SpeedTestEntity(
            downloadBps = measurements.downloadBytesPerSecond,
            uploadBps = measurements.uploadBytesPerSecond,
            pingMillis = measurements.pingMillis,
            jitterMillis = measurements.jitterMillis,
            networkType = currentNetworkLabel()
        )
        val id = speedTestDao.insert(entity)

        return SpeedTestResult(
            id = id,
            downloadBytesPerSecond = entity.downloadBps,
            uploadBytesPerSecond = entity.uploadBps,
            pingMillis = entity.pingMillis,
            jitterMillis = entity.jitterMillis,
            timestamp = entity.timestamp,
            networkType = entity.networkType
        )
    }

    override suspend fun clearHistory() = speedTestDao.deleteAll()

    private fun currentNetworkLabel(): String {
        return try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            when {
                capabilities == null -> "Unknown"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
