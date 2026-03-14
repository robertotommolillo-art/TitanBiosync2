package com.titanbiosync.domain.usecase.user

import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.domain.usecase.UseCase
import java.util.UUID

class CreateUserUseCase(
    private val userRepository: UserRepository
) : UseCase<CreateUserUseCase.Params, User> {

    override suspend fun invoke(params: Params): User {
        val user = User(
            id = UUID.randomUUID().toString(),
            email = params.email,
            displayName = params.displayName,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        userRepository.upsert(user)
        return user
    }

    data class Params(
        val email: String,
        val displayName: String
    )
}