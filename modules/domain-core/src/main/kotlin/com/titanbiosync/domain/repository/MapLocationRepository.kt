package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.MapLocation
import kotlinx.coroutines.flow.Flow

interface MapLocationRepository {
    fun observeBySession(sessionId: String): Flow<List<MapLocation>>
    suspend fun insert(point: MapLocation)
    suspend fun insertAll(points: List<MapLocation>)
}