package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateExerciseSetDao {

    @Query(
        """
        SELECT * FROM gym_workout_template_exercise_sets
        WHERE templateId = :templateId AND position = :position
        ORDER BY setIndex ASC
        """
    )
    fun observeForExercise(templateId: String, position: Int): Flow<List<WorkoutTemplateExerciseSetEntity>>

    @Query(
        """
        SELECT * FROM gym_workout_template_exercise_sets
        WHERE templateId = :templateId
        ORDER BY position ASC, setIndex ASC
        """
    )
    suspend fun getAllForTemplateOnce(templateId: String): List<WorkoutTemplateExerciseSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WorkoutTemplateExerciseSetEntity>)

    @Query(
        """
        DELETE FROM gym_workout_template_exercise_sets
        WHERE templateId = :templateId AND position = :position
        """
    )
    suspend fun deleteForExercise(templateId: String, position: Int)

    @Query(
        """
        DELETE FROM gym_workout_template_exercise_sets
        WHERE templateId = :templateId
        """
    )
    suspend fun deleteAllForTemplate(templateId: String)
}