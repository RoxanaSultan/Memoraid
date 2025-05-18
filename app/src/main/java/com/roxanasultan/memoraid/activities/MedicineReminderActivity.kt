package com.roxanasultan.memoraid.activities

import com.roxanasultan.memoraid.R
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MedicineReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pill_reminder)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            finish()
        }
    }
}
