package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.HealthMetrics
import kotlinx.coroutines.flow.Flow

interface HealthMetricsRepository {
    fun observeByUser(userId: String): Flow<List<HealthMetrics>>
    fun observeByUserAndDate(userId: String, date: String): Flow<HealthMetrics?>
    suspend fun upsert(metrics: HealthMetrics)
}