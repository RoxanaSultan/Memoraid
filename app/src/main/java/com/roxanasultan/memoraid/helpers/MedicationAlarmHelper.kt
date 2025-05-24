package com.roxanasultan.memoraid.helpers

import android.content.Context

object MedicationAlarmHelper {
    private const val PREFS_NAME = "medication_prefs"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_TITLE = "title"
    private const val KEY_BODY = "body"

    fun saveReminder(context: Context, hour: Int, minute: Int, title: String, body: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putString(KEY_TITLE, title)
            .putString(KEY_BODY, body)
            .apply()
    }
}