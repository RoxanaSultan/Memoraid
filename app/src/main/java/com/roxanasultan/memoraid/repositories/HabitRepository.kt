package com.roxanasultan.memoraid.repositories

import com.roxanasultan.memoraid.models.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val habitsCollection get() = firestore.collection("habits")
    private val habitProgressCollection get() = firestore.collection("habits")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw SecurityException("User not authenticated")
    }

    suspend fun loadHabits(userId: String): List<Habit> {
//        val userId = requireUserId()
        return habitsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Habit::class.java)?.copy(id = it.id) }
    }

    suspend fun addHabit(habit: Habit): String? {
        val userId = requireUserId()

        val patientId = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            .getString("selectedPatient")

        if (patientId != null) {
            habit.userId = patientId
        }
        habit.checkedDates = arrayListOf()

        return try {
            val docRef = habitsCollection.add(habit).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateHabit(habit: Habit): Boolean {
        return try {
            if (habit.id != null) {
                habitsCollection.document(habit.id!!).set(habit).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteHabit(habitId: String): Boolean {
        return try {
            habitsCollection.document(habitId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateHabitCheckedDates(habitId: String, updatedDates: List<String>): Boolean {
        return try {
            habitsCollection.document(habitId)
                .update("checkedDates", updatedDates)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}