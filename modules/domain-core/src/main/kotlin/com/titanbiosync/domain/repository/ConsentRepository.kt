package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.ConsentRecord
import kotlinx.coroutines.flow.Flow

interface ConsentRepository {
    fun observeByUser(userId: String): Flow<List<ConsentRecord>>
    suspend fun findByUserAndType(userId: String, type: String): ConsentRecord?
    suspend fun upsert(consent: ConsentRecord)
}