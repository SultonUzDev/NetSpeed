package com.sultonuzdev.netspeed.domain.repository

import com.sultonuzdev.netspeed.domain.models.NetworkInfo
import com.sultonuzdev.netspeed.domain.models.NetworkSpeed
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getNetworkSpeed(): Flow<NetworkSpeed>
    fun getNetworkInfo(): Flow<NetworkInfo>
    suspend fun startMonitoring()
    suspend fun stopMonitoring()
}