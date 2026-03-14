package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevice(id: String): Flow<Device?>
    suspend fun findByDeviceId(deviceId: String): Device?
    suspend fun upsert(device: Device)
}