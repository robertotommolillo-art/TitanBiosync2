package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.SensorReadingDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.SensorReading
import com.titanbiosync.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SensorRepositoryImpl(
    private val dao: SensorReadingDao
) : SensorRepository {
    override fun observeBySession(sessionId: String): Flow<List<SensorReading>> =
        dao.observeBySession(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun findBetween(fromTs: Long, toTs: Long): List<SensorReading> =
        dao.findBetween(fromTs, toTs).map { it.toDomain() }

    override suspend fun insert(reading: SensorReading) =
        dao.insert(reading.toEntity())

    override suspend fun insertAll(readings: List<SensorReading>) =
        dao.insertAll(readings.map { it.toEntity() })

    override suspend fun deleteByIds(ids: List<String>) =
        dao.deleteByIds(ids)
}