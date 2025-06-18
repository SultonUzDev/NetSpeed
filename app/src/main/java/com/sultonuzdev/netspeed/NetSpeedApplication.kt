package com.sultonuzdev.netspeed

import android.app.Application
import com.sultonuzdev.netspeed.di.appModule
import com.sultonuzdev.netspeed.di.databaseModule
import com.sultonuzdev.netspeed.di.networkModule
import com.sultonuzdev.netspeed.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NetSpeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@NetSpeedApplication)
            modules(
                appModule,
                databaseModule,
                networkModule,
                repositoryModule
            )
        }
    }
}