package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): AppConfigEntity?

    @Query("SELECT value_json FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM app_config")
    fun observeAll(): Flow<List<AppConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AppConfigEntity)

    @Query("DELETE FROM app_config WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}