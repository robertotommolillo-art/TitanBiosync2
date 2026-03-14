package com.titanbiosync.di

import android.content.Context
import com.titanbiosync.ble.BleManager
import com.titanbiosync.location.LocationTracker
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
    fun provideBleManager(
        @ApplicationContext context: Context
    ): BleManager {
        return BleManager(context)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        @ApplicationContext context: Context
    ): LocationTracker {
        return LocationTracker(context)
    }
}