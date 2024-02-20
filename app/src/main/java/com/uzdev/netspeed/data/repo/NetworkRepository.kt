package com.uzdev.netspeed.data.repo

import com.uzdev.netspeed.domain.model.NetworkHistory
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun insertNetworkResult(networkHistory: NetworkHistory)


    fun getNetworkTestHistory(): Flow<List<NetworkHistory>>

    fun clearHistory()
}