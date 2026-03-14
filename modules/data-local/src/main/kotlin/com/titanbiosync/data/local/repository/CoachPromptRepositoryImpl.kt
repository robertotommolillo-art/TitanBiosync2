package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.CoachPromptDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.CoachPrompt
import com.titanbiosync.domain.repository.CoachPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CoachPromptRepositoryImpl(
    private val dao: CoachPromptDao
) : CoachPromptRepository {
    override fun observeConversation(userId: String): Flow<List<CoachPrompt>> =
        dao.observeConversation(userId).map { it.map { ent -> ent.toDomain() } }

    override suspend fun findRecent(userId: String, limit: Int): List<CoachPrompt> =
        dao.findRecent(userId, limit).map { it.toDomain() }

    override suspend fun insert(prompt: CoachPrompt) =
        dao.insert(prompt.toEntity())
}