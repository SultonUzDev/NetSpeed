package com.sultonuzdev.netspeed.di


import com.sultonuzdev.netspeed.data.datasource.NetworkStatsDataSource
import com.sultonuzdev.netspeed.data.datasource.SpeedTestRunner
import com.sultonuzdev.netspeed.utils.NetworkDetailsReader
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single { NetworkStatsDataSource(androidContext()) }
    single { SpeedTestRunner() }
    single { NetworkDetailsReader(androidContext()) }
}
