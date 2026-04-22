package com.titanbiosync.data.local.dao.gym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymWorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: GymWorkoutSessionEntity)

    @Query("SELECT * FROM gym_workout_session WHERE id = :id")
    fun observeById(id: String): Flow<GymWorkoutSessionEntity?>

    @Query("SELECT * FROM gym_workout_session WHERE id = :id")
    suspend fun getById(id: String): GymWorkoutSessionEntity?

    @Query("UPDATE gym_workout_session SET endedAt = :endedAt WHERE id = :id")
    suspend fun endSession(id: String, endedAt: Long)

    @Query(
        """
        SELECT * FROM gym_workout_session
        WHERE templateId = :templateId
          AND endedAt IS NOT NULL
          AND startedAt < :beforeStartedAt
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    suspend fun findPreviousCompletedByTemplate(
        templateId: String,
        beforeStartedAt: Long
    ): GymWorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM gym_workout_session
        WHERE startedAt >= :startInclusive AND startedAt < :endExclusive
        ORDER BY startedAt DESC
        """
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<GymWorkoutSessionEntity>>

    @Query(
        """
        SELECT * FROM gym_workout_session
        ORDER BY startedAt DESC
        """
    )
    fun observeAll(): Flow<List<GymWorkoutSessionEntity>>

    @Query(
        """
        SELECT * FROM gym_workout_session 
        WHERE endedAt IS NOT NULL 
        ORDER BY startedAt DESC 
        LIMIT :limit
        """
    )
    suspend fun getRecentCompleted(limit: Int): List<GymWorkoutSessionEntity>

    @Query(
        """
        SELECT startedAt FROM gym_workout_session 
        WHERE endedAt IS NOT NULL AND startedAt >= :since 
        ORDER BY startedAt DESC
        """
    )
    suspend fun getCompletedStartTimesSince(since: Long): List<Long>
}
