package com.titanbiosync.gym.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.ExerciseMuscleDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class WorkoutReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val setDao: GymWorkoutSetLogDao,
    private val exerciseMuscleDao: ExerciseMuscleDao,
    private val muscleDao: MuscleDao
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _summary = MutableStateFlow(WorkoutSummaryUiState.empty(sessionId))
    val summary = _summary.asLiveData()

    private val _muscles = MutableStateFlow<List<MuscleWorkUi>>(emptyList())
    val muscles = _muscles.asLiveData()

    private val _exercises = MutableStateFlow<List<ExerciseWorkUi>>(emptyList())
    val exercises = _exercises.asLiveData()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val session = sessionDao.getById(sessionId)
            if (session == null) {
                _summary.value = WorkoutSummaryUiState.empty(sessionId)
                _muscles.value = emptyList()
                _exercises.value = emptyList()
                return@launch
            }

            val endedAt = session.endedAt ?: System.currentTimeMillis()
            val durationMinutes = ((endedAt - session.startedAt) / 60_000.0).roundToInt()

            val sessionExercises = sessionExerciseDao.getForSession(sessionId)
            val setLogs = setDao.getForSession(sessionId)
            val completedSetLogs = setLogs.filter { it.completed }

            val totalVolume = completedSetLogs.sumOf { l ->
                val reps = l.reps ?: 0
                val w = l.weightKg ?: 0f
                (reps * w).toDouble()
            }.toFloat()

            // ---- previous session (same template) ----
            val previousSession = sessionDao.findPreviousCompletedByTemplate(
                templateId = session.templateId,
                beforeStartedAt = session.startedAt
            )
            val previousVolume: Float? = if (previousSession != null) {
                val prevLogs = setDao.getForSession(previousSession.id).filter { it.completed }
                prevLogs.sumOf { l ->
                    val reps = l.reps ?: 0
                    val w = l.weightKg ?: 0f
                    (reps * w).toDouble()
                }.toFloat()
            } else null

            _summary.value = WorkoutSummaryUiState(
                sessionId = session.id,
                durationMinutes = durationMinutes.coerceAtLeast(0),
                completedSets = completedSetLogs.size,
                totalVolume = totalVolume,
                previousVolume = previousVolume
            )

            // ---- work per sessionExercise (reused by exercises & muscles) ----
            val workBySessionExerciseId: Map<String, Float> =
                completedSetLogs.groupBy { it.sessionExerciseId }
                    .mapValues { (_, logs) ->
                        logs.sumOf { l ->
                            val reps = l.reps ?: 0
                            val w = l.weightKg ?: 0f
                            (reps * w).toDouble()
                        }.toFloat()
                    }

            // ---- Exercises tab (MVP: volume per exercise) ----
            val exercisesUi = sessionExercises.map { se ->
                ExerciseWorkUi(
                    sessionExerciseId = se.id,
                    exerciseId = se.exerciseId,
                    nameIt = se.nameItSnapshot,
                    volume = workBySessionExerciseId[se.id] ?: 0f
                )
            }.sortedByDescending { it.volume }

            _exercises.value = exercisesUi

            // ---- Muscles tab (weighted) ----
            val exerciseIds = sessionExercises.map { it.exerciseId }.distinct()
            if (exerciseIds.isEmpty()) {
                _muscles.value = emptyList()
                return@launch
            }

            val musclesByExercise = exerciseMuscleDao.getForExercises(exerciseIds)
                .groupBy { it.exerciseId }

            fun roleFactor(role: String): Float {
                return when (role.trim().uppercase()) {
                    "PRIMARY" -> 1.0f
                    "SECONDARY" -> 0.6f
                    "SYNERGIST" -> 0.5f
                    "STABILIZER" -> 0.35f
                    else -> 0.5f
                }
            }

            fun contribution(exerciseWork: Float, em: ExerciseMuscleEntity): Float {
                val base = exerciseWork * roleFactor(em.role)
                val weighted = base * em.weight
                return weighted.coerceAtLeast(0f)
            }

            val muscleWork = linkedMapOf<String, Float>()
            for (se in sessionExercises) {
                val exerciseWork = workBySessionExerciseId[se.id] ?: 0f
                if (exerciseWork <= 0f) continue

                val emList = musclesByExercise[se.exerciseId].orEmpty()
                for (em in emList) {
                    val add = contribution(exerciseWork, em)
                    if (add <= 0f) continue
                    muscleWork[em.muscleId] = (muscleWork[em.muscleId] ?: 0f) + add
                }
            }

            if (muscleWork.isEmpty()) {
                _muscles.value = emptyList()
                return@launch
            }

            val max = muscleWork.values.maxOrNull()?.takeIf { it > 0f } ?: 1f
            val muscleIds = muscleWork.keys.toList()
            val muscles = muscleDao.getByIds(muscleIds).associateBy { it.id }

            val musclesUi = muscleWork.entries
                .mapNotNull { (muscleId, work) ->
                    val muscle = muscles[muscleId] ?: return@mapNotNull null
                    val intensity = (work / max).coerceIn(0f, 1f)
                    MuscleWorkUi(
                        muscleId = muscle.id,
                        muscleNameIt = muscle.nameIt,
                        intensity = intensity,
                        rawWork = work
                    )
                }
                .sortedByDescending { it.rawWork }

            _muscles.value = musclesUi
        }
    }
}

data class WorkoutSummaryUiState(
    val sessionId: String,
    val durationMinutes: Int,
    val completedSets: Int,
    val totalVolume: Float,
    val previousVolume: Float?
) {
    companion object {
        fun empty(sessionId: String) = WorkoutSummaryUiState(
            sessionId = sessionId,
            durationMinutes = 0,
            completedSets = 0,
            totalVolume = 0f,
            previousVolume = null
        )
    }
}

data class MuscleWorkUi(
    val muscleId: String,
    val muscleNameIt: String,
    val intensity: Float, // 0..1
    val rawWork: Float
)

data class ExerciseWorkUi(
    val sessionExerciseId: String,
    val exerciseId: String,
    val nameIt: String,
    val volume: Float
)