package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun observeBySession(sessionId: String): Flow<List<SensorReading>>
    suspend fun findBetween(fromTs: Long, toTs: Long): List<SensorReading>
    suspend fun insert(reading: SensorReading)
    suspend fun insertAll(readings: List<SensorReading>)
    suspend fun deleteByIds(ids: List<String>)
}