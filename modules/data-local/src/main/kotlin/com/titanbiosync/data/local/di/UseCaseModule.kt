package com.titanbiosync.data.local.di

import com.titanbiosync.domain.repository.*
import com.titanbiosync.domain.usecase.device.*
import com.titanbiosync.domain.usecase.health.*
import com.titanbiosync.domain.usecase.sensor.*
import com.titanbiosync.domain.usecase.session.*
import com.titanbiosync.domain.usecase.user.*
import com.titanbiosync.domain.usecase.location.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // User Use Cases
    @Provides
    @Singleton
    fun provideGetUserUseCase(userRepository: UserRepository) =
        GetUserUseCase(userRepository)

    @Provides
    @Singleton
    fun provideCreateUserUseCase(userRepository: UserRepository) =
        CreateUserUseCase(userRepository)

    // Device Use Cases
    @Provides
    @Singleton
    fun provideRegisterDeviceUseCase(deviceRepository: DeviceRepository) =
        RegisterDeviceUseCase(deviceRepository)

    @Provides
    @Singleton
    fun provideGetDeviceStatusUseCase(deviceRepository: DeviceRepository) =
        GetDeviceStatusUseCase(deviceRepository)

    // Session Use Cases
    @Provides
    @Singleton
    fun provideStartSessionUseCase(sessionRepository: SessionRepository) =
        StartSessionUseCase(sessionRepository)

    @Provides
    @Singleton
    fun provideEndSessionUseCase(sessionRepository: SessionRepository) =
        EndSessionUseCase(sessionRepository)

    @Provides
    @Singleton
    fun provideGetActiveSessionUseCase(sessionRepository: SessionRepository) =
        GetActiveSessionUseCase(sessionRepository)

    // Location Use Cases
    @Provides
    @Singleton
    fun provideCalculateRouteMetricsUseCase() = CalculateRouteMetricsUseCase()

    @Provides
    @Singleton
    fun provideRecordLocationUseCase(mapLocationRepository: MapLocationRepository) =
        RecordLocationUseCase(mapLocationRepository)

    @Provides
    @Singleton
    fun provideGetSessionLocationsUseCase(mapLocationRepository: MapLocationRepository) =
        GetSessionLocationsUseCase(mapLocationRepository)

    @Provides
    @Singleton
    fun provideGetSessionSummaryUseCase(
        sessionRepository: SessionRepository,
        sensorRepository: SensorRepository,
        mapLocationRepository: MapLocationRepository,
        calculateRouteMetricsUseCase: CalculateRouteMetricsUseCase
    ) = GetSessionSummaryUseCase(
        sessionRepository,
        sensorRepository,
        mapLocationRepository,
        calculateRouteMetricsUseCase
    )

    // Sensor Use Cases
    @Provides
    @Singleton
    fun provideRecordSensorReadingUseCase(sensorRepository: SensorRepository) =
        RecordSensorReadingUseCase(sensorRepository)

    @Provides
    @Singleton
    fun provideGetSessionDataUseCase(sensorRepository: SensorRepository) =
        GetSessionDataUseCase(sensorRepository)

    // Health Use Cases
    @Provides
    @Singleton
    fun provideGetDailyMetricsUseCase(healthMetricsRepository: HealthMetricsRepository) =
        GetDailyMetricsUseCase(healthMetricsRepository)

    @Provides
    @Singleton
    fun provideUpdateHealthMetricsUseCase(healthMetricsRepository: HealthMetricsRepository) =
        UpdateHealthMetricsUseCase(healthMetricsRepository)
}