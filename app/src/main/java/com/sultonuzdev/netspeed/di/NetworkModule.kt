package com.sultonuzdev.netspeed.di


import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import org.koin.dsl.module

val networkModule = module {
    factory { SpeedMonitorService() }
}