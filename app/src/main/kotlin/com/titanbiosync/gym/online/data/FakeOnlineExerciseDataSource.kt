package com.titanbiosync.gym.online.data

import com.titanbiosync.gym.online.model.OnlineExerciseCandidate
import com.titanbiosync.gym.online.model.OnlineExerciseResolved
import kotlinx.coroutines.delay

class FakeOnlineExerciseDataSource : OnlineExerciseDataSource {

    override suspend fun search(query: String, lang: String): List<OnlineExerciseCandidate> {
        delay(300) // simula rete
        val q = query.trim().lowercase()

        if (q.isBlank()) return emptyList()

        // Esempio: “croci ai cavi panca inclinata”
        if (q.contains("croci") && q.contains("cavi")) {
            return listOf(
                OnlineExerciseCandidate(
                    candidateId = "fake_1",
                    title = "Croci ai cavi su panca inclinata",
                    subtitle = "Variante delle croci ai cavi con focus su alto petto",
                    sourceName = "AI (stub)",
                    sourceUrl = null,
                    confidence = 0.72f
                ),
                OnlineExerciseCandidate(
                    candidateId = "fake_2",
                    title = "Incline cable fly (bench)",
                    subtitle = "Cable fly performed on an incline bench",
                    sourceName = "AI (stub)",
                    sourceUrl = null,
                    confidence = 0.64f
                )
            )
        }

        return listOf(
            OnlineExerciseCandidate(
                candidateId = "fake_generic",
                title = query.trim(),
                subtitle = "Risultato generato (stub) da confermare",
                sourceName = "AI (stub)",
                sourceUrl = null,
                confidence = 0.50f
            )
        )
    }

    override suspend fun resolve(candidateId: String, lang: String): OnlineExerciseResolved {
        delay(300)

        return when (candidateId) {
            "fake_1" -> OnlineExerciseResolved(
                nameIt = "Croci ai cavi su panca inclinata",
                nameEn = "Incline cable fly (bench)",
                descriptionIt = "Sdraiati su panca inclinata. Impugna i cavi e unisci le mani sopra il petto mantenendo gomiti leggermente flessi. Ritorna controllando.",
                descriptionEn = "Lie on an incline bench. Grab the cables and bring hands together over the chest with a slight elbow bend. Return under control.",
                category = "chest",
                equipment = "cable",
                mechanics = "isolation",
                level = "beginner",
                sourceName = "AI (stub)",
                muscles = listOf(
                    OnlineExerciseResolved.MuscleLink(muscleId = "chest", role = "primary", weight = 1.0f),
                    OnlineExerciseResolved.MuscleLink(muscleId = "anterior_deltoid", role = "secondary", weight = 0.6f),
                    OnlineExerciseResolved.MuscleLink(muscleId = "triceps", role = "secondary", weight = 0.4f)
                )
            )

            else -> OnlineExerciseResolved(
                nameIt = "Esercizio personalizzato",
                nameEn = "Custom exercise",
                descriptionIt = "Descrizione da completare.",
                descriptionEn = "Description to be completed.",
                category = "other",
                equipment = null,
                mechanics = null,
                level = null,
                sourceName = "AI (stub)",
                muscles = emptyList()
            )
        }
    }
}