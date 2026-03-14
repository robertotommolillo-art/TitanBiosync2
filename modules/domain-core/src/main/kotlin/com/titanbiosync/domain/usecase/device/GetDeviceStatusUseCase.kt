package com.titanbiosync.domain.usecase.device

import com.titanbiosync.domain.model.Device
import com.titanbiosync.domain.repository.DeviceRepository

/**
 * Use Case per ottenere lo stato di un device tramite deviceId.
 * Restituisce null se il device non è registrato.
 */
class GetDeviceStatusUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Device? {
        return deviceRepository.findByDeviceId(deviceId)
    }
}