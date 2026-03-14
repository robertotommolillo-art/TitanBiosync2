package com.titanbiosync.gym.online.data

import com.titanbiosync.gym.online.model.OnlineExerciseCandidate
import com.titanbiosync.gym.online.model.OnlineExerciseResolved

interface OnlineExerciseDataSource {
    suspend fun search(query: String, lang: String = "it"): List<OnlineExerciseCandidate>
    suspend fun resolve(candidateId: String, lang: String = "it"): OnlineExerciseResolved
}