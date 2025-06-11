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
import androidx.appcompat.app.AppCompatActivity
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.ActivityAppointmentReminderBinding

class AppointmentReminderActivity  : AppCompatActivity() {

    private var _binding: ActivityAppointmentReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAppointmentReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("AppointmentReminderActivity", "Activity launched")

        val name = intent.getStringExtra("name") ?: "Unknown Appointment"
        val doctor = intent.getStringExtra("doctor") ?: "Unknown Doctor"
        val time = intent.getStringExtra("time") ?: "Unknown Time"
        val date = intent.getStringExtra("date") ?: "Unknown Date"
        val location = intent.getStringExtra("location") ?: "Unknown Location"

        binding.appointment.text = name
        binding.doctor.text = doctor
        binding.time.text = time
        binding.date.text = date
        binding.location.text = location

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
        Log.d("AppointmentReminderActivity", "Vibration started")

        try {
            mediaPlayer = MediaPlayer().apply {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setDataSource(this@AppointmentReminderActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d("AppointmentReminderActivity", "Alarm sound started")
        } catch (e: Exception) {
            Log.e("AppointmentReminderActivity", "Error starting alarm sound", e)
        }

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }
    }

    private fun stopAlarm() {
        Log.d("AppointmentReminderActivity", "Alarm dismissed")

        try {
            if (::mediaPlayer.isInitialized) {
                try {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                    }
                } catch (e: IllegalStateException) {
                    Log.w("AppointmentReminderActivity", "MediaPlayer is in illegal state during stop", e)
                } finally {
                    mediaPlayer.release()
                }
            }
        } catch (e: Exception) {
            Log.e("AppointmentReminderActivity", "Error stopping media player", e)
        }

        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
            Log.d("AppointmentReminderActivity", "WakeLock released")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        _binding = null
    }
}