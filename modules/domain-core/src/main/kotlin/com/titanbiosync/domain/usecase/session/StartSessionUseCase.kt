package com.titanbiosync.domain.usecase.session

import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.repository.SessionRepository
import com.titanbiosync.domain.usecase.UseCase
import java.util.UUID

/**
 * Use Case per iniziare una nuova sessione di allenamento/monitoraggio.
 */
class StartSessionUseCase (
    private val sessionRepository: SessionRepository
) : UseCase<StartSessionUseCase.Params, Session> {

    override suspend fun invoke(params: Params): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            userId = params.userId,
            type = params.sessionType,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            deviceIdsJson = params.deviceIdsJson,
            aggregatedMetricsJson = null,
            annotationsJson = null
        )

        sessionRepository.upsert(session)
        return session
    }

    data class Params(
        val userId: String,
        val sessionType: String, // es: "running", "cycling", "workout", "sleep"
        val deviceIdsJson: String? = null
    )
}