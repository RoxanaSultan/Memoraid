package com.example.memoraid.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class DoctorAppointment(
    @ServerTimestamp val date: Date? = null, // Firestore server timestamp for date
    val hour: String,
    val location: String,
    val doctorName: String
) : ReminderDetails()
