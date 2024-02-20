package com.uzdev.netspeed.utils

import android.annotation.SuppressLint
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import java.util.Date


fun Long.formatMillsToSecond(): String {

    val s: Float = this / 1000f
    val s1: Float = this % 1000f

    return if (s < 1) {
        "${s}ms"
    } else {
        "${s}s${s1}ms"
    }
}

@SuppressLint("SimpleDateFormat")
fun Long.convertLongToTime(): String {
    val date = Date(this)
    val format = SimpleDateFormat("HH:mm")
    return format.format(date)
}

@SuppressLint("SimpleDateFormat")
fun Long.convertLongToDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy.MM.dd")
    return format.format(date)
}

fun Long.formatSpeed(): String {
    val b = this / 8
    val k = this / 1024.0
    val m = this / 1024.0 / 1024.0
    val g = this / 1024.0 / 1024.0 / 1024.0
    val t = this / 1024.0 / 1024.0 / 1024.0 / 1024.0
    val dec = DecimalFormat("0.00")

    val speed = if (t > 1) {
        dec.format(t)
    } else if (g > 1) {
        dec.format(g)
    } else if (m > 1) {
        dec.format(m)
    } else if (k > 1) {
        dec.format(k)
    } else if (b > 1) {
        dec.format(b)
    } else {
        dec.format(this)
    }
    return speed
}

fun Long.formatSpeedUnitType(): String {
    val b = this / 8
    val k = this / 1024.0
    val m = this / 1024.0 / 1024.0
    val g = this / 1024.0 / 1024.0 / 1024.0
    val t = this / 1024.0 / 1024.0 / 1024.0 / 1024.0

    val unit: String = if (t > 1) {
        ("TB")
    } else if (g > 1) {
        ("GB")
    } else if (m > 1) {
        "MB"
    } else if (k > 1) {
        "Kb"
    } else if (b > 1) {
        "b"
    } else {
        "bit"
    }
    return unit
}