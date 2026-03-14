package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.RecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM recommendations WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeByUser(userId: String): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recommendation: RecommendationEntity)

    @Query("DELETE FROM recommendations WHERE id = :id")
    suspend fun deleteById(id: String)
}