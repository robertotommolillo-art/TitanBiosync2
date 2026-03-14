package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeById(id: String): Flow<Session?>
    fun observeByUser(userId: String): Flow<List<Session>>
    suspend fun upsert(session: Session)
}