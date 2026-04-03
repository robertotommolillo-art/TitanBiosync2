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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GymWorkoutSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val setDao: GymWorkoutSetLogDao,
    weightUnitRepository: WeightUnitRepository
) : ViewModel() {

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