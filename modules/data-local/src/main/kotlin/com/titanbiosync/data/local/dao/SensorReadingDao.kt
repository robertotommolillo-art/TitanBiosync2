package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.SensorReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE timestamp BETWEEN :fromTs AND :toTs ORDER BY timestamp ASC")
    suspend fun findBetween(fromTs: Long, toTs: Long): List<SensorReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: SensorReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<SensorReadingEntity>)

    @Query("DELETE FROM sensor_readings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM sensor_readings WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}