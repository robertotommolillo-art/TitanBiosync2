package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gym_exercises",
    indices = [
        Index(value = ["nameIt"]),
        Index(value = ["nameEn"]),
        Index(value = ["category"]),
        Index(value = ["equipment"]),
        Index(value = ["mechanics"]),
        Index(value = ["level"]),
        Index(value = ["archivedAt"]),
        Index(value = ["isCustom"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,

    val nameIt: String,
    val nameEn: String,

    val descriptionIt: String? = null,
    val descriptionEn: String? = null,

    // <-- prima era obbligatorio: ora ha default
    val category: String = "bodybuilding",

    val equipment: String? = null,
    val mechanics: String? = null,
    val level: String? = null,

    val isCustom: Boolean = false,
    val createdAt: Long = 0L,
    val archivedAt: Long? = null
)