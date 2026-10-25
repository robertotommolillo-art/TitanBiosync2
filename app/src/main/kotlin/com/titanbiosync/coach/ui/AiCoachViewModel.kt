package com.titanbiosync.coach.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.coach.data.AiCoachException
import com.titanbiosync.coach.data.AiCoachRepository
import com.titanbiosync.coach.domain.ChatMessage
import com.titanbiosync.coach.domain.WorkoutPlan
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.ui.profile.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val repository: AiCoachRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<UiChatMessage>>(emptyList())
    val messages: LiveData<List<UiChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Set when the AI returns one or more structured plans — triggers confirmation dialog. */
    private val _pendingPlans = MutableLiveData<List<WorkoutPlan>?>(null)
    val pendingPlans: LiveData<List<WorkoutPlan>?> = _pendingPlans

    private val _savedTemplateIds = MutableLiveData<List<String>?>(null)
    val savedTemplateIds: LiveData<List<String>?> = _savedTemplateIds

    private val _shouldShowWelcomeDialog = MutableLiveData(false)
    val shouldShowWelcomeDialog: LiveData<Boolean> = _shouldShowWelcomeDialog

    // Keeps the prompt persistence working without a real auth provider.
    private val localUserId: String = "local"

    init {
        loadWelcomeState()
    }

    /** Sends a user text message to the AI Coach. */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = UiChatMessage(role = ROLE_USER, content = text.trim())
        val updated = (_messages.value ?: emptyList()) + userMsg
        _messages.value = updated
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                repository.chat(
                    userId = localUserId,
                    messages = updated.map { ChatMessage(it.role, it.content) }
                )
            }.onSuccess { reply ->
                _messages.value = (_messages.value ?: emptyList()) +
                    UiChatMessage(role = ROLE_ASSISTANT, content = reply)
            }.onFailure { error ->
                _error.value = toUserFriendlyError(error)
            }
            _isLoading.value = false
        }
    }

    /**
     * Requests the AI Coach to generate a workout plan based on [spec].
     * [webSearch] enables online exercise enrichment (default true).
     * On success, populates [pendingPlans] for user confirmation.
     */
    fun generateWorkout(spec: String, webSearch: Boolean = true) {
        if (spec.isBlank()) return

        val webNote = if (webSearch) " 🌐" else ""
        val userMsg = UiChatMessage(
            role = ROLE_USER,
            content = "Genera una scheda: $spec$webNote"
        )
        _messages.value = (_messages.value ?: emptyList()) + userMsg
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                repository.generateWorkout(spec, webSearch)
            }.onSuccess { (message, plans) ->
                _messages.value = (_messages.value ?: emptyList()) +
                    UiChatMessage(role = ROLE_ASSISTANT, content = message)
                _pendingPlans.value = plans
            }.onFailure { error ->
                _error.value = toUserFriendlyError(error)
            }
            _isLoading.value = false
        }
    }

    /** Confirms and saves the pending workout plan to the Gym. */
    fun confirmSavePlan() {
        val plans = _pendingPlans.value ?: return
        _pendingPlans.value = null
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                repository.saveWorkoutPlansToGym(plans)
            }.onSuccess { templateIds ->
                _savedTemplateIds.value = templateIds
                val savedCount = templateIds.size
                val message = if (savedCount > 1) {
                    "✅ Programma salvato: $savedCount schede nella cartella \"${AiCoachRepository.AI_FOLDER_NAME}\"!"
                } else {
                    "✅ Scheda \"${plans.firstOrNull()?.title.orEmpty()}\" salvata nella cartella \"${AiCoachRepository.AI_FOLDER_NAME}\"!"
                }
                _messages.value = (_messages.value ?: emptyList()) + UiChatMessage(
                    role = ROLE_ASSISTANT,
                    content = message
                )
            }.onFailure { error ->
                _error.value = toUserFriendlyError(error)
            }
            _isLoading.value = false
        }
    }

    /** Discards the pending plan without saving. */
    fun discardPlan() {
        _pendingPlans.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSavedTemplateIds() {
        _savedTemplateIds.value = null
    }

    fun dismissWelcomeDialog(showAgainNextTime: Boolean) {
        _shouldShowWelcomeDialog.value = false
        if (!showAgainNextTime) {
            markWelcomeDialogSeen()
        }
    }

    private fun loadWelcomeState() {
        viewModelScope.launch {
            val prefs = UserPreferences.fromJson(userRepository.getCurrentProfile()?.preferencesJson)
            _shouldShowWelcomeDialog.value = !prefs.aiCoachIntroShown
        }
    }

    private fun markWelcomeDialogSeen() {
        viewModelScope.launch {
            val user = userRepository.getCurrentProfile() ?: return@launch
            val prefs = UserPreferences.fromJson(user.preferencesJson)
            if (prefs.aiCoachIntroShown) return@launch
            userRepository.upsert(
                user.copy(
                    preferencesJson = UserPreferences.toJson(
                        prefs.copy(aiCoachIntroShown = true)
                    )
                )
            )
        }
    }

    private fun toUserFriendlyError(error: Throwable): String {
        return when (error) {
            is AiCoachException -> {
                when (error.statusCode) {
                    400 -> "Richiesta non valida. Riprova descrivendo meglio la scheda."
                    401 -> "Accesso al Coach non autorizzato. Verifica la configurazione del token."
                    429 -> "Hai raggiunto il limite di richieste. Riprova tra qualche minuto."
                    500, 502, 503 -> "Il servizio Coach e' temporaneamente non disponibile. Riprova tra poco."
                    else -> "Il Coach ha restituito un errore (${error.statusCode}). Riprova."
                }
            }
            is IOException -> "Problema di rete. Controlla la connessione e riprova."
            else -> error.message ?: "Si e' verificato un errore imprevisto."
        }
    }

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val AI_FOLDER_NAME = AiCoachRepository.AI_FOLDER_NAME
    }
}

/** Display model for a single chat message. */
data class UiChatMessage(
    val role: String,
    val content: String
)
