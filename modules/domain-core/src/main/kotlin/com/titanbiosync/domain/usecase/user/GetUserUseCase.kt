package com.titanbiosync.domain.usecase.user

import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow

class GetUserUseCase(
    private val userRepository: UserRepository
) : FlowUseCase<GetUserUseCase.Params, Flow<User?>> {

    override fun invoke(params: Params): Flow<User?> {
        return userRepository.observeUser(params.userId)
    }

    data class Params(val userId: String)
}