package com.sultonuzdev.netspeed.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date())
}

fun getCurrentMonth(): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return formatter.format(Date())
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}