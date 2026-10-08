package com.sultonuzdev.netspeed.di

import com.sultonuzdev.netspeed.presentation.MainViewModel
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsViewModel
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedViewModel
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageViewModel
import com.sultonuzdev.netspeed.utils.AndroidStringProvider
import com.sultonuzdev.netspeed.utils.StringProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // The one way out of composition to a string resource. Everything that needs text
    // without a Composable scope takes this rather than a raw Context.
    single<StringProvider> { AndroidStringProvider(androidContext()) }

    viewModel { MainViewModel(get()) }
    viewModel { SpeedViewModel(get(), get(), get(), get(), get()) }
    viewModel { UsageViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}