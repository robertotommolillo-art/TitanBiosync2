package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseMediaEntity>)

    @Query("SELECT COUNT(*) FROM gym_exercise_media")
    suspend fun count(): Int

    @Query(
        """
        SELECT * 
        FROM gym_exercise_media
        WHERE exerciseId = :exerciseId
          AND type = 'video'
        ORDER BY id
        LIMIT 1
        """
    )
    fun observeFirstVideoForExercise(exerciseId: String): Flow<ExerciseMediaEntity?>

    @Query(
        """
        SELECT *
        FROM gym_exercise_media
        WHERE exerciseId = :exerciseId
          AND type = 'video'
        ORDER BY
          CASE source
            WHEN 'remote' THEN 0
            WHEN 'asset' THEN 1
            ELSE 2
          END,
          id
        """
    )
    fun observeVideosForExercise(exerciseId: String): Flow<List<ExerciseMediaEntity>>
}