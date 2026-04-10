package com.titanbiosync.coach.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.coach.data.AiCoachRepository
import com.titanbiosync.coach.domain.ChatMessage
import com.titanbiosync.coach.domain.WorkoutPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val repository: AiCoachRepository
) : ViewModel() {

    // ── Chat messages ──────────────────────────────────────────────────────

    private val _messages = MutableLiveData<List<UiChatMessage>>(emptyList())
    val messages: LiveData<List<UiChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // ── Generate workflow ──────────────────────────────────────────────────

    /** Set when the AI returns a structured plan — triggers confirmation dialog. */
    private val _pendingPlan = MutableLiveData<WorkoutPlan?>(null)
    val pendingPlan: LiveData<WorkoutPlan?> = _pendingPlan

    private val _savedTemplateId = MutableLiveData<String?>(null)
    val savedTemplateId: LiveData<String?> = _savedTemplateId

    // ── Local user ID (local-first; no Firebase required) ─────────────────
    // Keeps the prompt persistence working without a real auth provider.
    private val localUserId: String = "local"

    // ── Actions ────────────────────────────────────────────────────────────

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
            }.onFailure { e ->
                _error.value = e.message ?: "Errore durante la comunicazione con il coach."
            }
            _isLoading.value = false
        }
    }

    /**
     * Requests the AI Coach to generate a workout plan based on [spec].
     * On success, populates [pendingPlan] for user confirmation.
     */
    fun generateWorkout(spec: String) {
        if (spec.isBlank()) return

        val userMsg = UiChatMessage(
            role = ROLE_USER,
            content = "Genera una scheda: $spec"
        )
        _messages.value = (_messages.value ?: emptyList()) + userMsg
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                repository.generateWorkout(spec)
            }.onSuccess { (message, plan) ->
                _messages.value = (_messages.value ?: emptyList()) +
                        UiChatMessage(role = ROLE_ASSISTANT, content = message)
                _pendingPlan.value = plan
            }.onFailure { e ->
                _error.value = e.message ?: "Errore nella generazione della scheda."
            }
            _isLoading.value = false
        }
    }

    /** Confirms and saves the pending workout plan to the Gym. */
    fun confirmSavePlan() {
        val plan = _pendingPlan.value ?: return
        _pendingPlan.value = null
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                repository.saveWorkoutPlanToGym(plan)
            }.onSuccess { templateId ->
                _savedTemplateId.value = templateId
                _messages.value = (_messages.value ?: emptyList()) + UiChatMessage(
                    role = ROLE_ASSISTANT,
                    content = "✅ Scheda \"${plan.title}\" salvata nella cartella \"${AiCoachRepository.AI_FOLDER_NAME}\"!"
                )
            }.onFailure { e ->
                _error.value = e.message ?: "Errore nel salvataggio della scheda."
            }
            _isLoading.value = false
        }
    }

    /** Discards the pending plan without saving. */
    fun discardPlan() {
        _pendingPlan.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSavedTemplateId() {
        _savedTemplateId.value = null
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
