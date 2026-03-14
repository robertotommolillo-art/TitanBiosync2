package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.AppConfig
import kotlinx.coroutines.flow.Flow

interface AppConfigRepository {
    suspend fun getByKey(key: String): AppConfig?
    fun observeAll(): Flow<List<AppConfig>>
    suspend fun upsert(config: AppConfig)
}