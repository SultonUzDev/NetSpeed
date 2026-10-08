package com.sultonuzdev.netspeed.utils

import androidx.annotation.StringRes
import com.sultonuzdev.netspeed.R
import java.text.NumberFormat
import java.util.Locale

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
/**
 * [labelRes] is what the picker shows; [symbol] is what gets appended to a figure.
 *
 * They are separate because only one of them is language: "Auto" is a word and translates, while
 * "MB/s" is a symbol written the same way worldwide. The symbols are declared in strings.xml as
 * translatable="false" for translators' reference, and held here as constants because the
 * formatter that appends them is a stateless object with no Context to resolve against -- and
 * resolving a string that is identical in every locale would buy nothing.
 */
enum class SpeedUnit(
    val prefName: String,
    @param:StringRes val labelRes: Int,
    val symbol: String
) {
    AUTO("auto", R.string.option_auto, "Auto"),
    MBPS("mbps", R.string.unit_megabits_per_second, "Mbps"),
    KBPS("kbps", R.string.unit_kilobits_per_second, "Kbps"),
    MEGABYTES("mb/s", R.string.unit_megabytes_per_second, "MB/s"),
    KILOBYTES("kb/s", R.string.unit_kilobytes_per_second, "KB/s");

    companion object {
        fun fromPrefName(name: String?): SpeedUnit =
            entries.firstOrNull { it.prefName == name?.lowercase() } ?: AUTO
    }
}

/** A speed split into its number and its unit, so callers can lay the two out separately. */
data class FormattedSpeed(val value: String, val unit: String) {
    /**
     * The display form, bidi-isolated so the unit cannot drift to the far side of the number in a
     * right-to-left locale. [value] and [unit] stay raw: the status-bar icon draws them onto a
     * canvas, where isolate marks would only confuse the width measurement.
     */
    override fun toString(): String = NetworkUtils.measurement("$value $unit")
}

object SpeedFormatter {

    private const val BITS_PER_BYTE = 8

    fun format(bytesPerSecond: Double, unit: SpeedUnit): FormattedSpeed {
        val bytes = bytesPerSecond.coerceAtLeast(0.0)
        return when (unit) {
            SpeedUnit.AUTO -> auto(bytes)
            SpeedUnit.MEGABYTES -> FormattedSpeed(
                trim(bytes / (1 shl 20)),
                SpeedUnit.MEGABYTES.symbol
            )

            SpeedUnit.KILOBYTES -> FormattedSpeed(
                trim(bytes / 1024.0),
                SpeedUnit.KILOBYTES.symbol
            )

            SpeedUnit.MBPS -> FormattedSpeed(
                trim(bytes * BITS_PER_BYTE / 1_000_000.0),
                SpeedUnit.MBPS.symbol
            )

            SpeedUnit.KBPS -> FormattedSpeed(
                trim(bytes * BITS_PER_BYTE / 1_000.0),
                SpeedUnit.KBPS.symbol
            )
        }
    }

    /** Scales through byte units so the number stays short and readable. */
    private fun auto(bytes: Double): FormattedSpeed = when {
        bytes >= (1 shl 30) -> FormattedSpeed(trim(bytes / (1 shl 30)), "GB/s")
        bytes >= (1 shl 20) -> FormattedSpeed(trim(bytes / (1 shl 20)), "MB/s")
        bytes >= 1024.0 -> FormattedSpeed(trim(bytes / 1024.0), "KB/s")
        else -> FormattedSpeed(whole().format(bytes), "B/s")
    }

    /**
     * Keeps the number to roughly three significant characters, which is all that fits in a
     * status-bar icon.
     *
     * The trailing zero is dropped by the formatter (minimumFractionDigits = 0) rather than by
     * trimming characters off the end. The old code guarded its trim on `contains('.')`, which is
     * false on a comma-decimal locale -- so German and Russian kept the ",0" that English drops,
     * and the figure needed one more character than the icon has room for.
     *
     * Grouping is off: this figure goes into a status-bar icon 96px wide, where a thousands
     * separator costs a character that the number itself needs.
     */
    private fun trim(value: Double): String =
        (if (value >= 100) whole() else oneDecimal()).format(value)

    private fun oneDecimal(): NumberFormat =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }

    private fun whole(): NumberFormat =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            isGroupingUsed = false
            maximumFractionDigits = 0
        }
}
