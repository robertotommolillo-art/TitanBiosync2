package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.HealthMetricsDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.HealthMetrics
import com.titanbiosync.domain.repository.HealthMetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthMetricsRepositoryImpl(
    private val dao: HealthMetricsDao
) : HealthMetricsRepository {
    override fun observeByUser(userId: String): Flow<List<HealthMetrics>> =
        dao.observeByUser(userId).map { it.map { ent -> ent.toDomain() } }

    override fun observeByUserAndDate(userId: String, date: String): Flow<HealthMetrics?> =
        dao.observeByUserAndDate(userId, date).map { it?.toDomain() }

    override suspend fun upsert(metrics: HealthMetrics) =
        dao.insert(metrics.toEntity())
}