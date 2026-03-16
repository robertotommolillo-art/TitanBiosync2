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
import kotlinx.coroutines.flow.SharingStarted
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

    fun addSet(sessionExerciseId: String) {
        viewModelScope.launch {
            val nextIndex = setDao.getMaxSetIndex(sessionExerciseId) + 1
            setDao.insert(
                GymWorkoutSetLogEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = sessionExerciseId,
                    setIndex = nextIndex
                )
            )
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