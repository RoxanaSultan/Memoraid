package com.example.memoraid.models

import java.text.SimpleDateFormat
import java.util.Locale

data class Appointment(
    var id: String = "",
    val name: String = "",
    val doctor: String? = null,
    val time: String = "",  // Format: "hh:mm" (e.g., "14:30")
    val location: String = "",
    val type: String = "",
    val date: String = "",  // Format: "dd-MM-yyyy" (e.g., "15-08-2023")
    val userId: String = "",
    var isCompleted: Boolean = false,
    var alarms: ArrayList<Map<String, String>> = arrayListOf()
) {
    // Helper function to get the timestamp in milliseconds
    fun getAlarmTimeInMillis(): Long {
        return try {
            val dateTimeString = "$date $time"
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                .parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}