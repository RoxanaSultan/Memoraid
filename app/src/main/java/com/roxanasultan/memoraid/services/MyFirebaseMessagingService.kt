package com.roxanasultan.memoraid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.models.Appointment
import java.text.SimpleDateFormat
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "memoraid_channel"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("MyFirebaseMessagingService", "Message received: ${remoteMessage.data}")

        val type = remoteMessage.data["type"]
        val id = remoteMessage.data["id"]
        val isDeleted = remoteMessage.data["deleted"] == "true"
        val isUpdated = remoteMessage.data["updated"] == "true"
        val isAdded = remoteMessage.data["added"] == "true"

        Log.d("FCM", "type: $type, id: $id, deleted: $isDeleted, updated: $isUpdated, added: $isAdded")

        if (id.isNullOrEmpty()) return

        val firestore = FirebaseFirestore.getInstance()

        when (type) {
            "medication" -> {
                firestore.collection("medicine").document(id).get()
                    .addOnSuccessListener { document ->
                        val medicine = document.toObject(Medicine::class.java)
                        if (medicine != null) {
                            handleMedicationNotification(medicine, isDeleted, isUpdated, isAdded)
                        }
                    }
                    .addOnFailureListener {
                        Log.e("FCM", "Failed to fetch medicine $id", it)
                    }
            }

            "appointment" -> {
                firestore.collection("appointments").document(id).get()
                    .addOnSuccessListener { document ->
                        val appointment = document.toObject(Appointment::class.java)
                        if (appointment != null) {
                            handleAppointmentNotification(appointment, isDeleted, isUpdated, isAdded)
                        }
                    }
                    .addOnFailureListener {
                        Log.e("FCM", "Failed to fetch appointment $id", it)
                    }
            }
        }

        // Show Notification
        val title = remoteMessage.data["title"] ?: "Memoraid Notification"
        val body = remoteMessage.data["body"] ?: "Check your reminders."

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", remoteMessage.data["navigate_to"])
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        when (type) {
            "medication" -> {
                val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.medicine)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            }
            "appointment" -> {
                val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.appointment)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            }
        }
    }

    private fun handleMedicationNotification(medicine: Medicine, deleted: Boolean, updated: Boolean, added: Boolean) {
        Log.d("MyFirebaseMessagingService", "handleMedicationNotification called with medicine: ${medicine.name}, deleted: $deleted, updated: $updated, added: $added")
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date: Date? = try {
            formatter.parse(medicine.nextAlarm)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (date != null) {
            if (deleted) {
                Log.d("MyFirebaseMessagingService", "Cancelling alarm for deleted medication: ${medicine.name}")
                AlarmScheduler.cancelAlarmForMedication(this, medicine, date)
                medicine.nextAlarm = null
            } else if (updated) {
                Log.d("MyFirebaseMessagingService", "Updating alarm for medication: ${medicine.name}")
                AlarmScheduler.cancelAlarmForMedication(this, medicine, date)
                val nextDate = getNextDateForMedication(medicine, date)
                Log.d("MyFirebaseMessagingService", "Next date for medication ${medicine.name}: $nextDate")
                nextDate?.let { AlarmScheduler.scheduleAlarmForMedication(this, medicine, it) }
                medicine.nextAlarm = nextDate?.let { formatter.format(it) }
            } else if (added) {
                Log.d("MyFirebaseMessagingService", "Scheduling alarm for added medication: ${medicine.name}")
                AlarmScheduler.scheduleAlarmForMedication(this, medicine, date)
            }
        }
    }

    private fun handleAppointmentNotification(appointment: Appointment, deleted: Boolean, updated: Boolean, added: Boolean) {
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

        if (deleted) {
            Log.d("MyFirebaseMessagingService", "Cancelling alarms for deleted appointment: ${appointment.name}")
            appointment.nextAlarms?.forEach { dateTimeString ->
                val parsedDate = try {
                    formatter.parse(dateTimeString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (parsedDate != null) {
                    AlarmScheduler.cancelAlarmForAppointment(this, appointment, parsedDate)
                }
            }

        } else if (updated) {
            Log.d("MyFirebaseMessagingService", "Updating alarms for appointment: ${appointment.name}")

            appointment.nextAlarms?.forEach { dateTimeString ->
                val parsedDate = try {
                    formatter.parse(dateTimeString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (parsedDate != null) {
                    AlarmScheduler.cancelAlarmForAppointment(this, appointment, parsedDate)
                }
            }

            val baseDate = try {
                formatter.parse("${appointment.date} ${appointment.time}")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val newAlarms = mutableListOf<String>()
            baseDate?.let {
                val cal = Calendar.getInstance()

                cal.time = it
                newAlarms.add(formatter.format(cal.time))

                cal.time = it
                cal.add(Calendar.HOUR_OF_DAY, -1)
                newAlarms.add(formatter.format(cal.time))

                cal.time = it
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 20)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                newAlarms.add(formatter.format(cal.time))
            }

            newAlarms.forEach { dateTimeString ->
                val parsedDate = try {
                    formatter.parse(dateTimeString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (parsedDate != null) {
                    AlarmScheduler.scheduleAlarmForAppointment(this, appointment, parsedDate)
                }
            }

            FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appointment.id)
                .update("nextAlarms", newAlarms)
                .addOnSuccessListener {
                    Log.d("MyFirebaseMessagingService", "nextAlarms updated in Firestore")
                }
                .addOnFailureListener {
                    Log.e("MyFirebaseMessagingService", "Failed to update nextAlarms", it)
                }

        } else if (added) {
            Log.d("MyFirebaseMessagingService", "Scheduling alarms for added appointment: ${appointment.name}")
            appointment.nextAlarms?.forEach { dateTimeString ->
                val parsedDate = try {
                    formatter.parse(dateTimeString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (parsedDate != null) {
                    AlarmScheduler.scheduleAlarmForAppointment(this, appointment, parsedDate)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Memoraid Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication and appointment notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun getNextDateForMedication(medicine: Medicine, date: Date): Date? {
        var nextDate: Date? = null

        val calendar = Calendar.getInstance().apply {
            time = date
        }

        fun dayOfWeekFromString(day: String): Int {
            return when (day.lowercase(Locale.getDefault())) {
                "sunday" -> Calendar.SUNDAY
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                else -> Calendar.MONDAY // default
            }
        }

        when (medicine.frequency) {
            "Daily" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                nextDate = calendar.time
            }
            "Weekly" -> {
                // Avem o listă de zile (ex: ["Monday", "Wednesday"])
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weeklyDays = medicine.weeklyDays ?: return date

                // Convertim zilele în numere Calendar.DAY_OF_WEEK
                val daysOfWeek = weeklyDays.map { dayOfWeekFromString(it) }

                // Căutăm prima zi din săptămână din daysOfWeek care este după azi
                val sortedDays = daysOfWeek.sorted()
                for (day in sortedDays) {
                    if (day > todayDayOfWeek) {
                        val daysToAdd = day - todayDayOfWeek
                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        return calendar.time
                    }
                }
                // Dacă nu am găsit niciuna după azi, alegem prima zi din lista săptămânii de săptămâna viitoare
                val firstDay = sortedDays.first()
                val daysToAdd = 7 - todayDayOfWeek + firstDay
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                nextDate = calendar.time
            }
            "Every X days" -> {
                val x = medicine.everyXDays ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, x)
                nextDate = calendar.time
            }
            "Monthly" -> {
                val monthlyDay = medicine.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                // Mutăm luna la următoarea lună
                calendar.add(Calendar.MONTH, 1)
                // Setăm ziua din lună la cea dorită
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceAtMost(maxDay))
                nextDate = calendar.time
            }
            else -> {
                return null
            }
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(nextDate)

        if (medicine.skippedDates != null && medicine.skippedDates.contains(dateAsString))
        {
            return getNextDateForMedication(medicine, nextDate)
        } else if (medicine.endDate != null && dateAsString >= medicine.endDate){
            return null
        }
        else {
            return nextDate
        }
    }

    fun getNextDateForAppointment(appointment: Appointment, date: Date): Date? {
        var nextDate: Date? = null

        val calendar = Calendar.getInstance().apply {
            time = date
        }

        fun dayOfWeekFromString(day: String): Int {
            return when (day.lowercase(Locale.getDefault())) {
                "sunday" -> Calendar.SUNDAY
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                else -> Calendar.MONDAY // default
            }
        }

        when (appointment.frequency) {
            "Daily" -> {
                // Adaugă o zi
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                nextDate = calendar.time
            }
            "Weekly" -> {
                // Avem o listă de zile (ex: ["Monday", "Wednesday"])
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weeklyDays = appointment.weeklyDays ?: return date // fallback

                // Convertim zilele în numere Calendar.DAY_OF_WEEK
                val daysOfWeek = weeklyDays.map { dayOfWeekFromString(it) }

                // Căutăm prima zi din săptămână din daysOfWeek care este după azi
                val sortedDays = daysOfWeek.sorted()
                for (day in sortedDays) {
                    if (day > todayDayOfWeek) {
                        val daysToAdd = day - todayDayOfWeek
                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        return calendar.time
                    }
                }
                // Dacă nu am găsit niciuna după azi, alegem prima zi din lista săptămânii de săptămâna viitoare
                val firstDay = sortedDays.first()
                val daysToAdd = 7 - todayDayOfWeek + firstDay
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                nextDate = calendar.time
            }
            "Every X days" -> {
                val x = appointment.everyXDays ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, x)
                nextDate = calendar.time
            }
            "Monthly" -> {
                val monthlyDay = appointment.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                // Mutăm luna la următoarea lună
                calendar.add(Calendar.MONTH, 1)
                // Setăm ziua din lună la cea dorită
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceAtMost(maxDay))
                nextDate = calendar.time
            }
            else -> {
                return null
            }
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(nextDate)

        if (appointment.skippedDates != null && appointment.skippedDates.contains(dateAsString))
        {
            return getNextDateForAppointment(appointment, nextDate)
        } else if (appointment.endDate != null && dateAsString >= appointment.endDate){
            return null
        }
        else {
            return nextDate
        }
    }
}