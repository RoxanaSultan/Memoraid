package com.example.memoraid

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MemoraidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
