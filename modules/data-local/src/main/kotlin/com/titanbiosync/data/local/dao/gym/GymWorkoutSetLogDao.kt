package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: GymWorkoutSetLogEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<GymWorkoutSetLogEntity>)

    @Update
    suspend fun update(item: GymWorkoutSetLogEntity)
}