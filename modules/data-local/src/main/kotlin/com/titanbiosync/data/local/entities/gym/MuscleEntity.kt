package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "gym_muscles")
data class MuscleEntity(
    @PrimaryKey val id: String,
    val nameIt: String,
    val nameEn: String
)