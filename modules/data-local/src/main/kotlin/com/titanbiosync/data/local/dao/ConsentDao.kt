package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.ConsentRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsentDao {
    @Query("SELECT * FROM consents WHERE user_id = :userId")
    fun observeByUser(userId: String): Flow<List<ConsentRecordEntity>>

    @Query("SELECT * FROM consents WHERE user_id = :userId AND consent_type = :type LIMIT 1")
    suspend fun findByUserAndType(userId: String, type: String): ConsentRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consent: ConsentRecordEntity)

    @Query("DELETE FROM consents WHERE user_id = :userId AND consent_type = :type")
    suspend fun deleteByUserAndType(userId: String, type: String)
}