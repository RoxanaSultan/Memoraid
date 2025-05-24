package com.roxanasultan.memoraid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roxanasultan.memoraid.activities.MedicineReminderActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        val dose = intent.getStringExtra("dose") ?: "medicamentul"

        // Pornește MedicineReminderActivity în loc de notificare simplă
        val reminderIntent = Intent(context, MedicineReminderActivity::class.java).apply {
            putExtra("dose", dose)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        context.startActivity(reminderIntent)
        Log.d("AlarmReceiver", "MedicineReminderActivity started with dose: $dose")
    }
}