package com.titanbiosync.auth.di

import com.google.firebase.auth.FirebaseAuth
import com.titanbiosync.auth.data.FirebaseAuthRepositoryImpl
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository =
        FirebaseAuthRepositoryImpl(firebaseAuth)
}
