package com.uzdev.netspeed.data.network

import android.icu.text.DecimalFormat
import com.uzdev.netspeed.domain.model.Speed

object NetworkSpeedChecker {


    private var totalSpeed: Long = 0L
    private var downSpeed: Long = 0L
    private var upSpeed: Long = 0L


//    private fun getSpeed(s: Long): Speed {
//        var speed = s
//
//        if (isSpeedUnitBits) {
//            speed *= 8
//        }
//
//        if (speed < 1000000) {
//            speedUnit =
//                if (isSpeedUnitBits) "Kb" else "KB"
//            speedValue = (speed / 1000).toString()
//        } else
//            if (speed > 1000000) {
//                speedUnit =
//                    if (isSpeedUnitBits) "Mb" else "MB"
//
//                speedValue = if (speed < 10000000) {
//                    java.lang.String.format(Locale.ENGLISH, "%.1f", speed / 1000000.0)
//                } else if (speed < 100000000) {
//                    (speed / 1000000).toString()
//                } else {
//                    "99+"
//                }
//            } else {
//                speedValue = "-"
//                speedUnit = "-"
//            }
//
//        return Speed(speedValue, speedUnit)
//    }




    fun calculateSpeed(timeTaken: Long, downBytes: Long, upBytes: Long): List<Speed> {
        var totalSpeed: Long = 0
        var downSpeed: Long = 0
        var upSpeed: Long = 0
        val totalBytes = downBytes + upBytes
        if (timeTaken > 0) {
            totalSpeed = totalBytes * 1000 / timeTaken
            downSpeed = downBytes * 1000 / timeTaken
            upSpeed = upBytes * 1000 / timeTaken
        }
        this.totalSpeed = totalSpeed
        this.downSpeed = downSpeed
        this.upSpeed = upSpeed

        return listOf(
            Speed(totalSpeed),
            Speed(downSpeed),
            Speed(upSpeed)
        )
    }
}