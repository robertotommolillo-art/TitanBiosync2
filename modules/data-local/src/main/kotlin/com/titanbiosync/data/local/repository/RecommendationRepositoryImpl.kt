package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.RecommendationDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.Recommendation
import com.titanbiosync.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecommendationRepositoryImpl(
    private val dao: RecommendationDao
) : RecommendationRepository {
    override fun observeByUser(userId: String): Flow<List<Recommendation>> =
        dao.observeByUser(userId).map { it.map { ent -> ent.toDomain() } }

    override suspend fun findById(id: String): Recommendation? =
        dao.findById(id)?.toDomain()

    override suspend fun upsert(recommendation: Recommendation) =
        dao.insert(recommendation.toEntity())
}