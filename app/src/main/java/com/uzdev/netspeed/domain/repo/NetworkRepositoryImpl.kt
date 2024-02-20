package com.uzdev.netspeed.domain.repo

import com.uzdev.netspeed.data.db.NetworkDao
import com.uzdev.netspeed.data.repo.NetworkRepository
import com.uzdev.netspeed.domain.model.NetworkHistory
import kotlinx.coroutines.flow.Flow

class NetworkRepositoryImpl(private val networkDao: NetworkDao) : NetworkRepository {
    override fun insertNetworkResult(networkHistory: NetworkHistory) {
        networkDao.insertNetworkResult(networkHistory)
    }

    override fun getNetworkTestHistory(): Flow<List<NetworkHistory>> {
        return networkDao.getNetworkTestHistory()
    }

    override fun clearHistory() {
        networkDao.clearHistory()
    }
}