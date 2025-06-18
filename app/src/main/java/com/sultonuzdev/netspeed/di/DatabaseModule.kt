package com.sultonuzdev.netspeed.di

import androidx.room.Room
import com.sultonuzdev.netspeed.data.database.NetSpeedDatabase
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            NetSpeedDatabase::class.java,
            "net_speed_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<NetSpeedDatabase>().usageDao() }
    single { PreferencesManager(get()) }

}