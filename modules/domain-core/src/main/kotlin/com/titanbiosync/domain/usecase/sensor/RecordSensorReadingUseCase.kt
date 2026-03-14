package com.titanbiosync.domain.usecase.sensor

import com.titanbiosync.domain.model.SensorReading
import com.titanbiosync.domain.repository.SensorRepository
import com.titanbiosync.domain.usecase.UseCase
import java.util.UUID
/**
 * Use Case per registrare una lettura da sensore.
 */
class RecordSensorReadingUseCase (
    private val sensorRepository: SensorRepository
) : UseCase<RecordSensorReadingUseCase.Params, Unit> {

    override suspend fun invoke(params: Params) {
        val reading = SensorReading(
            id = UUID.randomUUID().toString(),
            deviceId = params.deviceId,
            timestamp = params.timestamp ?: System.currentTimeMillis(),
            sensorType = params.sensorType,
            value = params.value,
            qualityScore = params.qualityScore,
            sessionId = params.sessionId
        )

        sensorRepository.insert(reading)
    }

    data class Params(
        val deviceId: String?,
        val sensorType: String, // es: "heart_rate", "spo2", "temperature", "accelerometer"
        val value: String,
        val qualityScore: Float? = null,
        val sessionId: String? = null,
        val timestamp: Long? = null
    )
}