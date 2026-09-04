package com.sultonuzdev.netspeed.di

import androidx.room.Room
import com.sultonuzdev.netspeed.data.database.NetSpeedDatabase
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.utils.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            NetSpeedDatabase::class.java,
            Constants.DB_NAME
        )
            .addMigrations(NetSpeedDatabase.MIGRATION_2_3, NetSpeedDatabase.MIGRATION_3_4)
            .build()
    }

    single { get<NetSpeedDatabase>().usageDao() }
    single { PreferencesManager(get()) }

}