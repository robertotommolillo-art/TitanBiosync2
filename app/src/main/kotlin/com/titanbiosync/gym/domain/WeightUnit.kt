package com.titanbiosync.gym.domain

enum class WeightUnit(val key: String) {
    KG("kg"),
    LB("lb");

    companion object {
        fun fromKey(key: String?): WeightUnit =
            entries.firstOrNull { it.key == key } ?: KG
    }
}