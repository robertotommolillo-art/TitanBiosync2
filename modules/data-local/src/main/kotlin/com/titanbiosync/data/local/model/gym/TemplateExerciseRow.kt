package com.titanbiosync.data.local.model.gym

data class TemplateExerciseRow(
    val position: Int,
    val exerciseId: String,
    val nameIt: String,
    val nameEn: String,

    // v10+ (nullable per retrocompat)
    val supersetGroupId: String? = null,
    val supersetOrder: Int? = null
)