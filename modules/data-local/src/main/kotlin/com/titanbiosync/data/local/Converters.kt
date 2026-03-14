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