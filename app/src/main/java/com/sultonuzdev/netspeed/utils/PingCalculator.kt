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