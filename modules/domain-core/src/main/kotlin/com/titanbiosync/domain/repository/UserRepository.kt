package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(id: String): Flow<User?>
    suspend fun findByEmail(email: String): User?
    suspend fun upsert(user: User)
    suspend fun deleteById(id: String)
}