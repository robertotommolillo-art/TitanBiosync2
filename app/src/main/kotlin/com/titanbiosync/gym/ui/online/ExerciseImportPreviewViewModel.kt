package com.titanbiosync.gym.ui.online

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.ExerciseMuscleDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity
import com.titanbiosync.gym.online.data.OnlineExerciseDataSource
import com.titanbiosync.gym.online.model.OnlineExerciseResolved
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExerciseImportPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSource: OnlineExerciseDataSource,
    private val exerciseDao: ExerciseDao,
    private val muscleDao: MuscleDao,
    private val exerciseMuscleDao: ExerciseMuscleDao
) : ViewModel() {

    private val candidateId: String = savedStateHandle["candidateId"] ?: ""
    private val candidateTitle: String = savedStateHandle["candidateTitle"] ?: ""

    private val _resolved = MutableStateFlow<OnlineExerciseResolved?>(null)
    val resolved = _resolved.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _resolved.value = dataSource.resolve(candidateId)
        }
    }

    private fun computeDisplayNameIt(r: OnlineExerciseResolved): String {
        val resolvedName = r.nameIt?.trim().orEmpty()
        val title = candidateTitle.trim()

        // Se lo stub restituisce "Esercizio personalizzato" (o vuoto), usa il titolo scelto nella lista
        return when {
            resolvedName.isBlank() -> title.ifBlank { resolvedName }
            resolvedName.equals("Esercizio personalizzato", ignoreCase = true) -> title.ifBlank { resolvedName }
            else -> resolvedName
        }
    }

    fun save(
        onDone: (exerciseId: String, nameIt: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val r = _resolved.value ?: return

        viewModelScope.launch {
            try {
                val newExerciseId = r.idHint ?: UUID.randomUUID().toString()
                val nameItFinal = computeDisplayNameIt(r)

                // 1) salva exercise
                exerciseDao.upsert(
                    ExerciseEntity(
                        id = newExerciseId,
                        nameIt = nameItFinal,
                        nameEn = r.nameEn,
                        descriptionIt = r.descriptionIt,
                        descriptionEn = r.descriptionEn,
                        category = r.category,
                        equipment = r.equipment,
                        mechanics = r.mechanics,
                        level = r.level,
                        isCustom = true
                    )
                )

                // 2) salva muscoli (solo quelli che esistono in gym_muscles)
                val existingMuscleIds = muscleDao.getAllIds().toSet()

                val links = r.muscles
                    .filter { it.muscleId in existingMuscleIds }
                    .map {
                        ExerciseMuscleEntity(
                            exerciseId = newExerciseId,
                            muscleId = it.muscleId,
                            role = it.role,
                            weight = it.weight
                        )
                    }

                if (links.isNotEmpty()) {
                    exerciseMuscleDao.upsertAll(links)
                }

                onDone(newExerciseId, nameItFinal)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}