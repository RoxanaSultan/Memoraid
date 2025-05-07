package com.example.memoraid.repository

import com.example.memoraid.models.CardGame
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class CardGameRepository @Inject constructor(
    private val database: FirebaseFirestore
) {
    private val firebaseCollection = database.collection("card_matching_game")

    suspend fun loadLastPlayedGames(userId: String): List<CardGame> {
        val result = mutableListOf<CardGame>()

        val querySnapshot = firebaseCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()

        if (querySnapshot.isEmpty) return result

        val document = querySnapshot.documents[0]
        val levels = document.get("levels") as? List<Map<String, Any>> ?: return result

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val startOfLastWeek = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfLastWeek = calendar.time

        for (levelMap in levels) {
            val lastPlayedGames = levelMap["lastPlayedGames"] as? List<Map<String, Any>> ?: continue

            for (game in lastPlayedGames) {
                val timestamp = game["date"] as? com.google.firebase.Timestamp ?: continue
                val date = timestamp.toDate()

                if (date >= startOfLastWeek && date <= endOfLastWeek) {
                    val dayOfWeekName = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date)

                    val score = (game["totalLevelScore"] as? Long ?: (game["totalLevelScore"] as? Double)?.toLong()) ?: 0L
                    val moves = (game["moves"] as? Long ?: (game["moves"] as? Double)?.toLong()) ?: 0L
                    val time = (game["time"] as? Long ?: (game["time"] as? Double)?.toLong()) ?: 0L

                    val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)

                    result.add(
                        CardGame(
                            dayOfWeek = dayOfWeekName,
                            date = dateStr,
                            moves = moves.toString(),
                            time = time.toString(),
                            score = score.toString(),
                            startTime = startTime
                        )
                    )
                }
            }
        }

        return result
    }

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

                    for (levelMap in levels) {
                        val currentLevel = levelMap["level"] as? String
                        if (currentLevel == level) {
                            val lastPlayedGames = levelMap["lastPlayedGames"] as? List<Map<String, Any>> ?: emptyList()

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
