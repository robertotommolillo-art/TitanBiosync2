package com.titanbiosync.gym.online.di

import com.titanbiosync.gym.online.data.FakeOnlineExerciseDataSource
import com.titanbiosync.gym.online.data.OnlineExerciseDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnlineExerciseModule {

    @Provides
    @Singleton
    fun provideOnlineExerciseDataSource(): OnlineExerciseDataSource {
        // oggi stub, domani RetrofitOnlineExerciseDataSource()
        return FakeOnlineExerciseDataSource()
    }
}