package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.ExercisePrEntity

@Dao
interface ExercisePrDao {

    /** Returns the stored PR for the given exercise, or null if none exists. */
    @Query("SELECT * FROM gym_exercise_pr WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getByExerciseId(exerciseId: String): ExercisePrEntity?

    /** Returns stored PRs for multiple exercises in one query. */
    @Query("SELECT * FROM gym_exercise_pr WHERE exerciseId IN (:exerciseIds)")
    suspend fun getByExerciseIds(exerciseIds: List<String>): List<ExercisePrEntity>

    /**
     * Inserts or replaces the PR record for an exercise.
     * Should be called only when a new PR has been confirmed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pr: ExercisePrEntity)
}
