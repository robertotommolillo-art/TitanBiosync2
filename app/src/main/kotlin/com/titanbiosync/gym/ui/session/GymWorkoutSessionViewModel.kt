package com.titanbiosync.gym.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import com.titanbiosync.gym.domain.WeightUnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RestTimerState(
    val isRunning: Boolean = false,
    val remainingSec: Int = 0,
    val exerciseName: String = "",
    val setIndex: Int = 0,
    val durationSec: Int = GymWorkoutSessionViewModel.DEFAULT_REST_DURATION_SEC
)

@HiltViewModel
class GymWorkoutSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val setDao: GymWorkoutSetLogDao,
    weightUnitRepository: WeightUnitRepository
) : ViewModel() {

    companion object {
        const val DEFAULT_REST_DURATION_SEC = 90
        const val MAX_REST_DURATION_SEC = 180
        val REST_DURATION_PRESETS = listOf(30, 45, 60, 90, 120, 150, 180)
    }

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    val weightUnit = weightUnitRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        .asLiveData()

    val exercises = sessionExerciseDao.observeForSession(sessionId).asLiveData()

    val startedAt = sessionDao.observeById(sessionId)
        .map { it?.startedAt }
        .asLiveData()

    fun observeSets(sessionExerciseId: String) =
        setDao.observeForSessionExercise(sessionExerciseId).asLiveData()

    private val _scrollToNewSet = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val scrollToNewSet: SharedFlow<String> = _scrollToNewSet.asSharedFlow()

    // --- Rest timer ---

    private var currentRestDurationSec = DEFAULT_REST_DURATION_SEC
    private var restTimerJob: Job? = null
    private val _restTimerState = MutableStateFlow(RestTimerState())
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    /** Call this when a set transitions from incomplete to completed. */
    fun onSetCompleted(exerciseName: String, setIndex: Int) {
        restTimerJob?.cancel()
        val duration = currentRestDurationSec
        _restTimerState.value = RestTimerState(
            isRunning = true,
            remainingSec = duration,
            exerciseName = exerciseName,
            setIndex = setIndex,
            durationSec = duration
        )
        restTimerJob = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _restTimerState.update { it.copy(remainingSec = remaining) }
            }
            // Natural completion (remaining reached 0): always mark as done.
            if (remaining == 0) {
                _restTimerState.update { it.copy(isRunning = false) }
            }
        }
    }

    /** Adjust the remaining time of the currently running timer by [deltaSec] seconds. */
    fun adjustRestTimer(deltaSec: Int) {
        _restTimerState.update { state ->
            if (!state.isRunning) state
            else state.copy(remainingSec = (state.remainingSec + deltaSec).coerceIn(0, MAX_REST_DURATION_SEC))
        }
    }

    /** Stop the running rest timer immediately. */
    fun stopRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _restTimerState.value = RestTimerState()
    }

    /**
     * Update the default rest duration for this session.
     * If a timer is currently running (A1 behaviour), it restarts with the new duration.
     */
    fun setRestDuration(seconds: Int) {
        currentRestDurationSec = seconds.coerceIn(1, MAX_REST_DURATION_SEC)
        val state = _restTimerState.value
        if (state.isRunning) {
            onSetCompleted(state.exerciseName, state.setIndex)
        }
    }

    // --- Set management ---

    fun addSet(sessionExerciseId: String) {
        viewModelScope.launch {
            val lastSet = setDao.getLastSet(sessionExerciseId)
            val nextIndex = (lastSet?.setIndex ?: -1) + 1
            setDao.insert(
                GymWorkoutSetLogEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = sessionExerciseId,
                    setIndex = nextIndex,
                    reps = lastSet?.reps,
                    weightKg = lastSet?.weightKg,
                    completed = false,
                    completedAt = null
                )
            )
            _scrollToNewSet.emit(sessionExerciseId)
        }
    }

    fun updateSet(
        set: GymWorkoutSetLogEntity,
        reps: Int?,
        weightKg: Float?,
        completed: Boolean
    ) {
        viewModelScope.launch {
            val completedAt = if (completed) (set.completedAt ?: System.currentTimeMillis()) else null
            setDao.update(
                set.copy(
                    reps = reps,
                    weightKg = weightKg,
                    completed = completed,
                    completedAt = completedAt
                )
            )
        }
    }

    fun endSession(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionDao.endSession(sessionId, System.currentTimeMillis())
            onDone()
        }
    }

    fun getSessionId(): String = sessionId
}