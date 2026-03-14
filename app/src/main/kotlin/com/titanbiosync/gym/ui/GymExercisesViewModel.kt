package com.titanbiosync.gym.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.BuildConfig
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.ExerciseFilterOptionsDao
import com.titanbiosync.data.local.dao.gym.ExerciseMediaDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.data.local.entities.gym.MuscleEntity
import com.titanbiosync.data.local.gym.seed.GymSeedInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GymExercisesViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseMediaDao: ExerciseMediaDao,
    private val muscleDao: MuscleDao,
    private val exerciseFilterOptionsDao: ExerciseFilterOptionsDao,
    private val gymSeedInitializer: GymSeedInitializer
) : ViewModel() {

    init {
        viewModelScope.launch {
            gymSeedInitializer.ensureSeeded(strict = BuildConfig.DEBUG)
        }
    }

    enum class MuscleMatchMode { ANY, ALL }

    data class Filters(
        val category: String? = null,
        val equipment: String? = null,
        val mechanics: String? = null,
        val level: String? = null,
        val muscleIds: List<String> = emptyList(),
        val role: String? = null,
        val muscleMatchMode: MuscleMatchMode = MuscleMatchMode.ANY
    )

    private val query = MutableStateFlow("")
    private val filtersState = MutableStateFlow(Filters())

    val filters: LiveData<Filters> = filtersState.asLiveData()

    val muscles: LiveData<List<MuscleEntity>> =
        muscleDao.observeAll().asLiveData()

    val equipmentOptions: LiveData<List<String>> =
        exerciseFilterOptionsDao.observeEquipments().asLiveData()

    val levelOptions: LiveData<List<String>> =
        exerciseFilterOptionsDao.observeLevels().asLiveData()

    val mechanicsOptions: LiveData<List<String>> =
        exerciseFilterOptionsDao.observeMechanics().asLiveData()

    val categoryOptions: LiveData<List<String>> =
        exerciseFilterOptionsDao.observeCategories().asLiveData()

    private val exercisesFlow: StateFlow<List<ExerciseEntity>> =
        combine(query, filtersState) { q, f -> q to f }
            .flatMapLatest { (q, f) ->
                val hasQuery = q.isNotBlank()
                val hasMuscles = f.muscleIds.isNotEmpty()

                when {
                    hasQuery && !hasMuscles &&
                            f.category == null && f.equipment == null && f.mechanics == null && f.level == null && f.role == null -> {
                        exerciseDao.searchActive(q)
                    }

                    else -> {
                        val baseFlow =
                            if (hasMuscles) {
                                when (f.muscleMatchMode) {
                                    MuscleMatchMode.ANY -> exerciseDao.filterActiveAny(
                                        category = f.category,
                                        equipment = f.equipment,
                                        mechanics = f.mechanics,
                                        level = f.level,
                                        muscleIds = f.muscleIds,
                                        muscleIdsEmpty = false,
                                        role = f.role
                                    )

                                    MuscleMatchMode.ALL -> exerciseDao.filterActiveAll(
                                        category = f.category,
                                        equipment = f.equipment,
                                        mechanics = f.mechanics,
                                        level = f.level,
                                        muscleIds = f.muscleIds,
                                        muscleCount = f.muscleIds.size,
                                        role = f.role
                                    )
                                }
                            } else {
                                exerciseDao.filterActiveBasic(
                                    category = f.category,
                                    equipment = f.equipment,
                                    mechanics = f.mechanics,
                                    level = f.level
                                )
                            }

                        if (!hasQuery) {
                            baseFlow
                        } else {
                            val needle = q.trim().lowercase()
                            baseFlow.map { list ->
                                list.filter { e ->
                                    e.nameIt.lowercase().contains(needle) || e.nameEn.lowercase().contains(needle)
                                }
                            }
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val exercises: LiveData<List<ExerciseEntity>> = exercisesFlow.asLiveData()

    private val selectedExerciseId = MutableStateFlow<String?>(null)

    val currentVideos: LiveData<List<ExerciseMediaEntity>> =
        selectedExerciseId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else exerciseMediaDao.observeVideosForExercise(id)
            }
            .asLiveData()

    fun setQuery(q: String) {
        query.value = q
    }

    fun setFilters(update: (Filters) -> Filters) {
        filtersState.value = update(filtersState.value)
    }

    fun clearFilters() {
        filtersState.value = Filters()
    }

    fun selectExercise(exerciseId: String) {
        selectedExerciseId.value = exerciseId
    }
}