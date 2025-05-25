package com.roxanasultan.memoraid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.roxanasultan.memoraid.services.LocationForegroundService

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (ActivityRecognitionResult.hasResult(it)) {
                val result = ActivityRecognitionResult.extractResult(it)!!
                val mostProbableActivity = result.mostProbableActivity
                val activityType = mostProbableActivity.type
                val confidence = mostProbableActivity.confidence

                Log.d("ActivityRecognition", "Activity: $activityType, confidence: $confidence")

                val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                    putExtra("activity_type", activityType)
                }
                context?.startService(serviceIntent)
            }
        }
    }
}