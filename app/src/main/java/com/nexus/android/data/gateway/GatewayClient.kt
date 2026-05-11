package com.nexus.android.data.gateway

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GatewayEvent(val type: String, val data: JSONObject)

@Singleton
class GatewayClient @Inject constructor() {
    private val _events = MutableSharedFlow<GatewayEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    private var ws: WebSocket? = null
    private var token: String? = null
    private var seq = 0
    private var heartbeatInterval = 41250L
    private var heartbeatThread: Thread? = null

    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(30, TimeUnit.SECONDS).build()

    fun connect(gatewayUrl: String, authToken: String) {
        token = authToken
        ws = client.newWebSocket(Request.Builder().url("$gatewayUrl?v=10&encoding=json").build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, r: Response) { Log.d("Gateway", "Connected") }
            override fun onMessage(ws: WebSocket, text: String) { handleMessage(text) }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) { stopHeartbeat() }
            override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) { Log.e("Gateway", "Error: ${t.message}"); stopHeartbeat() }
        })
    }

    private fun handleMessage(text: String) {
        val msg = JSONObject(text)
        val op  = msg.getInt("op"); val data = msg.optJSONObject("d"); val type = msg.optString("t", "")
        seq = msg.optInt("s", seq)
        when (op) {
            10 -> { heartbeatInterval = data?.getLong("heartbeat_interval") ?: 41250L; startHeartbeat(); identify() }
            0  -> if (type.isNotEmpty() && data != null) _events.tryEmit(GatewayEvent(type, data))
        }
    }

    private fun identify() {
        ws?.send(JSONObject().apply {
            put("op", 2)
            put("d", JSONObject().apply {
                put("token", token); put("intents", 32767)
                put("properties", JSONObject().apply { put("os","android"); put("browser","nexus-android"); put("device","nexus-android") })
                put("presence", JSONObject().apply { put("status","online"); put("afk",false) })
            })
        }.toString())
    }

    fun sendPresenceUpdate(status: String) {
        ws?.send(JSONObject().apply { put("op",3); put("d", JSONObject().apply { put("status",status); put("afk",false); put("since",System.currentTimeMillis()) }) }.toString())
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatThread = Thread {
            try {
                Thread.sleep((heartbeatInterval * Math.random()).toLong())
                while (!Thread.currentThread().isInterrupted) {
                    ws?.send(JSONObject().apply { put("op",1); put("d",seq) }.toString())
                    Thread.sleep(heartbeatInterval)
                }
            } catch (_: InterruptedException) {}
        }.also { it.isDaemon = true; it.start() }
    }

    private fun stopHeartbeat() { heartbeatThread?.interrupt(); heartbeatThread = null }
    fun disconnect() { stopHeartbeat(); ws?.close(1000, "Disconnect"); ws = null }
}
