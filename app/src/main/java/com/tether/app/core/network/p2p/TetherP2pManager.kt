package com.tether.app.core.network.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.tether.app.core.crypto.SessionCryptoEngine
import com.tether.app.core.network.model.TetherPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.SecretKey

class TetherP2pManager(
    context: Context,
    private val sessionKeyProvider: () -> SecretKey? = { null }
) {
    private val appContext = context.applicationContext
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _incomingPackets = MutableSharedFlow<TetherPacket>(extraBufferCapacity = 64)
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _isP2pConnected = MutableStateFlow(false)
    val isP2pConnected = _isP2pConnected.asStateFlow()

    private val _connectedPeerIp = MutableStateFlow<String?>(null)
    val connectedPeerIp = _connectedPeerIp.asStateFlow()

    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isListening = false
    private val isResolving = AtomicBoolean(false)

    private val P2P_PORT = 42890
    private val SERVICE_TYPE = "_tether_p2p._tcp."
    private val localServiceName = "Tether_${UUID.randomUUID().toString().take(8)}"
    private var registeredServiceName: String = localServiceName

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startP2pEngine() {
        if (isListening) return
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        startSocketServer()
        registerNsdService()
        discoverNsdServices()
    }

    private fun startSocketServer() {
        scope.launch {
            try {
                // Set reuseAddress sebelum bind sesuai kontrak java.net.ServerSocket
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(P2P_PORT))
                }
                isListening = true
                Log.d("TetherP2P", "Socket server listening on port $P2P_PORT")
                while (isActive && isListening) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleIncomingConnection(clientSocket)
                }
            } catch (e: Exception) {
                if (isListening) Log.e("TetherP2P", "Server socket error: ${e.message}")
                isListening = false
            }
        }
    }

    private fun handleIncomingConnection(socket: Socket) {
        scope.launch {
            socket.use { client ->
                try {
                    client.soTimeout = 5000
                    val peerAddress = client.inetAddress.hostAddress
                    val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                    val wireLine = reader.readLine()
                    if (wireLine != null) {
                        val unwrappedJson = SessionCryptoEngine.unwrapWireEnvelopeSerialized(
                            wireLine = wireLine,
                            sessionKey = sessionKeyProvider()
                        )
                        if (unwrappedJson == null) {
                            Log.w("TetherP2P", "Dropped unauthenticated/undecryptable packet from $peerAddress")
                            return@use
                        }
                        val packet = TetherPacket.fromJson(unwrappedJson)
                        if (packet != null) {
                            _connectedPeerIp.value = peerAddress
                            _isP2pConnected.value = true
                            _incomingPackets.emit(packet)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TetherP2P", "Read error: ${e.message}")
                }
            }
        }
    }

    suspend fun sendPacket(peerIp: String, packet: TetherPacket): Boolean = withContext(Dispatchers.IO) {
        try {
            val wirePayload = SessionCryptoEngine.wrapWireEnvelopeSerialized(
                plainJson = packet.toJson(),
                sessionKey = sessionKeyProvider()
            )
            Socket().use { socket ->
                socket.connect(InetSocketAddress(peerIp, P2P_PORT), 5000)
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream().bufferedWriter(Charsets.UTF_8), true)
                writer.println(wirePayload)
            }
            true
        } catch (e: Exception) {
            Log.e("TetherP2P", "Failed to send packet to $peerIp: ${e.message}")
            false
        }
    }

    private fun registerNsdService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = localServiceName
            serviceType = SERVICE_TYPE
            port = P2P_PORT
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                registeredServiceName = info.serviceName
                Log.d("TetherP2P", "NSD registered as: $registeredServiceName")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("TetherP2P", "NSD registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d("TetherP2P", "NSD unregistered: ${info.serviceName}")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("TetherP2P", "NSD unregistration failed: $errorCode")
            }
        }
        registrationListener = listener
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e("TetherP2P", "NSD register error: ${e.message}")
        }
    }

    private fun discoverNsdServices() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName == registeredServiceName ||
                    serviceInfo.serviceName == localServiceName
                ) {
                    return
                }
                if (!isResolving.compareAndSet(false, true)) return

                try {
                    nsdManager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                                isResolving.set(false)
                            }
                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                isResolving.set(false)
                                if (resolvedInfo.serviceName == registeredServiceName) return
                                val host = resolvedInfo.host
                                val ip = host?.hostAddress
                                if (ip != null && !host.isLoopbackAddress) {
                                    _connectedPeerIp.value = ip
                                    _isP2pConnected.value = true
                                    Log.d("TetherP2P", "Peer found via NSD at $ip")
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    isResolving.set(false)
                    Log.e("TetherP2P", "NSD resolve error: ${e.message}")
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName != registeredServiceName) {
                    _isP2pConnected.value = false
                    _connectedPeerIp.value = null
                }
            }
        }
        discoveryListener = listener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e("TetherP2P", "NSD discovery error: ${e.message}")
        }
    }

    fun stop() {
        isListening = false
        isResolving.set(false)
        registrationListener?.let {
            try { nsdManager.unregisterService(it) } catch (_: Exception) {}
        }
        registrationListener = null
        discoveryListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        discoveryListener = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        scope.cancel()
    }
}
