package com.titanbiosync.gym.ui.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseSetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseDao: ExerciseDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao,
    private val templateExerciseSetDao: WorkoutTemplateExerciseSetDao
) : ViewModel() {

    data class PickedExercise(
        val exerciseId: String,
        val nameIt: String
    )

    private val templateId: String = savedStateHandle["templateId"] ?: ""
    private val query = MutableStateFlow("")

    val exercises = query
        .flatMapLatest { q ->
            if (q.isBlank()) exerciseDao.observeAll()
            else exerciseDao.search(q)
        }
        .asLiveData()

    fun setQuery(q: String) {
        query.value = q
    }

    fun addExerciseConfigured(
        exerciseId: String,
        repsBySet: List<Int>,
        restSeconds: Int?,
        notes: String?,
        supersetGroupId: String? = null,
        supersetOrder: Int? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val alreadyPresent = templateExerciseDao.exists(templateId, exerciseId)
            if (alreadyPresent) {
                onDone()
                return@launch
            }

            val maxPos = templateExerciseDao.getMaxPosition(templateId)
            val nextPos = maxPos + 1

            val cleanedReps = repsBySet.map { it.coerceAtLeast(0) }

            templateExerciseDao.upsertAll(
                listOf(
                    WorkoutTemplateExerciseEntity(
                        templateId = templateId,
                        position = nextPos,
                        exerciseId = exerciseId,
                        targetSets = cleanedReps.size,
                        targetReps = cleanedReps.firstOrNull(),
                        restSeconds = restSeconds,
                        notes = notes,
                        supersetGroupId = supersetGroupId,
                        supersetOrder = supersetOrder
                    )
                )
            )

            templateExerciseSetDao.deleteForExercise(templateId, nextPos)
            templateExerciseSetDao.upsertAll(
                cleanedReps.mapIndexed { index, reps ->
                    WorkoutTemplateExerciseSetEntity(
                        templateId = templateId,
                        position = nextPos,
                        setIndex = index,
                        reps = reps
                    )
                }
            )

            onDone()
        }
    }

    fun addSupersetConfigured(
        firstExerciseId: String,
        secondExerciseId: String,
        repsBySetA: List<Int>,
        repsBySetB: List<Int>,
        restSecondsAfter: Int?,
        notes: String?,
        supersetGroupId: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            // evita duplicati (se vuoi consentire duplicati nelle superserie dimmelo, ma ora seguiamo la regola attuale)
            if (templateExerciseDao.exists(templateId, firstExerciseId) || templateExerciseDao.exists(templateId, secondExerciseId)) {
                onDone()
                return@launch
            }

            val maxPos = templateExerciseDao.getMaxPosition(templateId)
            val posA = maxPos + 1
            val posB = maxPos + 2

            val cleanedA = repsBySetA.map { it.coerceAtLeast(0) }
            val cleanedB = repsBySetB.map { it.coerceAtLeast(0) }

            templateExerciseDao.upsertAll(
                listOf(
                    WorkoutTemplateExerciseEntity(
                        templateId = templateId,
                        position = posA,
                        exerciseId = firstExerciseId,
                        targetSets = cleanedA.size,
                        targetReps = cleanedA.firstOrNull(),
                        restSeconds = null, // no rest between A and B
                        notes = null,
                        supersetGroupId = supersetGroupId,
                        supersetOrder = 0
                    ),
                    WorkoutTemplateExerciseEntity(
                        templateId = templateId,
                        position = posB,
                        exerciseId = secondExerciseId,
                        targetSets = cleanedB.size,
                        targetReps = cleanedB.firstOrNull(),
                        restSeconds = restSecondsAfter, // rest after the whole superset
                        notes = notes,
                        supersetGroupId = supersetGroupId,
                        supersetOrder = 1
                    )
                )
            )

            templateExerciseSetDao.deleteForExercise(templateId, posA)
            templateExerciseSetDao.deleteForExercise(templateId, posB)

            templateExerciseSetDao.upsertAll(
                cleanedA.mapIndexed { idx, reps ->
                    WorkoutTemplateExerciseSetEntity(templateId = templateId, position = posA, setIndex = idx, reps = reps)
                } + cleanedB.mapIndexed { idx, reps ->
                    WorkoutTemplateExerciseSetEntity(templateId = templateId, position = posB, setIndex = idx, reps = reps)
                }
            )

            onDone()
        }
    }
}