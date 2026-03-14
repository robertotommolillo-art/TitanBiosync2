package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.ConsentDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.ConsentRecord
import com.titanbiosync.domain.repository.ConsentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConsentRepositoryImpl(
    private val dao: ConsentDao
) : ConsentRepository {
    override fun observeByUser(userId: String): Flow<List<ConsentRecord>> =
        dao.observeByUser(userId).map { it.map { ent -> ent.toDomain() } }

    override suspend fun findByUserAndType(userId: String, type: String): ConsentRecord? =
        dao.findByUserAndType(userId, type)?.toDomain()

    override suspend fun upsert(consent: ConsentRecord) =
        dao.insert(consent.toEntity())
}