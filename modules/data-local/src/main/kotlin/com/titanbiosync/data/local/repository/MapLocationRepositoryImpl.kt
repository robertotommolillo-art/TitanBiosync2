package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.MapLocationDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.MapLocation
import com.titanbiosync.domain.repository.MapLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MapLocationRepositoryImpl(
    private val dao: MapLocationDao
) : MapLocationRepository {
    override fun observeBySession(sessionId: String): Flow<List<MapLocation>> =
        dao.observeBySession(sessionId).map { it.map { ent -> ent.toDomain() } }

    override suspend fun insert(point: MapLocation) =
        dao.insert(point.toEntity())

    override suspend fun insertAll(points: List<MapLocation>) =
        dao.insertAll(points.map { it.toEntity() })
}