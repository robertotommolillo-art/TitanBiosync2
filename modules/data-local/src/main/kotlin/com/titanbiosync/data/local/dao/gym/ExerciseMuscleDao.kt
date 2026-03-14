package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity

@Dao
interface ExerciseMuscleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseMuscleEntity>)

    @Query("SELECT * FROM gym_exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun getForExercise(exerciseId: String): List<ExerciseMuscleEntity>

    @Query("SELECT * FROM gym_exercise_muscles WHERE exerciseId IN (:exerciseIds)")
    suspend fun getForExercises(exerciseIds: List<String>): List<ExerciseMuscleEntity>
}