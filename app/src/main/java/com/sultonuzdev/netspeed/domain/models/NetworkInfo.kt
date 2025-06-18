package com.sultonuzdev.netspeed.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    val isConnected: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val signalStrength: Int = 0,
    val networkName: String = ""
)

enum class NetworkType {
    WIFI, MOBILE, NONE
}