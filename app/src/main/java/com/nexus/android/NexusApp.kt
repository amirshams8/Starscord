package com.nexus.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannels(listOf(
                NotificationChannel("nexus_messages","Messages", NotificationManager.IMPORTANCE_HIGH).apply  { enableVibration(true) },
                NotificationChannel("nexus_mentions","Mentions", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("nexus_voice",   "Voice",   NotificationManager.IMPORTANCE_LOW),
                NotificationChannel("nexus_gateway", "Connection", NotificationManager.IMPORTANCE_MIN),
            ))
        }
    }
}
