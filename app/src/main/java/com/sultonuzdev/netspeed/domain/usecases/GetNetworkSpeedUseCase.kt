package com.sultonuzdev.netspeed.domain.usecases

import com.sultonuzdev.netspeed.domain.models.NetworkSpeed
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

class GetNetworkSpeedUseCase(private val repository: NetworkRepository) {
    operator fun invoke(): Flow<NetworkSpeed> = repository.getNetworkSpeed()
}