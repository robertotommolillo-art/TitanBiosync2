package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE device_id = :deviceId LIMIT 1")
    suspend fun findByDeviceId(deviceId: String): DeviceEntity?

    @Query("SELECT * FROM devices")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: String)
}