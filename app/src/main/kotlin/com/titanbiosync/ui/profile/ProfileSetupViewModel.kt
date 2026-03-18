package com.titanbiosync.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val profileExists: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    init {
        checkExistingProfile()
    }

    private fun checkExistingProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val existing = userRepository.getCurrentProfile()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profileExists = existing?.firstName?.isNotBlank() == true
            )
        }
    }

    fun saveProfile(
        firstName: String,
        lastName: String,
        age: Int?,
        height: Float?,
        sex: String?,
        avatarUri: String?
    ) {
        if (firstName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Il nome è obbligatorio")
            return
        }
        if (lastName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Il cognome è obbligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val existing = userRepository.getCurrentProfile()
                val now = System.currentTimeMillis()
                val user = User(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    externalId = existing?.externalId,
                    email = existing?.email,
                    displayName = "$firstName $lastName".trim(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    age = age,
                    height = height,
                    sex = sex,
                    avatarUri = avatarUri,
                    createdAt = existing?.createdAt ?: now,
                    lastActiveAt = now,
                    updatedAt = now,
                    privacyConsent = existing?.privacyConsent,
                    preferencesJson = existing?.preferencesJson
                )
                userRepository.upsert(user)
                _uiState.value = _uiState.value.copy(isLoading = false, savedSuccessfully = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Errore nel salvataggio"
                )
            }
        }
    }
}
