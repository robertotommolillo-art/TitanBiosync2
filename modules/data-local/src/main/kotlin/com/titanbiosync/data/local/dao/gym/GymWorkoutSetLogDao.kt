package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.titanbiosync.data.local.analytics.ExerciseRawSetRow
import com.titanbiosync.data.local.analytics.ExerciseSessionCountRow
import com.titanbiosync.data.local.analytics.GymWorkoutAnalyticsRow
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymWorkoutSetLogDao {

    @Query(
        "SELECT * FROM gym_workout_set_log WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex ASC"
    )
    fun observeForSessionExercise(sessionExerciseId: String): Flow<List<GymWorkoutSetLogEntity>>

    @Query(
        "SELECT COALESCE(MAX(setIndex), -1) FROM gym_workout_set_log WHERE sessionExerciseId = :sessionExerciseId"
    )
    suspend fun getMaxSetIndex(sessionExerciseId: String): Int

    @Query(
        "SELECT * FROM gym_workout_set_log WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex DESC LIMIT 1"
    )
    suspend fun getLastSet(sessionExerciseId: String): GymWorkoutSetLogEntity?

    @Query(
        """
        SELECT 
            s.id AS sessionId,
            s.startedAt AS startedAt,
            se.exerciseId AS exerciseId,
            se.nameItSnapshot AS exerciseNameIt,
            l.reps AS reps,
            l.weightKg AS weightKg,
            l.completed AS completed
        FROM gym_workout_set_log l
        INNER JOIN gym_workout_session_exercise se ON se.id = l.sessionExerciseId
        INNER JOIN gym_workout_session s ON s.id = se.sessionId
        WHERE s.startedAt >= :startInclusive AND s.startedAt < :endExclusive
        """
    )
    suspend fun getAnalyticsRowsBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<GymWorkoutAnalyticsRow>

    @Query(
        """
        SELECT l.* 
        FROM gym_workout_set_log l
        INNER JOIN gym_workout_session_exercise se ON se.id = l.sessionExerciseId
        WHERE se.sessionId = :sessionId
          AND l.completed = 1
        ORDER BY se.position ASC, l.setIndex ASC
        """
    )
    suspend fun getCompletedForSession(sessionId: String): List<GymWorkoutSetLogEntity>

    @Query(
        """
        SELECT l.*
        FROM gym_workout_set_log l
        INNER JOIN gym_workout_session_exercise se ON se.id = l.sessionExerciseId
        WHERE se.sessionId = :sessionId
        ORDER BY se.position ASC, l.setIndex ASC
        """
    )
    suspend fun getForSession(sessionId: String): List<GymWorkoutSetLogEntity>

    /**
     * Returns the number of distinct completed sessions for each exercise that has at least
     * one completed set in any finished session.
     */
    @Query(
        """
        SELECT se.exerciseId, COUNT(DISTINCT s.id) AS sessionCount
        FROM gym_workout_set_log l
        INNER JOIN gym_workout_session_exercise se ON se.id = l.sessionExerciseId
        INNER JOIN gym_workout_session s ON s.id = se.sessionId
        WHERE l.completed = 1
          AND s.endedAt IS NOT NULL
        GROUP BY se.exerciseId
        """
    )
    suspend fun getExerciseSessionCounts(): List<ExerciseSessionCountRow>

    /**
     * Returns all completed (reps > 0, weight > 0) sets for [exerciseId] across all finished
     * sessions, ordered by session start date ascending.
     * Used to build per-exercise progress time-series.
     */
    @Query(
        """
        SELECT s.id AS sessionId, s.startedAt, l.reps, l.weightKg
        FROM gym_workout_set_log l
        INNER JOIN gym_workout_session_exercise se ON se.id = l.sessionExerciseId
        INNER JOIN gym_workout_session s ON s.id = se.sessionId
        WHERE se.exerciseId = :exerciseId
          AND l.completed = 1
          AND l.reps IS NOT NULL
          AND l.weightKg IS NOT NULL
          AND s.endedAt IS NOT NULL
        ORDER BY s.startedAt ASC
        """
    )
    suspend fun getRawSetsForExercise(exerciseId: String): List<ExerciseRawSetRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: GymWorkoutSetLogEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<GymWorkoutSetLogEntity>)

    @Update
    suspend fun update(item: GymWorkoutSetLogEntity)
}