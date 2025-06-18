package com.sultonuzdev.netspeed.utils


import android.annotation.SuppressLint
import kotlin.math.pow

object NetworkUtils {

    @SuppressLint("DefaultLocale")
    fun formatSpeed(bytesPerSecond: Double): Pair<String, String> {
        return when {
            bytesPerSecond >= 1024.0.pow(3) -> {
                Pair(String.format("%.1f", bytesPerSecond / 1024.0.pow(3)), "GB/s")
            }

            bytesPerSecond >= 1024.0.pow(2) -> {
                Pair(String.format("%.1f", bytesPerSecond / 1024.0.pow(2)), "MB/s")
            }

            bytesPerSecond >= 1024.0 -> {
                Pair(String.format("%.1f", bytesPerSecond / 1024.0), "KB/s")
            }

            else -> {
                Pair(String.format("%.0f", bytesPerSecond), "B/s")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatSpeedImproved(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1024.0.pow(3) -> {
                String.format("%.1f GB/s", bytesPerSecond / 1024.0.pow(3))
            }

            bytesPerSecond >= 1024.0.pow(2) -> {
                String.format("%.1f MB/s", bytesPerSecond / 1024.0.pow(2))
            }

            bytesPerSecond >= 1024.0 -> {
                String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            }

            bytesPerSecond >= 1.0 -> {
                String.format("%.0f B/s", bytesPerSecond)
            }

            else -> {
                "0 B/s"
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatDataUsage(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1024L * 1024L * 1024L -> {
                Pair(String.format("%.2f", bytes.toDouble() / (1024L * 1024L * 1024L)), "GB")
            }

            bytes >= 1024L * 1024L -> {
                Pair(String.format("%.1f", bytes.toDouble() / (1024L * 1024L)), "MB")
            }

            bytes >= 1024L -> {
                Pair(String.format("%.1f", bytes.toDouble() / 1024L), "KB")
            }

            else -> {
                Pair(bytes.toString(), "B")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
