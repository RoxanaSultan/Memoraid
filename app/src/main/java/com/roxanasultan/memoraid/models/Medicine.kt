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
    val monthlyDay: Int? = null,
    val skippedDates: List<String>? = null,
    val endDate: String? = null,
    var nextAlarm: String? = null,
) {
    fun isActiveOnDate(targetDate: Date?): Boolean {
        if (targetDate == null || date.isBlank()) return false

        val calendar = Calendar.getInstance()
        calendar.time = targetDate
        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.uppercase()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedTargetDate = sdf.format(targetDate)

        val parsedStartDate = try {
            sdf.parse(date)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        if (parsedStartDate == null || targetDate.before(parsedStartDate)) return false

        if (skippedDates?.contains(formattedTargetDate) == true) return false

        endDate?.let { end ->
            val endParsed = try {
                sdf.parse(end)
            } catch (e: Exception) {
                null
            }
            if (endParsed != null && !targetDate.before(endParsed)) return false
        }

        return when (frequency) {
            "Once" -> parsedStartDate == targetDate
            "Daily" -> true
            "Every X days" -> {
                if (everyXDays == null) return false
                val diffDays = ((targetDate.time - parsedStartDate.time) / (1000 * 60 * 60 * 24)).toInt()
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