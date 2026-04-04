package com.titanbiosync.gym.domain

import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionExerciseEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class CreateGymWorkoutSessionFromTemplate @Inject constructor(
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao,
    private val templateExerciseSetDao: WorkoutTemplateExerciseSetDao,
    private val setLogDao: GymWorkoutSetLogDao
) {
    suspend operator fun invoke(templateId: String): String {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        sessionDao.insert(
            GymWorkoutSessionEntity(
                id = sessionId,
                templateId = templateId,
                startedAt = now,
                endedAt = null
            )
        )

        val rows = templateExerciseDao.observeRows(templateId).first()

        val sessionExercises = rows.map { row ->
            GymWorkoutSessionExerciseEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = row.exerciseId,
                position = row.position,
                nameItSnapshot = row.nameIt
            )
        }

        if (sessionExercises.isNotEmpty()) {
            sessionExerciseDao.insertAll(sessionExercises)

            // Build a map from template exercise position -> session exercise id
            val positionToSessionExerciseId = sessionExercises.associate { it.position to it.id }

            // Fetch template set definitions and pre-populate set log rows
            val templateSets = templateExerciseSetDao.getAllForTemplateOnce(templateId)
            val setLogs = templateSets.mapNotNull { templateSet ->
                val sessionExerciseId = positionToSessionExerciseId[templateSet.position]
                    ?: return@mapNotNull null
                GymWorkoutSetLogEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = sessionExerciseId,
                    setIndex = templateSet.setIndex,
                    reps = templateSet.reps,
                    weightKg = null,
                    completed = false,
                    completedAt = null,
                    rpe = null
                )
            }
            if (setLogs.isNotEmpty()) {
                setLogDao.insertAll(setLogs)
            }
        }

        return sessionId
    }
}