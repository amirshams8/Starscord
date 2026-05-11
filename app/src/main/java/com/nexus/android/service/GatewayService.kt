package com.nexus.android.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexus.android.BuildConfig
import com.nexus.android.MainActivity
import com.nexus.android.data.gateway.GatewayClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GatewayService : Service() {
    @Inject lateinit var gatewayClient: GatewayClient
    companion object { const val EXTRA_TOKEN = "token"; const val NOTIFICATION_ID = 1001 }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra(EXTRA_TOKEN) ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification())
        gatewayClient.connect(BuildConfig.GATEWAY_WSS_URL, token)
        return START_STICKY
    }

    override fun onDestroy() { gatewayClient.disconnect(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "nexus_gateway")
            .setContentTitle("Nexus").setContentText("Connected")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi).setOngoing(true).setPriority(NotificationCompat.PRIORITY_MIN).build()
    }
}
