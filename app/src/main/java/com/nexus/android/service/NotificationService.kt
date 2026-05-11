package com.nexus.android.service

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexus.android.MainActivity

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data    = remoteMessage.data
        val title   = data["title"]   ?: remoteMessage.notification?.title ?: "Nexus"
        val body    = data["body"]    ?: remoteMessage.notification?.body  ?: ""
        val channel = data["channel"] ?: "nexus_messages"
        val intent  = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            data["guild_id"]?.let   { putExtra("guild_id",   it) }
            data["channel_id"]?.let { putExtra("channel_id", it) }
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, channel)
                .setContentTitle(title).setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setAutoCancel(true).setContentIntent(pi).build()
        )
    }

    override fun onNewToken(token: String) {
        // TODO: POST /users/@me/fcm-token
    }
}
