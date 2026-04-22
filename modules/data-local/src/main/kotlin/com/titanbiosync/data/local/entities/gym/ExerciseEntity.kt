package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
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

    val name: String? = null,

    @SerialName("name_it")
    val nameIt: String? = null,

    @SerialName("name_en")
    val nameEn: String? = null,

    @SerialName("gif_file")
    val gifFile: String? = null,

    @SerialName("body_part")
    val bodyPart: String? = null,

    @SerialName("body_part_it")
    val bodyPartIt: String? = null,

    val equipment: String? = null,

    @SerialName("equipment_it")
    val equipmentIt: String? = null,

    val target: String? = null,

    @SerialName("target_it")
    val targetIt: String? = null,

    @SerialName("description_it")
    val descriptionIt: String? = null,

    @SerialName("description_en")
    val descriptionEn: String? = null,

    @SerialName("instructions_en")
    val instructionsEn: String? = null,

    @SerialName("instructions_it")
    val instructionsIt: String? = null,

    val category: String? = "bodybuilding",
    val mechanics: String? = null,
    val level: String? = null,

    val isCustom: Boolean = false,
    val createdAt: Long = 0L,
    val archivedAt: Long? = null
)