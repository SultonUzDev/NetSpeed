package com.sultonuzdev.netspeed.utils


import android.annotation.SuppressLint
import kotlin.math.pow

object NetworkUtils {

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
