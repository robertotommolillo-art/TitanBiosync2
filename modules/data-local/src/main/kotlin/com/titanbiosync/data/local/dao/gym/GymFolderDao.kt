package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymFolderDao {

    @Query("SELECT * FROM gym_folders ORDER BY sortIndex ASC, createdAt ASC")
    fun observeAll(): Flow<List<GymFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: GymFolderEntity)

    @Query("DELETE FROM gym_folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM gym_folders WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): GymFolderEntity?

    @Query("SELECT MAX(sortIndex) FROM gym_folders")
    suspend fun getMaxSortIndex(): Int?
}