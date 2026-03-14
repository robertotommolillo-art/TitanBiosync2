package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.HealthMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricsDao {
    @Query("SELECT * FROM health_metrics WHERE user_id = :userId ORDER BY date DESC")
    fun observeByUser(userId: String): Flow<List<HealthMetricsEntity>>

    @Query("SELECT * FROM health_metrics WHERE user_id = :userId AND date = :date LIMIT 1")
    fun observeByUserAndDate(userId: String, date: String): Flow<HealthMetricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metrics: HealthMetricsEntity)

    @Query("DELETE FROM health_metrics WHERE user_id = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: String, date: String)
}