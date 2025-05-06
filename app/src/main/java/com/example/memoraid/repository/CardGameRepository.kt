package com.example.memoraid.repository

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class CardGameRepository @Inject constructor(
    private val database: FirebaseFirestore
) {

    private val firebaseCollection = database.collection("card_matching_game")

    fun getCardGameLevels(userId: String, onSuccess: (List<Map<String, Any>>) -> Unit, onFailure: (Exception) -> Unit) {
        val levelsRef = firebaseCollection
            .whereEqualTo("userId", userId)

        levelsRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val levels = document.get("levels") as? List<Map<String, Any>> ?: emptyList()
                    onSuccess(levels)
                } else {
                    onFailure(Exception("No levels found"))
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun getTotalGameScore(userId: String, onSuccess: (Long) -> Unit, onFailure: (Exception) -> Unit) {
        val card_matching_game_ref = firebaseCollection
            .whereEqualTo("userId", userId)

        card_matching_game_ref.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val totalScore = document.get("totalScore") as? Long ?: 0
                    onSuccess(totalScore)
                } else {
                    onFailure(Exception("No game found"))
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun getBestScorePerLevel(
        userId: String,
        level: String,
        onSuccess: (Long) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bestScoreRef = firebaseCollection
            .whereEqualTo("userId", userId)

        bestScoreRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val levels = document.get("levels") as? List<Map<String, Any>> ?: emptyList()

                    var bestScore = 0L

                    // Căutăm nivelul dorit
                    for (levelMap in levels) {
                        val currentLevel = levelMap["level"] as? String
                        if (currentLevel == level) {
                            // Extragem lista lastPlayedGames
                            val lastPlayedGames = levelMap["lastPlayedGames"] as? List<Map<String, Any>> ?: emptyList()

                            // Căutăm cel mai mare totalLevelScore din lastPlayedGames
                            for (game in lastPlayedGames) {
                                val totalLevelScore = game["totalLevelScore"] as? Long ?: 0L
                                if (totalLevelScore > bestScore) {
                                    bestScore = totalLevelScore
                                }
                            }
                        }
                    }

                    if (bestScore > 0L) {
                        onSuccess(bestScore)
                    } else {
                        onFailure(Exception("No best score found for this level"))
                    }
                } else {
                    onFailure(Exception("No levels found"))
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun getTotalGamesPerLevel(
        userId: String,
        level: String,
        onSuccess: (Long) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val totalGamesRef = firebaseCollection
            .whereEqualTo("userId", userId)

        totalGamesRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val levels = document.get("levels") as? List<Map<String, Any>> ?: emptyList()

                    var totalGamesCount = 0L

                    // Căutăm nivelul dorit în array-ul levels
                    for (levelMap in levels) {
                        val currentLevel = levelMap["level"] as? String
                        if (currentLevel == level) {
                            // Extragem lista lastPlayedGames
                            val lastPlayedGames = levelMap["lastPlayedGames"] as? List<Map<String, Any>> ?: emptyList()

                            // Contorizăm câte jocuri au fost jucate
                            totalGamesCount = lastPlayedGames.size.toLong()
                            break  // Găsit nivelul, ieșim din for
                        }
                    }

                    if (totalGamesCount > 0L) {
                        onSuccess(totalGamesCount)
                    } else {
                        onFailure(Exception("No games found for this level"))
                    }
                } else {
                    onFailure(Exception("No levels found"))
                }
            }
            .addOnFailureListener(onFailure)
    }
}
