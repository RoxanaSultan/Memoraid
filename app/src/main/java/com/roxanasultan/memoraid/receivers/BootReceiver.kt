package com.roxanasultan.memoraid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roxanasultan.memoraid.helpers.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
            val hour = prefs.getInt("hour", -1)
            val minute = prefs.getInt("minute", -1)
            val dose = prefs.getString("dose", null)

            if (hour != -1 && minute != -1 && dose != null) {
                AlarmScheduler.scheduleAlarm(context, hour, minute, dose)
            }
        }
    }
}