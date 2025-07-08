package com.sultonuzdev.netspeed.di

import com.sultonuzdev.netspeed.presentation.MainViewModel
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsViewModel
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedViewModel
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { SpeedViewModel(get(), get(), get(), androidApplication()) }
    viewModel { UsageViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}