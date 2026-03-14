package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExerciseEntity)

    // --------------------
    // Base
    // --------------------

    @Query("SELECT * FROM gym_exercises ORDER BY nameIt ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM gym_exercises WHERE archivedAt IS NULL ORDER BY nameIt ASC")
    fun observeAllActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT id FROM gym_exercises")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM gym_exercises")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM gym_exercises")
    suspend fun count(): Int

    // --------------------
    // Search (LIKE)
    // --------------------

    @Query(
        """
        SELECT * FROM gym_exercises
        WHERE archivedAt IS NULL
          AND (
            nameIt LIKE '%' || :q || '%'
            OR nameEn LIKE '%' || :q || '%'
          )
        ORDER BY nameIt ASC
        LIMIT :limit
        """
    )
    fun searchActive(q: String, limit: Int = 100): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM gym_exercises
        WHERE archivedAt IS NULL
          AND (
            nameIt LIKE '%' || :q || '%'
            OR nameEn LIKE '%' || :q || '%'
          )
        ORDER BY nameIt ASC
        LIMIT :limit
        """
    )
    fun search(q: String, limit: Int = 100): Flow<List<ExerciseEntity>>

    // --------------------
    // Filtri semplici
    // --------------------

    @Query(
        """
        SELECT * FROM gym_exercises
        WHERE archivedAt IS NULL
          AND (:category IS NULL OR category = :category)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:mechanics IS NULL OR mechanics = :mechanics)
          AND (:level IS NULL OR level = :level)
        ORDER BY nameIt ASC
        LIMIT :limit
        """
    )
    fun filterActiveBasic(
        category: String? = null,
        equipment: String? = null,
        mechanics: String? = null,
        level: String? = null,
        limit: Int = 200
    ): Flow<List<ExerciseEntity>>

    // ============================================================
    // Muscles: ANY (OR)  -> almeno uno dei muscoli selezionati
    // ============================================================

    @Query(
        """
        SELECT e.*
        FROM gym_exercises e
        JOIN gym_exercise_muscles em ON em.exerciseId = e.id
        WHERE e.archivedAt IS NULL
          AND em.muscleId IN (:muscleIds)
          AND (:role IS NULL OR em.role = :role)
        GROUP BY e.id
        ORDER BY e.nameIt ASC
        LIMIT :limit
        """
    )
    fun filterActiveByMusclesAny(
        muscleIds: List<String>,
        role: String? = null,
        limit: Int = 200
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT e.*
        FROM gym_exercises e
        LEFT JOIN gym_exercise_muscles em ON em.exerciseId = e.id
        WHERE e.archivedAt IS NULL
          AND (:category IS NULL OR e.category = :category)
          AND (:equipment IS NULL OR e.equipment = :equipment)
          AND (:mechanics IS NULL OR e.mechanics = :mechanics)
          AND (:level IS NULL OR e.level = :level)
          AND (
            :muscleIdsEmpty = 1
            OR em.muscleId IN (:muscleIds)
          )
          AND (:role IS NULL OR em.role = :role)
        GROUP BY e.id
        ORDER BY e.nameIt ASC
        LIMIT :limit
        """
    )
    fun filterActiveAny(
        category: String? = null,
        equipment: String? = null,
        mechanics: String? = null,
        level: String? = null,
        muscleIds: List<String> = emptyList(),
        muscleIdsEmpty: Boolean = true,
        role: String? = null,
        limit: Int = 200
    ): Flow<List<ExerciseEntity>>

    // ============================================================
    // Muscles: ALL (AND) -> deve avere TUTTI i muscoli selezionati
    // ============================================================

    @Query(
        """
        SELECT e.*
        FROM gym_exercises e
        JOIN gym_exercise_muscles em ON em.exerciseId = e.id
        WHERE e.archivedAt IS NULL
          AND em.muscleId IN (:muscleIds)
          AND (:role IS NULL OR em.role = :role)
        GROUP BY e.id
        HAVING COUNT(DISTINCT em.muscleId) = :muscleCount
        ORDER BY e.nameIt ASC
        LIMIT :limit
        """
    )
    fun filterActiveByMusclesAll(
        muscleIds: List<String>,
        muscleCount: Int,
        role: String? = null,
        limit: Int = 200
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT e.*
        FROM gym_exercises e
        JOIN gym_exercise_muscles em ON em.exerciseId = e.id
        WHERE e.archivedAt IS NULL
          AND (:category IS NULL OR e.category = :category)
          AND (:equipment IS NULL OR e.equipment = :equipment)
          AND (:mechanics IS NULL OR e.mechanics = :mechanics)
          AND (:level IS NULL OR e.level = :level)
          AND em.muscleId IN (:muscleIds)
          AND (:role IS NULL OR em.role = :role)
        GROUP BY e.id
        HAVING COUNT(DISTINCT em.muscleId) = :muscleCount
        ORDER BY e.nameIt ASC
        LIMIT :limit
        """
    )
    fun filterActiveAll(
        category: String? = null,
        equipment: String? = null,
        mechanics: String? = null,
        level: String? = null,
        muscleIds: List<String>,
        muscleCount: Int,
        role: String? = null,
        limit: Int = 200
    ): Flow<List<ExerciseEntity>>
}