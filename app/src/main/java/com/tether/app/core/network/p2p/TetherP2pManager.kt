package com.tether.app.core.network.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.tether.app.core.network.model.TetherPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class TetherP2pManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _incomingPackets = MutableSharedFlow<TetherPacket>(extraBufferCapacity = 64)
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _isP2pConnected = MutableStateFlow(false)
    val isP2pConnected = _isP2pConnected.asStateFlow()

    private val _connectedPeerIp = MutableStateFlow<String?>(null)
    val connectedPeerIp = _connectedPeerIp.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var isListening = false
    private val P2P_PORT = 42890
    private val SERVICE_TYPE = "_tether_p2p._tcp."
    private val SERVICE_NAME = "Tether_Couple"

    fun startP2pEngine() {
        startSocketServer()
        registerNsdService()
        discoverNsdServices()
    }

    private fun startSocketServer() {
        if (isListening) return
        scope.launch {
            try {
                serverSocket = ServerSocket(P2P_PORT).apply { reuseAddress = true }
                isListening = true
                Log.d("TetherP2P", "Socket server listening on port $P2P_PORT")

                while (isActive && isListening) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleIncomingConnection(clientSocket)
                }
            } catch (e: Exception) {
                Log.e("TetherP2P", "Server socket error: ${e.message}")
            }
        }
    }

    private fun handleIncomingConnection(socket: Socket) {
        scope.launch {
            try {
                val peerAddress = socket.inetAddress.hostAddress
                _connectedPeerIp.value = peerAddress
                _isP2pConnected.value = true

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine()
                if (line != null) {
                    val packet = TetherPacket.fromJson(line)
                    if (packet != null) {
                        _incomingPackets.emit(packet)
                    }
                }
                socket.close()
            } catch (e: Exception) {
                Log.e("TetherP2P", "Read error: ${e.message}")
            }
        }
    }

    suspend fun sendPacket(peerIp: String, packet: TetherPacket): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket(peerIp, P2P_PORT).use { socket ->
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println(packet.toJson())
            }
            true
        } catch (e: Exception) {
            Log.e("TetherP2P", "Failed to send packet to $peerIp: ${e.message}")
            false
        }
    }

    private fun registerNsdService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = P2P_PORT
        }

        try {
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) {
                        Log.d("TetherP2P", "NSD registered: ${info.serviceName}")
                    }
                    override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceUnregistered(info: NsdServiceInfo) {}
                    override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                }
            )
        } catch (e: Exception) {
            Log.e("TetherP2P", "NSD register error: ${e.message}")
        }
    }

    private fun discoverNsdServices() {
        try {
            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onDiscoveryStarted(serviceType: String) {}
                    override fun onDiscoveryStopped(serviceType: String) {}

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        nsdManager.resolveService(
                            serviceInfo,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                    val host: InetAddress? = resolvedInfo.host
                                    val ip = host?.hostAddress
                                    if (ip != null) {
                                        _connectedPeerIp.value = ip
                                        _isP2pConnected.value = true
                                        Log.d("TetherP2P", "Peer found via NSD at $ip")
                                    }
                                }
                            }
                        )
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        _isP2pConnected.value = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("TetherP2P", "NSD discovery error: ${e.message}")
        }
    }

    fun stop() {
        isListening = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
    }
}
