package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("""
        SELECT * FROM gym_workout_templates
        WHERE folderId IS NULL
        ORDER BY sortIndex ASC, updatedAt DESC
    """)
    fun observeWithoutFolder(): Flow<List<WorkoutTemplateEntity>>

    @Query("""
        SELECT * FROM gym_workout_templates
        WHERE folderId = :folderId
        ORDER BY sortIndex ASC, updatedAt DESC
    """)
    fun observeByFolderId(folderId: String): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM gym_workout_templates WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WorkoutTemplateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: WorkoutTemplateEntity)

    @Query("DELETE FROM gym_workout_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE gym_workout_templates SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)

    @Query("UPDATE gym_workout_templates SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: String, sortIndex: Int)

    @Query("SELECT MAX(sortIndex) FROM gym_workout_templates WHERE folderId = :folderId")
    suspend fun getMaxSortIndexForFolder(folderId: String): Int?

    @Query("SELECT MAX(sortIndex) FROM gym_workout_templates WHERE folderId IS NULL")
    suspend fun getMaxSortIndexWithoutFolder(): Int?

    @Query("""
    SELECT * FROM gym_workout_templates
    WHERE folderId IS NULL
    ORDER BY sortIndex ASC, updatedAt DESC
""")
    suspend fun getWithoutFolderOnce(): List<WorkoutTemplateEntity>

    @Query("""
    SELECT * FROM gym_workout_templates
    WHERE folderId = :folderId
    ORDER BY sortIndex ASC, updatedAt DESC
""")
    suspend fun getByFolderIdOnce(folderId: String): List<WorkoutTemplateEntity>
}