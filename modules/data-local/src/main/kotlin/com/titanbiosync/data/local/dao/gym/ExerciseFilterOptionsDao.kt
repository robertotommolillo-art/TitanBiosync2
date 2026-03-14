package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseFilterOptionsDao {

    @Query("SELECT DISTINCT category FROM gym_exercises WHERE archivedAt IS NULL ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM gym_exercises WHERE archivedAt IS NULL AND equipment IS NOT NULL ORDER BY equipment ASC")
    fun observeEquipments(): Flow<List<String>>

    @Query("SELECT DISTINCT mechanics FROM gym_exercises WHERE archivedAt IS NULL AND mechanics IS NOT NULL ORDER BY mechanics ASC")
    fun observeMechanics(): Flow<List<String>>

    @Query("SELECT DISTINCT level FROM gym_exercises WHERE archivedAt IS NULL AND level IS NOT NULL ORDER BY level ASC")
    fun observeLevels(): Flow<List<String>>
}