package com.roxanasultan.memoraid.models

import com.google.firebase.firestore.DocumentId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Medicine(
    @DocumentId val id: String = "",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val dose: String = "",
    val note: String = "",
    var userId: String = "",
    var taken: Boolean = false,
    var hasAlarm: Boolean = false,
    val frequency: String = "Once",
    val everyXDays: Int? = null,
    val weeklyDays: List<String>? = null,
    val monthlyDay: Int? = null
) {
    fun isActiveOnDate(targetDate: Date?): Boolean {
        if (targetDate == null) return false

        val calendar = Calendar.getInstance()
        calendar.time = targetDate
        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.uppercase()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        return when (frequency) {
            "Once" -> {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val medDate = date?.let { sdf.parse(it) }
                medDate?.let { it == targetDate } ?: false
            }

            "Daily" -> true

            "Every X days" -> {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val startDate = date?.let { sdf.parse(it) } ?: return false
                if (everyXDays == null) return false
                val diffDays = ((targetDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt()
                diffDays >= 0 && diffDays % everyXDays == 0
            }

            "Weekly" -> {
                dayOfWeek != null && weeklyDays?.contains(dayOfWeek) == true
            }

            "Monthly" -> {
                monthlyDay != null && monthlyDay == dayOfMonth
            }

            else -> false
        }
    }
}