package com.sultonuzdev.netspeed.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Window boundaries for usage queries. Everything here works in the device's local timezone, so
 * "today" means the user's day and the billing cycle starts on the user's chosen day of month.
 */
object UsagePeriods {

    /**
     * ISO-8601, and deliberately not a locale-aware formatter.
     *
     * This string is the Room primary key, and the DAO matches it three ways: `= :date`,
     * `BETWEEN :start AND :end` on raw string ordering, and `LIKE :monthYear || '%'`. All three
     * require the same bytes every time, on every device.
     *
     * SimpleDateFormat with Locale.getDefault() did not give that. Measured on the JDK that
     * ships with Android Studio, for today's date:
     *
     *     ar-EG  ->  ٢٠٢٦-١٠-٠٧      (Arabic-Indic digits)
     *     fa-IR  ->  ۲۰۲۶-۱۰-۰۷      (Persian digits)
     *     th-TH  ->  2569-10-07      (Buddhist calendar, year + 543)
     *     de, ru, hi, ur  ->  2026-10-07   (unaffected)
     *
     * On those three locales the key written at midnight never matched the key read a second
     * later, so every write created a new row and no range query ever found them. java.time has
     * no locale or calendar to get wrong: LocalDate is proleptic ISO and
     * [DateTimeFormatter.ISO_LOCAL_DATE] always writes Latin digits.
     */
    private val KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun localDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

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
        localDate(millis).format(KEY_FORMAT)

    /** Inverse of [dayKey]: local midnight for a "yyyy-MM-dd" string, or null if unparseable. */
    fun millisForDayKey(key: String): Long? = try {
        LocalDate.parse(key, KEY_FORMAT)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        null
    }

    /** The [days] calendar days ending with today, oldest first, as (dayKey, bounds). */
    fun lastDays(days: Int, now: Long = System.currentTimeMillis()): List<Pair<String, LongRange>> {
        val cursor = Calendar.getInstance().apply { timeInMillis = now }.atStartOfDay()
        cursor.add(Calendar.DAY_OF_YEAR, -(days - 1))
        return (0 until days).map {
            val startMillis = cursor.timeInMillis
            val key = dayKey(startMillis)
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
