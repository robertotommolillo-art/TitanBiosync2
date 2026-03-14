package com.titanbiosync.domain.usecase

/**
 * Base interface per tutti i Use Cases.
 * Un Use Case rappresenta un singolo caso d'uso business.
 *
 * @param Params I parametri di input per il use case
 * @param Result Il tipo di ritorno del use case
 */
interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

/**
 * Use Case senza parametri
 */
interface NoParamsUseCase<out Result> {
    suspend operator fun invoke(): Result
}

/**
 * Use Case che ritorna un Flow (per osservare dati in tempo reale)
 */
interface FlowUseCase<in Params, out Result> {
    operator fun invoke(params: Params): Result
}