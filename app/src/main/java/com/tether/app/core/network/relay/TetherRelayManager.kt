package com.tether.app.core.network.relay

import android.util.Log
import com.tether.app.core.network.model.TetherPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class TetherRelayManager(
    private val serverUrl: String = "wss://alarm.birescompanny.my.id/tether"
) {
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var connectJob: Job? = null
    private var shouldReconnect = true

    private val _incomingPackets = MutableSharedFlow<TetherPacket>(extraBufferCapacity = 64)
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _isRelayConnected = MutableStateFlow(false)
    val isRelayConnected = _isRelayConnected.asStateFlow()

    fun startRelayEngine(pairId: String = "tether_couple_primary", clientId: String = "client_${System.currentTimeMillis()}") {
        shouldReconnect = true
        connect(pairId, clientId)
    }

    private fun connect(pairId: String, clientId: String) {
        connectJob?.cancel()
        connectJob = scope.launch {
            while (isActive && shouldReconnect) {
                if (_isRelayConnected.value) {
                    delay(5000)
                    continue
                }

                val url = "$serverUrl?pairId=$pairId&clientId=$clientId"
                val request = Request.Builder().url(url).build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        Log.d("TetherRelay", "Connected to relay: $url")
                        _isRelayConnected.value = true
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        try {
                            val packet = TetherPacket.fromJson(text)
                            if (packet != null) {
                                scope.launch {
                                    _incomingPackets.emit(packet)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TetherRelay", "Error parsing incoming packet: ${e.message}")
                        }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        Log.d("TetherRelay", "Relay closed: $reason")
                        _isRelayConnected.value = false
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        Log.e("TetherRelay", "Relay connection failed: ${t.message}")
                        _isRelayConnected.value = false
                    }
                })

                delay(5000)
            }
        }
    }

    fun sendPacket(packet: TetherPacket): Boolean {
        val ws = webSocket
        if (ws != null && _isRelayConnected.value) {
            val json = packet.toJson()
            return ws.send(json)
        }
        return false
    }

    fun stop() {
        shouldReconnect = false
        connectJob?.cancel()
        try {
            webSocket?.close(1000, "App closed")
        } catch (_: Exception) {}
        webSocket = null
        _isRelayConnected.value = false
        scope.cancel()
    }
}
