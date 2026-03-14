package com.titanbiosync.domain.usecase.sensor

import com.titanbiosync.domain.model.SensorReading
import com.titanbiosync.domain.repository.SensorRepository
import com.titanbiosync.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
/**
 * Use Case per osservare tutti i dati sensori di una sessione in tempo reale.
 */
class GetSessionDataUseCase (
    private val sensorRepository: SensorRepository
) : FlowUseCase<GetSessionDataUseCase.Params, Flow<List<SensorReading>>> {

    override fun invoke(params: Params): Flow<List<SensorReading>> {
        return sensorRepository.observeBySession(params.sessionId)
    }

    data class Params(val sessionId: String)
}