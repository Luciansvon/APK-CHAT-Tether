package com.tether.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tether.app.core.database.TetherDatabase
import com.tether.app.core.database.entity.CouponEntity
import com.tether.app.core.database.entity.MessageEntity
import com.tether.app.core.database.entity.PartnerProfileEntity
import com.tether.app.core.network.model.TetherPacket
import com.tether.app.core.network.p2p.TetherP2pManager
import com.tether.app.core.overlay.TetherOverlayManager
import com.tether.app.core.sensor.HapticResonanceEngine
import com.tether.app.core.sensor.ProximitySensorManager
import com.tether.app.domain.command.TetherCommand
import com.tether.app.domain.command.TetherCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TetherDatabase.getInstance(application)
    private val messageDao = db.messageDao()
    private val couponDao = db.couponDao()
    private val profileDao = db.partnerProfileDao()

    val p2pManager = TetherP2pManager(application)
    val proximitySensor = ProximitySensorManager(application)
    val hapticEngine = HapticResonanceEngine(application)

    val messages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val partnerProfile: StateFlow<PartnerProfileEntity?> = profileDao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private var currentLamportClock: Long = 0L

    init {
        p2pManager.startP2pEngine()
        proximitySensor.startListening()

        viewModelScope.launch {
            seedDefaultCouponsAndProfile()
            currentLamportClock = messageDao.getLatestLamportTimestamp() ?: 0L
        }

        // Tangkap paket P2P yang masuk secara real-time
        viewModelScope.launch {
            p2pManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private suspend fun seedDefaultCouponsAndProfile() {
        if (profileDao.getProfile() == null) {
            profileDao.saveProfile(
                PartnerProfileEntity(
                    partnerId = "partner_1",
                    partnerName = "Lina",
                    myName = "Bima",
                    moodCycle = "Luteal",
                    publicKey = "pk_test_sample",
                    isPaired = true,
                    pairedAt = System.currentTimeMillis()
                )
            )
        }

        if (couponDao.getCouponsCount() == 0) {
            val defaultCoupons = listOf(
                CouponEntity("c1", "Kupon Kasih Sayang #01", "1x Peluk Hangat Kapan Saja", false, null, 0f),
                CouponEntity("c2", "Kupon Kasih Sayang #02", "1x Bebas Pilih Menu Makan Malam", false, null, 0f),
                CouponEntity("c3", "Kupon Kasih Sayang #03", "1x Ditraktir Kopi Favorit", false, null, 0f),
                CouponEntity("c4", "Kupon Kasih Sayang #04", "1x Pijat Punggung Bebas Request", true, System.currentTimeMillis(), 0f)
            )
            couponDao.insertCoupons(defaultCoupons)
        }
    }

    fun updateProfile(partnerName: String, myName: String, mood: String, partnerAvatarUri: String?, myAvatarUri: String? = null) {
        viewModelScope.launch {
            val current = profileDao.getProfile() ?: return@launch
            profileDao.updateProfileData(
                partnerId = current.partnerId,
                partnerName = partnerName,
                myName = myName,
                mood = mood,
                partnerAvatarUri = partnerAvatarUri,
                myAvatarUri = myAvatarUri ?: current.myAvatarUri
            )
        }
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        _inputText.value = ""

        if (TetherCommandParser.isCommand(text)) {
            executeCommand(text)
            return
        }

        currentLamportClock++

        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "TEXT",
            content = text,
            lamportTimestamp = currentLamportClock,
            localTimestamp = System.currentTimeMillis(),
            rotation = (-5..5).random() / 10f
        )

        viewModelScope.launch {
            messageDao.insertMessage(msg)
            val peerIp = p2pManager.connectedPeerIp.value
            if (peerIp != null) {
                p2pManager.sendPacket(
                    peerIp,
                    TetherPacket(
                        id = msg.id,
                        type = "TEXT",
                        senderId = "ME",
                        content = text,
                        lamportTimestamp = msg.lamportTimestamp
                    )
                )
            }
        }
    }

    private fun executeCommand(rawText: String) {
        val command = TetherCommandParser.parse(rawText)
        viewModelScope.launch {
            currentLamportClock++
            when (command) {
                is TetherCommand.Mood -> {
                    val current = profileDao.getProfile()
                    val moodCapitalized = command.mood.replaceFirstChar { it.uppercase() }
                    if (current != null) {
                        profileDao.updateProfileData(
                            partnerId = current.partnerId,
                            partnerName = current.partnerName,
                            myName = current.myName,
                            mood = moodCapitalized,
                            partnerAvatarUri = current.partnerAvatarUri,
                            myAvatarUri = current.myAvatarUri
                        )
                    }
                    val noteStr = if (!command.note.isNullOrBlank()) "\n\"${command.note}\"" else ""
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "◆ STATUS MOOD: ${moodCapitalized.uppercase()} ◆$noteStr",
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)

                    val peerIp = p2pManager.connectedPeerIp.value
                    if (peerIp != null) {
                        p2pManager.sendPacket(
                            peerIp,
                            TetherPacket(
                                id = msg.id,
                                type = "SYSTEM",
                                senderId = "ME",
                                content = msg.content,
                                lamportTimestamp = msg.lamportTimestamp
                            )
                        )
                    }
                }
                is TetherCommand.Tug -> {
                    hapticEngine.playTugHaptic()
                    val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                    TetherOverlayManager.showTugOverlay(
                        context = getApplication(),
                        partnerName = partnerName,
                        partnerAvatarUri = partnerProfile.value?.partnerAvatarUri
                    )
                    val noteStr = if (!command.message.isNullOrBlank()) "\n\"${command.message}\"" else ""
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "⚡ KAMU MENARIK TETHER! ⚡$noteStr",
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)

                    val peerIp = p2pManager.connectedPeerIp.value
                    if (peerIp != null) {
                        p2pManager.sendPacket(
                            peerIp,
                            TetherPacket(
                                id = msg.id,
                                type = "TUG",
                                senderId = "ME",
                                content = command.message ?: "Tether Tug!",
                                lamportTimestamp = msg.lamportTimestamp
                            )
                        )
                    }
                }
                is TetherCommand.Peluk -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "TEXT",
                        caption = "🩹 MODE PELUK (Dengerin Aja)",
                        content = command.message,
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis(),
                        rotation = (-5..5).random() / 10f
                    )
                    messageDao.insertMessage(msg)

                    val peerIp = p2pManager.connectedPeerIp.value
                    if (peerIp != null) {
                        p2pManager.sendPacket(
                            peerIp,
                            TetherPacket(
                                id = msg.id,
                                type = "TEXT",
                                senderId = "ME",
                                caption = msg.caption,
                                content = command.message,
                                lamportTimestamp = msg.lamportTimestamp
                            )
                        )
                    }
                }
                is TetherCommand.Solusi -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "TEXT",
                        caption = "🛠️ BUTUH SOLUSI",
                        content = command.message,
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis(),
                        rotation = (-5..5).random() / 10f
                    )
                    messageDao.insertMessage(msg)

                    val peerIp = p2pManager.connectedPeerIp.value
                    if (peerIp != null) {
                        p2pManager.sendPacket(
                            peerIp,
                            TetherPacket(
                                id = msg.id,
                                type = "TEXT",
                                senderId = "ME",
                                caption = msg.caption,
                                content = command.message,
                                lamportTimestamp = msg.lamportTimestamp
                            )
                        )
                    }
                }
                is TetherCommand.Dadu -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "🎲 DADU ANTI-TERSERAH:\nOpsi: [${command.options.joinToString(", ")}]\n▶ HASIL KELUAR: [ ${command.chosen.uppercase()} ] ★",
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)

                    val peerIp = p2pManager.connectedPeerIp.value
                    if (peerIp != null) {
                        p2pManager.sendPacket(
                            peerIp,
                            TetherPacket(
                                id = msg.id,
                                type = "SYSTEM",
                                senderId = "ME",
                                content = msg.content,
                                lamportTimestamp = msg.lamportTimestamp
                            )
                        )
                    }
                }
                is TetherCommand.Unknown -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "⚠️ ${command.reason}",
                        lamportTimestamp = currentLamportClock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                }
            }
        }
    }

    fun sendPolaroidSample() {
        currentLamportClock++
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "POLAROID",
            content = "polaroid_cafe",
            caption = "nanti malem ke sini yuk?",
            lamportTimestamp = currentLamportClock,
            localTimestamp = System.currentTimeMillis(),
            isFoggy = true,
            rotation = -2.0f
        )
        viewModelScope.launch {
            messageDao.insertMessage(msg)
        }
    }

    fun sendWhisperSample() {
        currentLamportClock++
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "WHISPER",
            content = "whisper_sample_audio",
            duration = "0:14",
            lamportTimestamp = currentLamportClock,
            localTimestamp = System.currentTimeMillis(),
            rotation = 1.0f
        )
        viewModelScope.launch {
            messageDao.insertMessage(msg)
        }
    }

    fun toggleAudioPlayback(id: String) {
        if (_playingAudioId.value == id) {
            _playingAudioId.value = null
        } else {
            _playingAudioId.value = id
            hapticEngine.playTouchPulse()
        }
    }

    private suspend fun handleIncomingPacket(packet: TetherPacket) {
        currentLamportClock = maxOf(currentLamportClock, packet.lamportTimestamp) + 1

        val isTug = packet.type == "TUG"
        if (isTug) {
            val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
            TetherOverlayManager.showTugOverlay(
                context = getApplication(),
                partnerName = partnerName,
                partnerAvatarUri = partnerProfile.value?.partnerAvatarUri
            )
            hapticEngine.playTugHaptic()
        }

        val msg = MessageEntity(
            id = packet.id,
            senderId = "PARTNER",
            type = if (isTug) "SYSTEM" else packet.type,
            content = if (isTug) "⚡ ${partnerProfile.value?.partnerName ?: "Pasangan"} menarik Tether kamu! ⚡\n\"${packet.content}\"" else packet.content,
            caption = packet.caption,
            duration = packet.duration,
            lamportTimestamp = currentLamportClock,
            localTimestamp = System.currentTimeMillis(),
            rotation = (-10..10).random() / 10f
        )
        messageDao.insertMessage(msg)
        if (!isTug) {
            hapticEngine.playHeartbeatResonance()
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pManager.stop()
        proximitySensor.stopListening()
    }
}
