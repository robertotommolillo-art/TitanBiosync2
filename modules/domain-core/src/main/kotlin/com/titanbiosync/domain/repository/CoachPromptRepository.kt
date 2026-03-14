package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.CoachPrompt
import kotlinx.coroutines.flow.Flow

interface CoachPromptRepository {
    fun observeConversation(userId: String): Flow<List<CoachPrompt>>
    suspend fun findRecent(userId: String, limit: Int = 50): List<CoachPrompt>
    suspend fun insert(prompt: CoachPrompt)
}