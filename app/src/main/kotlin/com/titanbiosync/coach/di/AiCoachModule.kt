package com.titanbiosync.coach.di

import com.titanbiosync.BuildConfig
import com.titanbiosync.coach.data.AiCoachApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiCoachModule {

    @Provides
    @Singleton
    fun provideAiCoachApi(): AiCoachApi = AiCoachApi(
        baseUrl = BuildConfig.COACH_API_URL,
        appToken = BuildConfig.COACH_APP_TOKEN
    )
}
