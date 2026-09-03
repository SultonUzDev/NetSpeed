package com.sultonuzdev.netspeed.data.datasource

import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.system.measureTimeMillis

/**
 * Measures throughput and latency against Cloudflare's public speed-test endpoints.
 *
 * Uses [HttpURLConnection] rather than pulling in an HTTP client: the whole job is "stream bytes
 * and time them", and a dependency would buy nothing here.
 *
 * Note this is the one place the app talks to a third-party server. Nothing about the user is
 * sent — the endpoints take a byte count and return or discard that many bytes — but the request
 * necessarily reveals the device's IP to Cloudflare, as any speed test must.
 */
class SpeedTestRunner {

    /** Progress callback: phase, and the live speed in bytes/sec for the gauge. */
    fun interface ProgressListener {
        fun onProgress(phase: SpeedTestPhase, bytesPerSecond: Double)
    }

    data class Measurements(
        val downloadBytesPerSecond: Double,
        val uploadBytesPerSecond: Double,
        val pingMillis: Int,
        val jitterMillis: Int
    )

    suspend fun run(listener: ProgressListener): Measurements = withContext(Dispatchers.IO) {
        listener.onProgress(SpeedTestPhase.PINGING, 0.0)
        val latencies = measureLatencies()
        val ping = latencies.minOrNull()?.toInt() ?: 0
        val jitter = jitterOf(latencies)

        listener.onProgress(SpeedTestPhase.DOWNLOADING, 0.0)
        val download = measureDownload(listener)

        listener.onProgress(SpeedTestPhase.UPLOADING, 0.0)
        val upload = measureUpload(listener)

        Measurements(
            downloadBytesPerSecond = download,
            uploadBytesPerSecond = upload,
            pingMillis = ping,
            jitterMillis = jitter
        )
    }

    /**
     * Round-trip times for a series of tiny requests.
     *
     * The reported ping is the *minimum*, not the mean: the fastest round trip is the one least
     * polluted by scheduling and queueing, so it is the closest estimate of the real path latency.
     */
    private suspend fun measureLatencies(): List<Long> {
        val samples = mutableListOf<Long>()
        repeat(PING_SAMPLES) {
            coroutineContext.ensureActive()
            try {
                val elapsed = measureTimeMillis {
                    openConnection("$BASE_URL/__down?bytes=0").apply {
                        requestMethod = "GET"
                        connect()
                        inputStream.use { it.readBytes() }
                        disconnect()
                    }
                }
                samples += elapsed
            } catch (e: IOException) {
                // A dropped sample is not a failed test; the remaining ones still characterise it.
            }
        }
        return samples
    }

    /** Mean absolute difference between consecutive samples — the usual jitter definition. */
    private fun jitterOf(samples: List<Long>): Int {
        if (samples.size < 2) return 0
        val deltas = samples.zipWithNext { a, b -> abs(b - a) }
        return (deltas.sum() / deltas.size).toInt()
    }

    private suspend fun measureDownload(listener: ProgressListener): Double {
        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection("$BASE_URL/__down?bytes=$DOWNLOAD_BYTES")
            connection.connect()

            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0L
            // Timing starts at the first byte so connection setup is not charged as transfer time.
            var startedAt = 0L

            connection.inputStream.use { stream ->
                while (true) {
                    coroutineContext.ensureActive()
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    if (startedAt == 0L) startedAt = System.nanoTime()
                    total += read

                    val elapsed = elapsedSeconds(startedAt)
                    if (elapsed > 0) {
                        listener.onProgress(SpeedTestPhase.DOWNLOADING, total / elapsed)
                        if (elapsed >= MAX_PHASE_SECONDS) break
                    }
                }
            }

            val elapsed = elapsedSeconds(startedAt)
            if (elapsed <= 0) 0.0 else total / elapsed
        } catch (e: Exception) {
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun measureUpload(listener: ProgressListener): Double {
        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection("$BASE_URL/__up").apply {
                requestMethod = "POST"
                doOutput = true
                setChunkedStreamingMode(BUFFER_SIZE)
                setRequestProperty("Content-Type", "application/octet-stream")
            }

            val chunk = ByteArray(BUFFER_SIZE) { it.toByte() }
            var total = 0L
            val startedAt = System.nanoTime()

            connection.outputStream.use { stream ->
                while (true) {
                    coroutineContext.ensureActive()
                    stream.write(chunk)
                    total += chunk.size

                    val elapsed = elapsedSeconds(startedAt)
                    if (elapsed > 0) {
                        listener.onProgress(SpeedTestPhase.UPLOADING, total / elapsed)
                    }
                    if (total >= UPLOAD_BYTES || elapsed >= MAX_PHASE_SECONDS) break
                }
            }

            // Reading the response is what confirms the bytes were actually accepted.
            connection.responseCode

            val elapsed = elapsedSeconds(startedAt)
            if (elapsed <= 0) 0.0 else total / elapsed
        } catch (e: Exception) {
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun elapsedSeconds(startNanos: Long): Double =
        if (startNanos == 0L) 0.0 else (System.nanoTime() - startNanos) / 1_000_000_000.0

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            // A cached response would measure the disk, not the network.
            setRequestProperty("Cache-Control", "no-cache, no-store")
        }

    private companion object {
        const val BASE_URL = "https://speed.cloudflare.com"
        const val DOWNLOAD_BYTES = 25_000_000
        const val UPLOAD_BYTES = 5_000_000L
        const val BUFFER_SIZE = 32 * 1024
        const val PING_SAMPLES = 6
        const val MAX_PHASE_SECONDS = 10.0
        const val TIMEOUT_MS = 15_000
    }
}
