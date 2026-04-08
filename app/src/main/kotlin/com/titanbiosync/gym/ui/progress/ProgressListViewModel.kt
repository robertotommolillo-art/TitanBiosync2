package com.titanbiosync.gym.ui.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressExerciseUi(
    val exerciseId: String,
    val exerciseName: String,
    val sessionCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ProgressListViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val setLogDao: GymWorkoutSetLogDao
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Map from exerciseId to number of distinct sessions containing completed sets. */
    private val _sessionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    val exercises: LiveData<List<ProgressExerciseUi>> = _query
        .debounce(200)
        .flatMapLatest { q ->
            exerciseDao.search(q, limit = 200)
        }
        .combine(_sessionCounts) { exercises, counts ->
            exercises.map { ex ->
                ProgressExerciseUi(
                    exerciseId = ex.id,
                    exerciseName = ex.nameIt,
                    sessionCount = counts[ex.id] ?: 0
                )
            }
        }
        .asLiveData()

    init {
        loadSessionCounts()
    }

    fun setQuery(q: String) {
        _query.value = q.trim()
    }

    private fun loadSessionCounts() {
        viewModelScope.launch {
            // Fetch all raw set rows for each exercise — we use a simpler approach:
            // get all exercises that have at least one completed set, grouped by exerciseId
            val rows = setLogDao.getExerciseSessionCounts()
            _sessionCounts.value = rows.associate { it.exerciseId to it.sessionCount }
        }
    }
}
