package com.tether.app.core.network.firebase

import android.util.Log
import com.tether.app.core.crypto.SessionCryptoEngine
import com.tether.app.core.network.config.CloudConfig
import com.tether.app.core.network.model.TetherPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

class FirebaseRealtimeManager(
    private val roomCodeProvider: () -> String? = { null },
    private val sessionKeyProvider: () -> SecretKey?
) {
    private val TAG = "FirebaseRealtime"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Read timeout 0 untuk streaming SSE jangka panjang
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamJob: Job? = null

    private val _incomingPackets = MutableSharedFlow<TetherPacket>(extraBufferCapacity = 64)
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    @Volatile
    private var idToken: String? = null
    @Volatile
    private var myUid: String? = null
    private var tokenTimestamp: Long = 0
    private val authMutex = Mutex()

    private val seenMessageIds = ConcurrentHashMap.newKeySet<String>()

    private fun getRoomMessagesUrl(token: String): String {
        val dbUrl = CloudConfig.FIREBASE_DATABASE_URL.trimEnd('/')
        val roomCode = roomCodeProvider()?.trim()?.lowercase().let {
            if (it.isNullOrBlank()) "default_couple" else it
        }
        return "$dbUrl/rooms/$roomCode/messages.json?auth=$token"
    }

    fun start() {
        if (!CloudConfig.isFirebaseConfigured()) {
            Log.d(TAG, "Firebase belum dikonfigurasi di CloudConfig, melewati inisialisasi cloud DB")
            return
        }

        streamJob?.cancel()
        streamJob = scope.launch {
            while (isActive) {
                try {
                    ensureAuthenticated()
                    if (idToken == null) {
                        delay(5000)
                        continue
                    }

                    updatePresence(true)
                    listenToMessagesStream()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Firebase stream terputus: ${e.message}, rekoneksi dalam 4 detik...")
                    _isConnected.value = false
                    delay(4000)
                }
            }
        }
    }

    private suspend fun ensureAuthenticated(): Boolean = authMutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (idToken != null && (now - tokenTimestamp) < 50 * 60 * 1000) {
                return@withContext true
            }

            val apiKey = CloudConfig.FIREBASE_API_KEY
            val email = CloudConfig.ACTIVE_EMAIL
            val password = CloudConfig.ACTIVE_PASSWORD

            if (apiKey.isBlank() || email.isBlank() || password.isBlank()) {
                Log.w(TAG, "Email/Password Firebase belum diatur di CloudConfig")
                return@withContext false
            }

            try {
                val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
                val reqBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("returnSecureToken", true)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder().url(url).post(reqBody).build()
                client.newCall(request).execute().use { response ->
                    val respStr = response.body?.string() ?: return@withContext false
                    if (response.isSuccessful) {
                        val json = JSONObject(respStr)
                        idToken = json.getString("idToken")
                        myUid = json.getString("localId")
                        tokenTimestamp = System.currentTimeMillis()
                        Log.d(TAG, "Login Firebase Auth berhasil. UID: $myUid")
                        true
                    } else {
                        Log.e(TAG, "Login Firebase Auth gagal: $respStr")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saat login Firebase Auth: ${e.message}")
                false
            }
        }
    }

    private suspend fun listenToMessagesStream() = withContext(Dispatchers.IO) {
        val token = idToken ?: return@withContext
        val url = getRoomMessagesUrl(token)

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                _isConnected.value = false
                Log.e(TAG, "Gagal konek SSE Firebase: HTTP ${response.code}")
                delay(3000)
                return@withContext
            }

            _isConnected.value = true
            Log.d(TAG, "Koneksi SSE Firebase Realtime aktif!")

            val reader = BufferedReader(InputStreamReader(response.body!!.byteStream(), Charsets.UTF_8))
            var line: String? = reader.readLine()
            var currentEvent = ""

            while (line != null && isActive) {
                if (line.startsWith("event: ")) {
                    currentEvent = line.substring(7).trim()
                } else if (line.startsWith("data: ")) {
                    val dataStr = line.substring(6).trim()
                    if (dataStr != "null" && (currentEvent == "put" || currentEvent == "patch")) {
                        handleSseData(dataStr)
                    }
                }
                line = reader.readLine()
            }
        }
    }

    private suspend fun handleSseData(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val path = root.optString("path", "")
            val dataObj = root.opt("data") ?: return

            if (dataObj is JSONObject) {
                if (dataObj.has("payload")) {
                    // Pesan baru tunggal
                    processMessageItem(dataObj)
                } else {
                    // Initial dump dari kamar berisi daftar pesan (termasuk yang dikirim saat offline)
                    val keys = dataObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val item = dataObj.optJSONObject(k)
                        if (item != null && item.has("payload")) {
                            processMessageItem(item)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal parse event SSE: ${e.message}")
        }
    }

    private suspend fun processMessageItem(msgObj: JSONObject) {
        val senderUid = msgObj.optString("senderUid", "")
        if (senderUid == myUid) return // Abaikan pesan dari diri sendiri

        val payload = msgObj.optString("payload", "")
        if (payload.isBlank()) return

        val unwrapped = SessionCryptoEngine.unwrapWireEnvelopeSerialized(payload, sessionKeyProvider()) ?: return
        val packet = TetherPacket.fromJson(unwrapped) ?: return

        if (seenMessageIds.size > 1000) {
            val iterator = seenMessageIds.iterator()
            var count = 0
            while (iterator.hasNext() && count < 500) {
                iterator.next()
                iterator.remove()
                count++
            }
        }
        if (seenMessageIds.add(packet.id)) {
            scope.launch {
                _incomingPackets.emit(packet)
            }
        }
    }

    suspend fun sendPacket(packet: TetherPacket): Boolean = withContext(Dispatchers.IO) {
        if (!CloudConfig.isFirebaseConfigured()) return@withContext false
        ensureAuthenticated()
        val token = idToken ?: return@withContext false

        val url = getRoomMessagesUrl(token)

        val wirePayload = SessionCryptoEngine.wrapWireEnvelopeSerialized(packet.toJson(), sessionKeyProvider())
        val msgPayload = JSONObject().apply {
            put("senderUid", myUid)
            put("payload", wirePayload)
            put("createdAt", System.currentTimeMillis())
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(msgPayload).build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal kirim pesan ke Firebase: ${e.message}")
            false
        }
    }

    private suspend fun updatePresence(online: Boolean) = withContext(Dispatchers.IO) {
        val token = idToken ?: return@withContext
        val uid = myUid ?: return@withContext
        val dbUrl = CloudConfig.FIREBASE_DATABASE_URL.trimEnd('/')
        val url = "$dbUrl/status/$uid.json?auth=$token"

        val body = JSONObject().apply {
            put("online", online)
            put("lastSeen", System.currentTimeMillis())
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).put(body).build()
        try {
            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    fun stop() {
        streamJob?.cancel()
        scope.launch {
            updatePresence(false)
        }
        _isConnected.value = false
    }
}
