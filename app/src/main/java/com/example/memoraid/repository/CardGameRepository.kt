package com.example.memoraid.repository

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class CardGameRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getCardGameLevels(userId: String, onSuccess: (List<Map<String, Any>>) -> Unit, onFailure: (Exception) -> Unit) {
        val levelsRef = firestore.collection("card_matching_game")
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
        val card_matching_game_ref = firestore.collection("card_matching_game")
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
}
