package com.sultonuzdev.netspeed.utils


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object PingCalculator {

    suspend fun calculatePing(host: String = "8.8.8.8"): String {
        return withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("ping -c 1 $host")
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        // Look for time= in ping output
                        if (it.contains("time=")) {
                            val timeIndex = it.indexOf("time=")
                            val timeSubstring = it.substring(timeIndex + 5)
                            val timeEnd = timeSubstring.indexOf(" ")
                            if (timeEnd > 0) {
                                val pingTime = timeSubstring.substring(0, timeEnd)
                                return@withContext "${pingTime}ms"
                            }
                        }
                    }
                }

                process.waitFor()
                reader.close()

                // Default if ping fails
                "N/A"
            } catch (e: Exception) {
                "N/A"
            }
        }
    }

    /**
     * Latency as the time to open a TCP connection, in milliseconds, or null if it fails.
     *
     * Preferred over [simplePing]: InetAddress.isReachable tries ICMP first, which unprivileged
     * Android apps generally cannot send, and its TCP fallback targets port 7 (echo) which almost
     * nothing listens on -- so it reports unreachable for hosts that are plainly reachable.
     * Connecting to a port that is definitely open measures the same round trip honestly.
     */
    suspend fun tcpLatencyMillis(
        host: String = "8.8.8.8",
        port: Int = 53,
        timeoutMillis: Int = 2000
    ): Int? = withContext(Dispatchers.IO) {
        try {
            java.net.Socket().use { socket ->
                val start = System.nanoTime()
                socket.connect(java.net.InetSocketAddress(host, port), timeoutMillis)
                ((System.nanoTime() - start) / 1_000_000).toInt()
            }
        } catch (e: Exception) {
            null
        }
    }

    // Alternative: Simple ping using InetAddress (less accurate but works)
    suspend fun simplePing(host: String = "8.8.8.8"): String {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val address = java.net.InetAddress.getByName(host)
                val reachable = address.isReachable(3000) // 3 second timeout
                val endTime = System.currentTimeMillis()

                if (reachable) {
                    "${endTime - startTime}ms"
                } else {
                    "N/A"
                }
            } catch (e: Exception) {
                "N/A"
            }
        }
    }
}