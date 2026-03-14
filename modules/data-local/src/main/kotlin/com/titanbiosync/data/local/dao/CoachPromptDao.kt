package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.CoachPromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachPromptDao {
    @Query("SELECT * FROM coach_prompts WHERE user_id = :userId ORDER BY timestamp DESC")
    fun observeConversation(userId: String): Flow<List<CoachPromptEntity>>

    @Query("SELECT * FROM coach_prompts WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findRecent(userId: String, limit: Int = 50): List<CoachPromptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: CoachPromptEntity)

    @Query("DELETE FROM coach_prompts WHERE user_id = :userId")
    suspend fun deleteByUser(userId: String)
}