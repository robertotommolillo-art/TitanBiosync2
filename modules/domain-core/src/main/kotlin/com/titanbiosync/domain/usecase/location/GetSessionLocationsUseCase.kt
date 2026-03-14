package com.titanbiosync.domain.usecase.location

import com.titanbiosync.domain.model.MapLocation
import com.titanbiosync.domain.repository.MapLocationRepository
import kotlinx.coroutines.flow.Flow

class GetSessionLocationsUseCase(
    private val mapLocationRepository: MapLocationRepository
) {
    operator fun invoke(sessionId: String): Flow<List<MapLocation>> {
        return mapLocationRepository.observeBySession(sessionId)
    }
}