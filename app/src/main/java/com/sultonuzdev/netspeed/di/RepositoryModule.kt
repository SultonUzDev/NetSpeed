package com.sultonuzdev.netspeed.di

import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.data.repository.NetworkRepositoryImpl
import com.sultonuzdev.netspeed.data.repository.UsageRepositoryImpl
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.domain.repository.UsageRepository
import com.sultonuzdev.netspeed.domain.usecases.GetNetworkSpeedUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageDataUseCase
import com.sultonuzdev.netspeed.domain.usecases.SaveUsageDataUseCase
import org.koin.dsl.module

val repositoryModule = module {

    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<UsageRepository> { UsageRepositoryImpl(get(), get()) }

    factory { GetNetworkSpeedUseCase(get()) }
    factory { GetUsageDataUseCase(get()) }
    factory { SaveUsageDataUseCase(get()) }
}