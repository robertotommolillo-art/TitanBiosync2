package com.titanbiosync.gym.domain

object WeightUnitConverter {
    private const val KG_PER_LB = 0.45359237f

    fun kgToDisplay(kg: Float, unit: WeightUnit): Float =
        when (unit) {
            WeightUnit.KG -> kg
            WeightUnit.LB -> kg / KG_PER_LB
        }

    fun displayToKg(value: Float, unit: WeightUnit): Float =
        when (unit) {
            WeightUnit.KG -> value
            WeightUnit.LB -> value * KG_PER_LB
        }
}