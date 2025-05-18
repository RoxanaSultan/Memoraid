package com.roxanasultan.memoraid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MemoraidApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createFullScreenChannel()
    }

    private fun createFullScreenChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "APPOINTMENT_CHANNEL",
                "Appointment Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for appointment reminders"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}