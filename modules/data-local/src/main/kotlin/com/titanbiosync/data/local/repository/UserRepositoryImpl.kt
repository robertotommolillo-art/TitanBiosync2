package com.titanbiosync.data.local.repository

import com.titanbiosync.data.local.dao.UserDao
import com.titanbiosync.data.local.mappers.toDomain
import com.titanbiosync.data.local.mappers.toEntity
import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {
    override fun observeUser(id: String): Flow<User?> =
        userDao.observeById(id).map { it?.toDomain() }

    override suspend fun findByEmail(email: String): User? =
        userDao.findByEmail(email)?.toDomain()

    override suspend fun findByExternalId(externalId: String): User? =
        userDao.findByExternalId(externalId)?.toDomain()

    override suspend fun upsert(user: User) =
        userDao.insert(user.toEntity())

    override suspend fun deleteById(id: String) =
        userDao.deleteById(id)
}