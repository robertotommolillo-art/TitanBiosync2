package com.titanbiosync.data.local.di

import com.titanbiosync.data.local.dao.*
import com.titanbiosync.data.local.repository.*
import com.titanbiosync.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(dao: UserDao): UserRepository {
        return UserRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideDeviceRepository(dao: DeviceDao): DeviceRepository {
        return DeviceRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSensorRepository(dao: SensorReadingDao): SensorRepository {
        return SensorRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(dao: SessionDao): SessionRepository {
        return SessionRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideHealthMetricsRepository(dao: HealthMetricsDao): HealthMetricsRepository {
        return HealthMetricsRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideRecommendationRepository(dao: RecommendationDao): RecommendationRepository {
        return RecommendationRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideCoachPromptRepository(dao: CoachPromptDao): CoachPromptRepository {
        return CoachPromptRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideMapLocationRepository(dao: MapLocationDao): MapLocationRepository {
        return MapLocationRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideConsentRepository(dao: ConsentDao): ConsentRepository {
        return ConsentRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAppConfigRepository(dao: AppConfigDao): AppConfigRepository {
        return AppConfigRepositoryImpl(dao)
    }
}