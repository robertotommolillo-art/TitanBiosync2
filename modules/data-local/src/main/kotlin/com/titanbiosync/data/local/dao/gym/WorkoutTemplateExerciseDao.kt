package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.data.local.model.gym.TemplateExerciseRow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateExerciseDao {

    @Query("SELECT * FROM gym_workout_template_exercises WHERE templateId = :templateId ORDER BY position ASC")
    fun observeByTemplate(templateId: String): Flow<List<WorkoutTemplateExerciseEntity>>

    @Query(
        """
        SELECT 
            t.position AS position,
            t.exerciseId AS exerciseId,
            e.nameIt AS nameIt,
            e.nameEn AS nameEn,
            t.supersetGroupId AS supersetGroupId,
            t.supersetOrder AS supersetOrder
        FROM gym_workout_template_exercises t
        JOIN gym_exercises e ON e.id = t.exerciseId
        WHERE t.templateId = :templateId
        ORDER BY t.position ASC
        """
    )
    fun observeRows(templateId: String): Flow<List<TemplateExerciseRow>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM gym_workout_template_exercises WHERE templateId = :templateId")
    suspend fun getMaxPosition(templateId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WorkoutTemplateExerciseEntity>)

    @Query("DELETE FROM gym_workout_template_exercises WHERE templateId = :templateId")
    suspend fun deleteAllForTemplate(templateId: String)

    @Query("DELETE FROM gym_workout_template_exercises WHERE templateId = :templateId AND position = :position")
    suspend fun deleteOne(templateId: String, position: Int)

    @Query(
        """
        SELECT * FROM gym_workout_template_exercises
        WHERE templateId = :templateId
        ORDER BY position ASC
        """
    )
    suspend fun getAllForTemplateOnce(templateId: String): List<WorkoutTemplateExerciseEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM gym_workout_template_exercises WHERE templateId = :templateId AND exerciseId = :exerciseId)")
    suspend fun exists(templateId: String, exerciseId: String): Boolean

    @Transaction
    suspend fun deleteAndReindex(templateId: String, positionToDelete: Int) {
        val current = getAllForTemplateOnce(templateId)

        val remaining = current
            .filterNot { it.position == positionToDelete }
            .sortedBy { it.position }
            .mapIndexed { newIndex, item -> item.copy(position = newIndex) }

        deleteAllForTemplate(templateId)
        if (remaining.isNotEmpty()) {
            upsertAll(remaining)
        }
    }

    @Transaction
    suspend fun deleteOneOrSupersetAndReindex(
        templateId: String,
        positionToDelete: Int,
        supersetGroupId: String?
    ) {
        val current = getAllForTemplateOnce(templateId)

        val remaining = current
            .filterNot { item ->
                if (supersetGroupId.isNullOrBlank()) {
                    item.position == positionToDelete
                } else {
                    item.supersetGroupId == supersetGroupId
                }
            }
            .sortedBy { it.position }
            .mapIndexed { newIndex, item -> item.copy(position = newIndex) }

        deleteAllForTemplate(templateId)
        if (remaining.isNotEmpty()) {
            upsertAll(remaining)
        }
    }
}