package com.example.memoraid.models

data class Reminder(
    val id: String = "",
    val patientId: String = "", // Reference to the Patient ID
    val type: String = "", // Either "doctorAppointment" or "pillReminder"
    val details: ReminderDetails? = null // Generic details object
)

sealed class ReminderDetails {
    data class DoctorAppointment(
        val date: Long, // Timestamp
        val hour: String,
        val location: String,
        val doctorName: String
    ) : ReminderDetails()

    data class PillReminder(
        val hour: String,
        val frequency: String, // e.g., "daily", "weekly"
        val name: String
    ) : ReminderDetails()
}
