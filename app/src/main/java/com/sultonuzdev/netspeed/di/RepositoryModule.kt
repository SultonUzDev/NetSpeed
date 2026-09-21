package com.sultonuzdev.netspeed.di

import com.sultonuzdev.netspeed.data.repository.NetworkRepository
import com.sultonuzdev.netspeed.data.repository.NetworkStatsRepository
import com.sultonuzdev.netspeed.data.repository.SpeedTestRepository
import com.sultonuzdev.netspeed.data.repository.UsageRepository
import com.sultonuzdev.netspeed.domain.usecases.CheckAlertsUseCase
import com.sultonuzdev.netspeed.domain.usecases.CheckDataLimitUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageDataUseCase
import com.sultonuzdev.netspeed.domain.usecases.GetUsageForecastUseCase
import org.koin.dsl.module

val repositoryModule = module {
    single { NetworkRepository(get()) }
    single { UsageRepository(get()) }
    single { NetworkStatsRepository(get()) }
    single { SpeedTestRepository(get()) }

    factory { GetUsageDataUseCase(get()) }
    factory { CheckDataLimitUseCase(get(), get(), get()) }
    factory { CheckAlertsUseCase(get(), get(), get()) }
    factory { GetUsageForecastUseCase(get(), get(), get()) }
}
