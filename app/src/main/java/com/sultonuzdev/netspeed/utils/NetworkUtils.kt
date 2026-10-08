package com.sultonuzdev.netspeed.utils

import androidx.core.text.BidiFormatter
import java.text.NumberFormat
import java.util.Locale

object NetworkUtils {

    /**
     * A byte count with its unit, in the reader's number format.
     *
     * The figure is localised, the symbol is not. German and Russian write a comma for the
     * decimal point and a space for thousands, Arabic and Persian use their own digits -- all of
     * which [NumberFormat] handles. "KB" and "GB" stay in Latin script because that is how they
     * are written worldwide; translating them would be wrong rather than merely unnecessary.
     *
     * The formatter is built per call rather than cached because Android 13's per-app language
     * changes the locale without restarting the process.
     */
    fun formatBytes(bytes: Long): String = measurement(
        when {
            bytes < KILOBYTE -> "${integerFormat().format(bytes)} B"
            bytes < MEGABYTE -> "${decimalFormat().format(bytes / KILOBYTE.toDouble())} KB"
            bytes < GIGABYTE -> "${decimalFormat().format(bytes / MEGABYTE.toDouble())} MB"
            else -> "${decimalFormat().format(bytes / GIGABYTE.toDouble())} GB"
        }
    )

    /**
     * Pins a figure and its unit together against bidirectional reordering.
     *
     * "30.1 MB" is a left-to-right run. Rendered inside a right-to-left paragraph without isolate
     * marks it comes out as "MB 30.1" -- confirmed under the ar-XB pseudolocale, where the live
     * speed read "B/s 0" instead of "0 B/s". Wrapping here rather than at each call site because
     * every consumer of this function has the same problem: the Compose screens, the notification,
     * both widgets and the overlay.
     *
     * The marks are zero-width, so this changes nothing in a left-to-right locale.
     */
    fun measurement(text: String): String = BidiFormatter.getInstance().unicodeWrap(text)

    /**
     * One decimal place at most, and none when it would be a trailing zero.
     *
     * minimumFractionDigits of 0 rather than trimming the text afterwards: the old code did
     * `trimEnd('0').trimEnd('.')` guarded by `contains('.')`, which assumes the decimal separator
     * is a full stop. On a German or Russian device the guard is simply false, so nothing was
     * trimmed and a round number read "10,0" where English showed "10".
     */
    private fun decimalFormat(): NumberFormat =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }

    private fun integerFormat(): NumberFormat =
        NumberFormat.getIntegerInstance(Locale.getDefault())

    private const val KILOBYTE = 1024L
    private const val MEGABYTE = KILOBYTE * 1024
    private const val GIGABYTE = MEGABYTE * 1024
}
