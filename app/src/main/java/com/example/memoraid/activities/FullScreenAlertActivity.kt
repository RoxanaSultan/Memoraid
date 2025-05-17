package com.example.memoraid.activities

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import com.example.memoraid.R

class FullScreenAlertActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen flags
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_full_screen_alert)

        val title = intent.getStringExtra("title") ?: "Appointment Alert"
        val message = intent.getStringExtra("message") ?: "You have an appointment now!"

        findViewById<TextView>(R.id.alertTitle).text = title
        findViewById<TextView>(R.id.alertMessage).text = message
    }
}