package com.sultonuzdev.netspeed.utils

import android.annotation.SuppressLint

/**
 * How to express a speed.
 *
 * Bit units and byte units are genuinely different quantities, not relabelings: 1 MB/s is 8 Mbps.
 * Carriers and speed tests quote bits; file managers quote bytes. Offering both means the numbers
 * have to be converted, not just suffixed differently.
 *
 * Byte units use binary steps (1 KB = 1024 B), matching the rest of the app. Bit units use decimal
 * steps (1 Mbps = 1,000,000 bit/s), which is the networking convention.
 */
enum class SpeedUnit(val prefName: String, val label: String) {
    AUTO("auto", "Auto"),
    MBPS("mbps", "Mbps"),
    KBPS("kbps", "Kbps"),
    MEGABYTES("mb/s", "MB/s"),
    KILOBYTES("kb/s", "KB/s");

    companion object {
        fun fromPrefName(name: String?): SpeedUnit =
            entries.firstOrNull { it.prefName == name?.lowercase() } ?: AUTO
    }
}

/** A speed split into its number and its unit, so callers can lay the two out separately. */
data class FormattedSpeed(val value: String, val unit: String) {
    override fun toString(): String = "$value $unit"
}

object SpeedFormatter {

    private const val BITS_PER_BYTE = 8

    @SuppressLint("DefaultLocale")
    fun format(bytesPerSecond: Double, unit: SpeedUnit): FormattedSpeed {
        val bytes = bytesPerSecond.coerceAtLeast(0.0)
        return when (unit) {
            SpeedUnit.AUTO -> auto(bytes)
            SpeedUnit.MEGABYTES -> FormattedSpeed(
                trim(bytes / (1 shl 20)),
                SpeedUnit.MEGABYTES.label
            )

            SpeedUnit.KILOBYTES -> FormattedSpeed(
                trim(bytes / 1024.0),
                SpeedUnit.KILOBYTES.label
            )

            SpeedUnit.MBPS -> FormattedSpeed(
                trim(bytes * BITS_PER_BYTE / 1_000_000.0),
                SpeedUnit.MBPS.label
            )

            SpeedUnit.KBPS -> FormattedSpeed(
                trim(bytes * BITS_PER_BYTE / 1_000.0),
                SpeedUnit.KBPS.label
            )
        }
    }

    /** Scales through byte units so the number stays short and readable. */
    @SuppressLint("DefaultLocale")
    private fun auto(bytes: Double): FormattedSpeed = when {
        bytes >= (1 shl 30) -> FormattedSpeed(trim(bytes / (1 shl 30)), "GB/s")
        bytes >= (1 shl 20) -> FormattedSpeed(trim(bytes / (1 shl 20)), "MB/s")
        bytes >= 1024.0 -> FormattedSpeed(trim(bytes / 1024.0), "KB/s")
        else -> FormattedSpeed(String.format("%.0f", bytes), "B/s")
    }

    /**
     * Keeps the number to roughly three significant characters, which is all that fits in a
     * status-bar icon.
     */
    @SuppressLint("DefaultLocale")
    private fun trim(value: Double): String = when {
        value >= 100 -> String.format("%.0f", value)
        value >= 10 -> String.format("%.1f", value)
        else -> String.format("%.1f", value)
    }.let { text ->
        if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
    }
}
