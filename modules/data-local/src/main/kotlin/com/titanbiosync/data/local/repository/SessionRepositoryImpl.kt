package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.SessionDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(
    private val dao: SessionDao
) : SessionRepository {
    override fun observeById(id: String): Flow<Session?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeByUser(userId: String): Flow<List<Session>> =
        dao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(session: Session) =
        dao.insert(session.toEntity())
}