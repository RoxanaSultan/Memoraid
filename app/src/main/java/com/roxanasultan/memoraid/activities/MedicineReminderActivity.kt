package com.roxanasultan.memoraid.activities

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.ActivityPillReminderBinding

class MedicineReminderActivity : AppCompatActivity() {

    private var _binding: ActivityPillReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        _binding = ActivityPillReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("MedicineReminderActivity", "Activity launched")

        val name = intent.getStringExtra("name") ?: "Unknown Medication"
        val dose = intent.getStringExtra("dose") ?: "Unknown Dose"
        val time = intent.getStringExtra("time") ?: "Unknown Time"
        val date = intent.getStringExtra("date") ?: "Unknown Date"
        val note = intent.getStringExtra("note") ?: "No additional notes"

        binding.medication.text = name
        binding.dose.text = dose
        binding.time.text = time
        binding.date.text = date
        binding.note.text = note

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Memoraid:WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, 0)
        }
        Log.d("MedicineReminderActivity", "Vibration started")

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
        _binding = null
    }
}