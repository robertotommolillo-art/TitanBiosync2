package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymWorkoutSessionExerciseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<GymWorkoutSessionExerciseEntity>)

    @Query(
        "SELECT * FROM gym_workout_session_exercise WHERE sessionId = :sessionId ORDER BY position ASC"
    )
    fun observeForSession(sessionId: String): Flow<List<GymWorkoutSessionExerciseEntity>>

    @Query(
        "SELECT * FROM gym_workout_session_exercise WHERE sessionId = :sessionId ORDER BY position ASC"
    )
    suspend fun getForSession(sessionId: String): List<GymWorkoutSessionExerciseEntity>

    @Query("DELETE FROM gym_workout_session_exercise WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}