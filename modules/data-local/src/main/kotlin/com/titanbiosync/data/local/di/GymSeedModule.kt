package com.titanbiosync.data.local.di

import android.content.Context
import com.titanbiosync.data.local.AppDatabase
import com.titanbiosync.data.local.gym.seed.GymSeedAssetLoader
import com.titanbiosync.data.local.gym.seed.GymSeedImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GymSeedModule {

    @Provides @Singleton
    fun provideGymSeedAssetLoader(@ApplicationContext context: Context) =
        GymSeedAssetLoader(context)

    @Provides @Singleton
    fun provideGymSeedImporter(db: AppDatabase) =
        GymSeedImporter(db)
}