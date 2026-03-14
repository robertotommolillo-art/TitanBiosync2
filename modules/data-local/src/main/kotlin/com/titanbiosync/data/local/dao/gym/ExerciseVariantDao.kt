package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.ExerciseVariantEntity

@Dao
interface ExerciseVariantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseVariantEntity>)

    @Query("SELECT COUNT(*) FROM gym_exercise_variants")
    suspend fun count(): Int

    @Query("SELECT id FROM gym_exercise_variants")
    suspend fun getAllIds(): List<String>
}