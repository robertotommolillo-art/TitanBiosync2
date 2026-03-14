# create_dao_files.ps1
# Esegui nella root del progetto (dove si trova settings.gradle.kts).
# Crea i file DAO dentro modules/data-local/src/main/kotlin/com/titanbiosync/data/local/dao
# Esegue backup dei file già esistenti (file.bak_yyyymmdd_HHMMSS).

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$daoDir = Join-Path $root "modules\data-local\src\main\kotlin\com\titanbiosync\data\local\dao"

# Lista dei file da creare: nome => contenuto (heredoc)
$files = @{
"HealthMetricsDao.kt" = @"
package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.HealthMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricsDao {
    @Query("SELECT * FROM health_metrics WHERE user_id = :userId AND date = :date LIMIT 1")
    fun observeByUserAndDate(userId: String, date: String): Flow<HealthMetricsEntity?>

    @Query("SELECT * FROM health_metrics WHERE user_id = :userId ORDER BY date DESC")
    fun observeByUser(userId: String): Flow<List<HealthMetricsEntity>>

    @Query("SELECT * FROM health_metrics WHERE user_id = :userId AND date BETWEEN :fromDate AND :toDate ORDER BY date ASC")
    suspend fun findBetweenDates(userId: String, fromDate: String, toDate: String): List<HealthMetricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metrics: HealthMetricsEntity)

    @Update
    suspend fun update(metrics: HealthMetricsEntity)

    @Query("DELETE FROM health_metrics WHERE id = :id")
    suspend fun deleteById(id: String)
}
"@

"RecommendationDao.kt" = @"
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

    @Update
    suspend fun update(recommendation: RecommendationEntity)

    @Query("DELETE FROM recommendations WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
"@

"CoachPromptDao.kt" = @"
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

    @Query("SELECT * FROM coach_prompts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CoachPromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: CoachPromptEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(prompts: List<CoachPromptEntity>)

    @Query("DELETE FROM coach_prompts WHERE id = :id")
    suspend fun deleteById(id: String)
}
"@

"MapLocationDao.kt" = @"
package com.titanbiosync.data.local.dao

import androidx.room.*
import com.titanbiosync.data.local.entities.MapLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapLocationDao {
    @Query("SELECT * FROM route_points WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<MapLocationEntity>>

    @Query("SELECT * FROM route_points WHERE session_id = :sessionId AND timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun findBetween(sessionId: String, from: Long, to: Long): List<MapLocationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(points: List<MapLocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: MapLocationEntity)

    @Query("DELETE FROM route_points WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
"@

"ConsentDao.kt" = @"
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
    suspend fun revoke(userId: String, type: String)
}
"@

"AppConfigDao.kt" = @"
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
"@
}

# Crea la directory se non esiste
if (-not (Test-Path $daoDir)) {
    Write-Host "Creazione directory: $daoDir"
    New-Item -ItemType Directory -Path $daoDir -Force | Out-Null
} else {
    Write-Host "Directory esistente: $daoDir"
}

# Funzione helper per backup + scrittura
function Write-FileWithBackup($path, $content) {
    if (Test-Path $path) {
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $backup = "$path.bak_$timestamp"
        Copy-Item -Path $path -Destination $backup -Force
        Write-Host "Backup creato: $backup"
    }
    # Scrive il file in UTF8 senza BOM
    $content | Out-File -FilePath $path -Encoding utf8 -Force
    Write-Host "Creato / sovrascritto: $path"
}

# Cicla e crea i file
foreach ($name in $files.Keys) {
    $filePath = Join-Path $daoDir $name
    Write-FileWithBackup -path $filePath -content $files[$name]
}

Write-Host ""
Write-Host "Completato. Controlla i file in: $daoDir"
Write-Host "Ora: apri Android Studio, fai 'File -> Sync Project with Gradle Files' e poi 'Build -> Rebuild Project'."