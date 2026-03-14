package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.AppConfigDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.AppConfig
import com.titanbiosync.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppConfigRepositoryImpl(
    private val dao: AppConfigDao
) : AppConfigRepository {
    override suspend fun getByKey(key: String): AppConfig? =
        dao.getByKey(key)?.toDomain()

    override fun observeAll(): Flow<List<AppConfig>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(config: AppConfig) =
        dao.upsert(config.toEntity())
}