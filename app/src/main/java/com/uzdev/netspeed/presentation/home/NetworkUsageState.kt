package com.uzdev.netspeed.presentation.home

import com.uzdev.netspeed.domain.model.Speed

data class NetworkUsageState(
    val maxDownloadSpeed: Speed = Speed(0),
    val maxUploadSpeed: Speed = Speed(0),
    val maxTotalSpeed: Speed = Speed(0),
    val duration: Long = 0L
)