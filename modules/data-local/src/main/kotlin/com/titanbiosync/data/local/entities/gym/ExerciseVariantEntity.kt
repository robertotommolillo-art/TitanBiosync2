package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gym_exercise_variants",
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["nameIt"]),
        Index(value = ["nameEn"]),
        Index(value = ["level"]),
        Index(value = ["equipment"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseVariantEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,

    val nameIt: String,
    val nameEn: String,

    val descriptionIt: String? = null,
    val descriptionEn: String? = null,

    val equipment: String? = null,
    val mechanics: String? = null,
    val level: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)