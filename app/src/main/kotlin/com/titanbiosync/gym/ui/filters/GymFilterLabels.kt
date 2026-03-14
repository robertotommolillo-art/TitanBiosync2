package com.titanbiosync.gym.ui.filters

object GymFilterLabels {

    fun equipmentLabel(value: String): String = when (value) {
        "barbell" -> "Bilanciere"
        "dumbbell" -> "Manubri"
        "machine" -> "Macchina"
        "cable" -> "Cavi"
        "bodyweight" -> "Corpo libero"
        "kettlebell" -> "Kettlebell"
        "band" -> "Elastico"
        "smith_machine" -> "Smith machine"
        "ez_bar" -> "EZ bar"
        "other" -> "Altro"
        else -> value
    }

    fun levelLabel(value: String): String = when (value) {
        "beginner" -> "Principiante"
        "intermediate" -> "Intermedio"
        "advanced" -> "Avanzato"
        else -> value
    }

    fun mechanicsLabel(value: String): String = when (value) {
        "compound" -> "Multiarticolare"
        "isolation" -> "Isolamento"
        else -> value
    }

    fun categoryLabel(value: String): String = when (value) {
        "bodybuilding" -> "Bodybuilding"
        "powerlifting" -> "Powerlifting"
        "weightlifting" -> "Weightlifting"
        "calisthenics" -> "Calisthenics"
        "mobility" -> "Mobilità"
        "stretching" -> "Stretching"
        "cardio" -> "Cardio"
        "rehab" -> "Riabilitazione"
        "other" -> "Altro"
        else -> value
    }
}