package com.titanbiosync.gym.domain

import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionExerciseEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class CreateGymWorkoutSessionFromTemplate @Inject constructor(
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao
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
        }

        return sessionId
    }
}