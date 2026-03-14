package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.DeviceDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.Device
import com.titanbiosync.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeviceRepositoryImpl(
    private val deviceDao: DeviceDao
) : DeviceRepository {
    override fun observeDevice(id: String): Flow<Device?> =
        deviceDao.observeById(id).map { it?.toDomain() }

    override suspend fun findByDeviceId(deviceId: String): Device? =
        deviceDao.findByDeviceId(deviceId)?.toDomain()

    override suspend fun upsert(device: Device) =
        deviceDao.insert(device.toEntity())
}