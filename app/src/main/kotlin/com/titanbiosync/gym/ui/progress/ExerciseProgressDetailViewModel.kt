package com.titanbiosync.gym.ui.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.gym.domain.ExerciseProgressAggregator
import com.titanbiosync.gym.domain.ExerciseSessionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class ProgressMetric { E1RM, MAX_WEIGHT, VOLUME }
enum class ProgressRange { DAYS_30, DAYS_90, DAYS_365, ALL_TIME }

data class ExerciseProgressChartUi(
    val exerciseName: String,
    val points: List<ExerciseSessionPoint>,
    val metric: ProgressMetric,
    val yLabel: String
)

@HiltViewModel
class ExerciseProgressDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setLogDao: GymWorkoutSetLogDao
) : ViewModel() {

    val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    val exerciseName: String = savedStateHandle["exerciseName"] ?: ""

    private val _allPoints = MutableStateFlow<List<ExerciseSessionPoint>>(emptyList())
    private val _metric = MutableStateFlow(ProgressMetric.E1RM)
    private val _range = MutableStateFlow(ProgressRange.ALL_TIME)

    val metric: LiveData<ProgressMetric> = _metric.asLiveData()
    val range: LiveData<ProgressRange> = _range.asLiveData()

    val chartUi: LiveData<ExerciseProgressChartUi> =
        combine(_allPoints, _metric, _range) { all, metric, range ->
            val cutoff = when (range) {
                ProgressRange.DAYS_30 -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                ProgressRange.DAYS_90 -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
                ProgressRange.DAYS_365 -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365)
                ProgressRange.ALL_TIME -> 0L
            }
            val filtered = if (cutoff > 0L) all.filter { it.sessionDate >= cutoff } else all

            // When metric is E1RM but no session has e1RM, fall back to max weight
            val effectiveMetric = if (metric == ProgressMetric.E1RM && filtered.all { it.bestE1rm == null }) {
                ProgressMetric.MAX_WEIGHT
            } else {
                metric
            }

            val yLabel = when (effectiveMetric) {
                ProgressMetric.E1RM -> "e1RM (kg)"
                ProgressMetric.MAX_WEIGHT -> "Peso max (kg)"
                ProgressMetric.VOLUME -> "Volume (kg)"
            }

            ExerciseProgressChartUi(
                exerciseName = exerciseName,
                points = filtered,
                metric = effectiveMetric,
                yLabel = yLabel
            )
        }.asLiveData()

    init {
        loadData()
    }

    fun setMetric(metric: ProgressMetric) {
        _metric.value = metric
    }

    fun setRange(range: ProgressRange) {
        _range.value = range
    }

    private fun loadData() {
        viewModelScope.launch {
            val rows = setLogDao.getRawSetsForExercise(exerciseId)
            _allPoints.value = ExerciseProgressAggregator.aggregate(rows)
        }
    }
}
