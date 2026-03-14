package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.MapLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapLocationDao {
    @Query("SELECT * FROM route_points WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<MapLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: MapLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<MapLocationEntity>)

    @Query("DELETE FROM route_points WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}