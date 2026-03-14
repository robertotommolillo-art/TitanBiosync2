package com.titanbiosync.domain.usecase.session

import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.repository.SessionRepository
import com.titanbiosync.domain.usecase.UseCase
import kotlinx.coroutines.flow.first
/**
 * Use Case per ottenere la sessione attiva (non terminata) di un utente.
 * Restituisce null se non ci sono sessioni attive.
 */
class GetActiveSessionUseCase (
    private val sessionRepository: SessionRepository
) : UseCase<GetActiveSessionUseCase.Params, Session?> {

    override suspend fun invoke(params: Params): Session? {
        val sessions = sessionRepository.observeByUser(params.userId).first()
        return sessions.firstOrNull { it.endedAt == null }
    }

    data class Params(val userId: String)
}