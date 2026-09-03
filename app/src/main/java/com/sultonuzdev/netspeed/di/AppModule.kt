package com.sultonuzdev.netspeed.di

import com.sultonuzdev.netspeed.presentation.MainViewModel
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsViewModel
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedViewModel
import com.sultonuzdev.netspeed.presentation.screens.speedtest.SpeedTestViewModel
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { SpeedViewModel(get(), get(), get()) }
    viewModel { UsageViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SpeedTestViewModel(get(), get()) }
}