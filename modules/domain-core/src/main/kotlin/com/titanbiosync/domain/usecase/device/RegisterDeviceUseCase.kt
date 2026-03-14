package com.titanbiosync.domain.usecase.device

import com.titanbiosync.domain.model.Device
import com.titanbiosync.domain.repository.DeviceRepository
import com.titanbiosync.domain.usecase.UseCase
import java.util.UUID
/**
 * Use Case per registrare un nuovo device o aggiornare uno esistente.
 */
class RegisterDeviceUseCase (
    private val deviceRepository: DeviceRepository
) : UseCase<RegisterDeviceUseCase.Params, Device> {

    override suspend fun invoke(params: Params): Device {
        // Verifica se il device esiste già
        val existing = deviceRepository.findByDeviceId(params.deviceId)

        val device = if (existing != null) {
            // Aggiorna device esistente
            existing.copy(
                model = params.model ?: existing.model,
                firmwareVersion = params.firmwareVersion ?: existing.firmwareVersion,
                lastSeenAt = System.currentTimeMillis(),
                status = "connected",
                capabilitiesJson = params.capabilitiesJson ?: existing.capabilitiesJson
            )
        } else {
            // Crea nuovo device
            Device(
                id = UUID.randomUUID().toString(),
                deviceId = params.deviceId,
                model = params.model,
                firmwareVersion = params.firmwareVersion,
                lastSeenAt = System.currentTimeMillis(),
                status = "connected",
                capabilitiesJson = params.capabilitiesJson
            )
        }

        deviceRepository.upsert(device)
        return device
    }

    data class Params(
        val deviceId: String,
        val model: String? = null,
        val firmwareVersion: String? = null,
        val capabilitiesJson: String? = null
    )
}