package com.titanbiosync.domain.usecase.location

import com.titanbiosync.domain.model.MapLocation
import com.titanbiosync.domain.repository.MapLocationRepository
import java.util.UUID

class RecordLocationUseCase(
    private val mapLocationRepository: MapLocationRepository
) {
    suspend operator fun invoke(params: Params) {
        val mapLocation = MapLocation(
            id = UUID.randomUUID().toString(),
            sessionId = params.sessionId,
            latitude = params.latitude,
            longitude = params.longitude,
            altitude = params.altitude,
            speed = params.speed,
            bearing = params.bearing,
            accuracy = params.accuracy,
            timestamp = params.timestamp
        )

        mapLocationRepository.insert(mapLocation)
    }

    data class Params(
        val sessionId: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val speed: Float?,
        val bearing: Float?,
        val accuracy: Float?,
        val timestamp: Long
    )
}