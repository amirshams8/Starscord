package com.nexus.android.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexus.android.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoiceService : Service() {
    companion object { const val NOTIFICATION_ID = 1002 }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, "nexus_voice")
            .setContentTitle("Nexus Voice").setContentText("In a voice channel")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
