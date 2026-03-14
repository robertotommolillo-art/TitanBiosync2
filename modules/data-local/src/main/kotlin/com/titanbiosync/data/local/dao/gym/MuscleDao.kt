package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.MuscleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MuscleEntity>)

    @Query("SELECT COUNT(*) FROM gym_muscles")
    suspend fun count(): Int

    @Query("SELECT id FROM gym_muscles")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM gym_muscles ORDER BY nameIt ASC")
    fun observeAll(): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM gym_muscles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MuscleEntity>
}