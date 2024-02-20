package com.uzdev.netspeed.di

import android.content.Context
import androidx.room.Room
import com.uzdev.netspeed.data.db.NetworkDao
import com.uzdev.netspeed.data.db.NetworkDatabase
import com.uzdev.netspeed.data.network.NetworkUsageManager
import com.uzdev.netspeed.data.preference.NetDataStoreManager
import com.uzdev.netspeed.data.repo.NetworkRepository
import com.uzdev.netspeed.domain.repo.NetworkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NetworkDatabase =
        Room.databaseBuilder(ctx, NetworkDatabase::class.java, "net.db")
            .fallbackToDestructiveMigration().allowMainThreadQueries().build()

    @Provides
    @Singleton
    fun provideNetDao(networkDatabase: NetworkDatabase): NetworkDao = networkDatabase.networkDao

    @Provides
    @Singleton
    fun provideNetworkRepository(networkDao: NetworkDao): NetworkRepository =
        NetworkRepositoryImpl(networkDao = networkDao)

    @Provides
    @Singleton
    fun providesNetworkUsageManager(@ApplicationContext context: Context): NetworkUsageManager =
        NetworkUsageManager(context)


    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): NetDataStoreManager =
        NetDataStoreManager(context)
}