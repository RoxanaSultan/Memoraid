package com.example.memoraid.models

data class PillReminder(
    val hour: String,
    val frequency: String, // e.g., "daily", "weekly"
    val name: String
) : ReminderDetails()
