package com.titanbiosync.gym.ui.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.room.withTransaction
import com.titanbiosync.data.local.AppDatabase
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseSetEntity
import com.titanbiosync.gym.domain.CreateGymWorkoutSessionFromTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutTemplateDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val db: AppDatabase,
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao,
    private val templateExerciseSetDao: WorkoutTemplateExerciseSetDao,
    private val createGymWorkoutSessionFromTemplate: CreateGymWorkoutSessionFromTemplate
) : ViewModel() {

    private val templateId: String = savedStateHandle["templateId"] ?: ""

    val template = templateDao.observeById(templateId).asLiveData()
    val rows = templateExerciseDao.observeRows(templateId).asLiveData()

    fun getTemplateId(): String = templateId

    fun removeExerciseAt(position: Int, supersetGroupId: String?) {
        viewModelScope.launch {
            db.withTransaction {
                val currentExercises = templateExerciseDao.getAllForTemplateOnce(templateId)
                val currentSets = templateExerciseSetDao.getAllForTemplateOnce(templateId)

                val removedPositions = if (supersetGroupId.isNullOrBlank()) {
                    setOf(position)
                } else {
                    currentExercises
                        .filter { it.supersetGroupId == supersetGroupId }
                        .map { it.position }
                        .toSet()
                }

                val remainingExercisesOld = currentExercises
                    .filterNot { it.position in removedPositions }
                    .sortedBy { it.position }

                // map oldPos -> newPos
                val posMap = remainingExercisesOld
                    .mapIndexed { newIndex, ex -> ex.position to newIndex }
                    .toMap()

                val remainingExercisesNew = remainingExercisesOld.mapIndexed { newIndex, ex ->
                    ex.copy(position = newIndex)
                }

                // riscrivo esercizi
                templateExerciseDao.deleteAllForTemplate(templateId)
                if (remainingExercisesNew.isNotEmpty()) {
                    templateExerciseDao.upsertAll(remainingExercisesNew)
                }

                // riscrivo set: tengo solo set per esercizi rimasti e rimappo la position
                val remainingSetsNew = currentSets
                    .filter { it.position in posMap.keys }
                    .map { s ->
                        s.copy(position = posMap.getValue(s.position))
                    }
                    .sortedWith(
                        compareBy<WorkoutTemplateExerciseSetEntity> { it.position }
                            .thenBy { it.setIndex }
                    )

                templateExerciseSetDao.deleteAllForTemplate(templateId)
                if (remainingSetsNew.isNotEmpty()) {
                    templateExerciseSetDao.upsertAll(remainingSetsNew)
                }
            }
        }
    }

    fun renameTemplate(newName: String) {
        val name = newName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val current = template.value ?: return@launch
            templateDao.upsert(
                current.copy(
                    name = name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTemplate(onDone: () -> Unit) {
        viewModelScope.launch {
            db.withTransaction {
                templateExerciseSetDao.deleteAllForTemplate(templateId)
                templateExerciseDao.deleteAllForTemplate(templateId)
                templateDao.deleteById(templateId)
            }
            onDone()
        }
    }

    fun startWorkout(onCreated: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val sessionId = createGymWorkoutSessionFromTemplate(templateId)
            onCreated(sessionId)
        }
    }
}