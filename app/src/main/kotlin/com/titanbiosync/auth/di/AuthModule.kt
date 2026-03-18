package com.titanbiosync.auth.di

import com.titanbiosync.auth.data.NoOpAuthRepositoryImpl
import com.titanbiosync.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = NoOpAuthRepositoryImpl()
}
