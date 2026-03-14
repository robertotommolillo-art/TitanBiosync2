package com.titanbiosync.domain.usecase.session

import com.titanbiosync.domain.repository.SessionRepository
import com.titanbiosync.domain.usecase.UseCase
import kotlinx.coroutines.flow.first
/**
 * Use Case per terminare una sessione esistente.
 */
class EndSessionUseCase (
    private val sessionRepository: SessionRepository
) : UseCase<EndSessionUseCase.Params, Unit> {

    override suspend fun invoke(params: Params) {
        val session = sessionRepository.observeById(params.sessionId).first()
            ?: throw IllegalArgumentException("Session ${params.sessionId} not found")

        val updatedSession = session.copy(
            endedAt = System.currentTimeMillis(),
            aggregatedMetricsJson = params.aggregatedMetricsJson,
            annotationsJson = params.annotationsJson
        )

        sessionRepository.upsert(updatedSession)
    }

    data class Params(
        val sessionId: String,
        val aggregatedMetricsJson: String? = null,
        val annotationsJson: String? = null
    )
}