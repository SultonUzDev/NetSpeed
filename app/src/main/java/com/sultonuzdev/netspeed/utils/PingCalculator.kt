package com.sultonuzdev.netspeed.utils


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object PingCalculator {


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
}
