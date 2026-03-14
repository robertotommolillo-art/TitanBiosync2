# create_all_local_files.ps1
# Script semplificato che scrive i file essenziali nel modulo modules/data-local.
# Salva questo file nella root del progetto e poi eseguilo:
# powershell -ExecutionPolicy Bypass -File .\create_all_local_files.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$baseDir = Join-Path $root "modules\data-local\src\main\kotlin\com\titanbiosync\data\local"

function Ensure-Dir($path) {
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
        Write-Host "Creato: $path"
    } else {
        Write-Host "Esiste: $path"
    }
}

function Write-File-WithBackup($fullPath, $content) {
    if (Test-Path $fullPath) {
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $backup = "$fullPath.bak_$timestamp"
        Copy-Item -Path $fullPath -Destination $backup -Force
        Write-Host "Backup creato: $backup"
    }
    $dir = Split-Path $fullPath -Parent
    Ensure-Dir $dir
    $content | Out-File -FilePath $fullPath -Encoding utf8 -Force
    Write-Host "Scritto: $fullPath"
}

# Crea le directory principali
Ensure-Dir $baseDir
Ensure-Dir (Join-Path $baseDir "entities")
Ensure-Dir (Join-Path $baseDir "dao")
Ensure-Dir (Join-Path $baseDir "migrations")
Ensure-Dir (Join-Path $baseDir "di")

# 1) Converters.kt
$path = Join-Path $baseDir "Converters.kt"
$content = @'
package com.titanbiosync.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.util.UUID

object RoomJson {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun mapToJson(value: Map<String, Float>?): String? =
        value?.let { RoomJson.json.encodeToString(it) }

    @TypeConverter
    fun jsonToMap(value: String?): Map<String, Float>? =
        value?.let { RoomJson.json.decodeFromString(it) }

    @TypeConverter
    fun listToJson(value: List<String>?): String? =
        value?.let { RoomJson.json.encodeToString(it) }

    @TypeConverter
    fun jsonToList(value: String?): List<String>? =
        value?.let { RoomJson.json.decodeFromString(it) }

    @TypeConverter
    fun enumToString(value: Enum<*>?): String? = value?.name
}
'@
Write-File-WithBackup $path $content

# 2) AppDatabase.kt
$path = Join-Path $baseDir "AppDatabase.kt"
$content = @'
package com.titanbiosync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.titanbiosync.data.local.dao.*
import com.titanbiosync.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        SensorReadingEntity::class,
        SessionEntity::class,
        HealthMetricsEntity::class,
        RecommendationEntity::class,
        CoachPromptEntity::class,
        MapLocationEntity::class,
        ConsentRecordEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(com.titanbiosync.data.local.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun sessionDao(): SessionDao
    abstract fun healthMetricsDao(): HealthMetricsDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun coachPromptDao(): CoachPromptDao
    abstract fun mapLocationDao(): MapLocationDao
    abstract fun consentDao(): ConsentDao
    abstract fun appConfigDao(): AppConfigDao
}
'@
Write-File-WithBackup $path $content

# 3) MigrationSamples.kt
$path = Join-Path $baseDir "migrations\MigrationSamples.kt"
$content = @'
package com.titanbiosync.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sessions ADD COLUMN aggregated_metrics_json TEXT")
        Log.i("MIGRATION", "Applied MIGRATION_1_2: added aggregated_metrics_json")
    }
}
'@
Write-File-WithBackup $path $content

# 4) DatabaseProvider.kt (senza Hilt)
$path = Join-Path $baseDir "di\DatabaseProvider.kt"
$content = @'
package com.titanbiosync.data.local.di

import android.content.Context
import androidx.room.Room
import com.titanbiosync.data.local.AppDatabase
import com.titanbiosync.data.local.migrations.MIGRATION_1_2

/**
 * Simple provider senza Hilt.
 * Usa questa versione temporanea se non hai ancora configurato Hilt.
 *
 * Per ottenere l'istanza del DB:
 * val db = DatabaseProvider.create(context)
 */
object DatabaseProvider {
    fun create(appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "titanbiosync.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
'@
Write-File-WithBackup $path $content

# 5) DatabaseModule_Hilt.kt (commentato, pronto per Hilt)
$path = Join-Path $baseDir "di\DatabaseModule_Hilt.kt"
$content = @'
package com.titanbiosync.data.local.di

/* 
  Questo file è un esempio per usare Hilt. 
  Decommentalo e aggiungi Hilt alle dipendenze quando vuoi usarlo.

import android.content.Context
import androidx.room.Room
import com.titanbiosync.data.local.AppDatabase
](#)
