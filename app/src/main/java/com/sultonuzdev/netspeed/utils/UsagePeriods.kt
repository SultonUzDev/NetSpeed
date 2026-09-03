package com.sultonuzdev.netspeed.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Window boundaries for usage queries. Everything here works in the device's local timezone, so
 * "today" means the user's day and the billing cycle starts on the user's chosen day of month.
 */
object UsagePeriods {

    private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun Calendar.atStartOfDay(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** [start, end) for the calendar day containing [millis]. */
    fun dayBounds(millis: Long = System.currentTimeMillis()): LongRange {
        val start = Calendar.getInstance().apply { timeInMillis = millis }.atStartOfDay()
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return start.timeInMillis until end.timeInMillis
    }

    /** Local date key ("yyyy-MM-dd") for [millis]; matches the Room primary key format. */
    fun dayKey(millis: Long = System.currentTimeMillis()): String =
        dayFormat().format(Date(millis))

    /** Inverse of [dayKey]: local midnight for a "yyyy-MM-dd" string, or null if unparseable. */
    fun millisForDayKey(key: String): Long? = try {
        dayFormat().parse(key)?.time
    } catch (e: Exception) {
        null
    }

    /** The [days] calendar days ending with today, oldest first, as (dayKey, bounds). */
    fun lastDays(days: Int, now: Long = System.currentTimeMillis()): List<Pair<String, LongRange>> {
        val cursor = Calendar.getInstance().apply { timeInMillis = now }.atStartOfDay()
        cursor.add(Calendar.DAY_OF_YEAR, -(days - 1))
        return (0 until days).map {
            val startMillis = cursor.timeInMillis
            val key = dayFormat().format(Date(startMillis))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            key to (startMillis until cursor.timeInMillis)
        }
    }

    /** Number of days in the billing cycle containing [now]; used to derive a daily budget. */
    fun daysInCycle(resetDayOfMonth: Int, now: Long = System.currentTimeMillis()): Int {
        val bounds = billingCycleBounds(resetDayOfMonth, now)
        val days = ((bounds.last + 1 - bounds.first) / MILLIS_PER_DAY).toInt()
        return days.coerceAtLeast(1)
    }

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * [start, end) for the billing cycle containing [now], where the cycle turns over on
     * [resetDayOfMonth]. Months shorter than the chosen day clamp to their last day, so a cycle
     * starting on the 31st still behaves sanely in February.
     */
    fun billingCycleBounds(
        resetDayOfMonth: Int,
        now: Long = System.currentTimeMillis()
    ): LongRange {
        val day = resetDayOfMonth.coerceIn(1, 28)
        val start = Calendar.getInstance().apply { timeInMillis = now }.atStartOfDay()
        if (start.get(Calendar.DAY_OF_MONTH) < day) {
            start.add(Calendar.MONTH, -1)
        }
        start.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(start.getActualMaximum(Calendar.DAY_OF_MONTH)))

        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        return start.timeInMillis until end.timeInMillis
    }
}
