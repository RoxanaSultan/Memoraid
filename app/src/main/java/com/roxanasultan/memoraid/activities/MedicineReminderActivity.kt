package com.roxanasultan.memoraid.activities

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.roxanasultan.memoraid.R

class MedicineReminderActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pill_reminder)

        Log.d("MedicineReminderActivity", "Activity launched")

        // Obține doza din intent
        val dose = intent.getStringExtra("dose") ?: "medicamentul"

        // Setează textul în UI dacă ai TextView pentru afișarea dozei
        findViewById<TextView>(R.id.tvReminderDesc)?.text = "Este ora să iei: $dose"

        // Activează ecranul dacă este oprit (WakeLock)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Memoraid:WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // Trezește telefonul pentru 10 minute
        Log.d("MedicineReminderActivity", "WakeLock activated")

        // Asigură că ecranul rămâne aprins și activitatea vizibilă
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Pornire vibrații
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = longArrayOf(0, 1000, 1000)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, 0)
        }
        Log.d("MedicineReminderActivity", "Vibration started")

        // Pornire alarmă
        try {
            mediaPlayer = MediaPlayer().apply {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setDataSource(this@MedicineReminderActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d("MedicineReminderActivity", "Alarm sound started")
        } catch (e: Exception) {
            Log.e("MedicineReminderActivity", "Error starting alarm sound", e)
        }

        // Buton pentru oprirea alarmei
        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }
    }

    private fun stopAlarm() {
        Log.d("MedicineReminderActivity", "Alarm dismissed")

        try {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            Log.e("MedicineReminderActivity", "Error stopping media player", e)
        }

        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
            Log.d("MedicineReminderActivity", "WakeLock released")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}