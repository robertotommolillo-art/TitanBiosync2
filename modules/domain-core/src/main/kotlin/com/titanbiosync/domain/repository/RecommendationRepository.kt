package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.Recommendation
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun observeByUser(userId: String): Flow<List<Recommendation>>
    suspend fun findById(id: String): Recommendation?
    suspend fun upsert(recommendation: Recommendation)
}