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
import com.tether.app.core.network.relay.TetherRelayManager
import com.tether.app.core.overlay.TetherOverlayManager
import com.tether.app.core.sensor.HapticResonanceEngine
import com.tether.app.core.sensor.ProximitySensorManager
import com.tether.app.core.sound.SoundEffectManager
import com.tether.app.domain.command.TetherCommand
import com.tether.app.domain.command.TetherCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TetherDatabase.getInstance(application)
    private val messageDao = db.messageDao()
    private val couponDao = db.couponDao()
    private val profileDao = db.partnerProfileDao()

    val p2pManager = TetherP2pManager(application)
    val relayManager = TetherRelayManager()
    val proximitySensor = ProximitySensorManager(application)
    val hapticEngine = HapticResonanceEngine(application)
    val soundManager = SoundEffectManager(application)

    val messages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val partnerProfile: StateFlow<PartnerProfileEntity?> = profileDao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private val currentLamportClock = AtomicLong(0L)

    init {
        p2pManager.startP2pEngine()
        relayManager.startRelayEngine()

        viewModelScope.launch {
            seedDefaultCouponsAndProfile()
            val latest = messageDao.getLatestLamportTimestamp() ?: 0L
            currentLamportClock.set(latest)
        }

        // Tangkap paket P2P yang masuk secara real-time
        viewModelScope.launch {
            p2pManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        // Tangkap paket Relay Internet yang masuk secara real-time
        viewModelScope.launch {
            relayManager.incomingPackets.collect { packet ->
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

        soundManager.playSendSound()
        val clock = currentLamportClock.incrementAndGet()

        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "TEXT",
            content = text,
            lamportTimestamp = clock,
            localTimestamp = System.currentTimeMillis(),
            rotation = (-5..5).random() / 10f
        )

        viewModelScope.launch {
            messageDao.insertMessage(msg)
            dispatchPacket(
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

    private suspend fun dispatchPacket(packet: TetherPacket) {
        val peerIp = p2pManager.connectedPeerIp.value
        if (p2pManager.isP2pConnected.value && peerIp != null) {
            p2pManager.sendPacket(peerIp, packet)
        } else if (relayManager.isRelayConnected.value) {
            relayManager.sendPacket(packet)
        }
    }

    private fun executeCommand(rawText: String) {
        soundManager.playSendSound()
        val command = TetherCommandParser.parse(rawText)
        viewModelScope.launch {
            val clock = currentLamportClock.incrementAndGet()
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
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
                        TetherPacket(
                            id = msg.id,
                            type = "SYSTEM",
                            senderId = "ME",
                            content = msg.content,
                            lamportTimestamp = msg.lamportTimestamp
                        )
                    )
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
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
                        TetherPacket(
                            id = msg.id,
                            type = "TUG",
                            senderId = "ME",
                            content = command.message ?: "Tether Tug!",
                            lamportTimestamp = msg.lamportTimestamp
                        )
                    )
                }
                is TetherCommand.Peluk -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "TEXT",
                        caption = "🩹 MODE PELUK (Dengerin Aja)",
                        content = command.message,
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis(),
                        rotation = (-5..5).random() / 10f
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
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
                is TetherCommand.Solusi -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "TEXT",
                        caption = "🛠️ BUTUH SOLUSI",
                        content = command.message,
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis(),
                        rotation = (-5..5).random() / 10f
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
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
                is TetherCommand.Dadu -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "🎲 DADU ANTI-TERSERAH:\nOpsi: [${command.options.joinToString(", ")}]\n▶ HASIL KELUAR: [ ${command.chosen.uppercase()} ] ★",
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
                        TetherPacket(
                            id = msg.id,
                            type = "SYSTEM",
                            senderId = "ME",
                            content = msg.content,
                            lamportTimestamp = msg.lamportTimestamp
                        )
                    )
                }
                is TetherCommand.Unknown -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "ME",
                        type = "SYSTEM",
                        content = "⚠️ ${command.reason}",
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                }
            }
        }
    }

    fun sendPolaroidSample() {
        soundManager.playSendSound()
        val clock = currentLamportClock.incrementAndGet()
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "POLAROID",
            content = "polaroid_cafe",
            caption = "nanti malem ke sini yuk?",
            lamportTimestamp = clock,
            localTimestamp = System.currentTimeMillis(),
            isFoggy = true,
            rotation = -2.0f
        )
        viewModelScope.launch {
            messageDao.insertMessage(msg)
        }
    }

    fun sendWhisperSample() {
        soundManager.playSendSound()
        val clock = currentLamportClock.incrementAndGet()
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "ME",
            type = "WHISPER",
            content = "whisper_sample_audio",
            duration = "0:14",
            lamportTimestamp = clock,
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
            proximitySensor.stopListening()
        } else {
            _playingAudioId.value = id
            proximitySensor.startListening()
            hapticEngine.playTouchPulse()
        }
    }

    fun claimCoupon(couponId: String) {
        viewModelScope.launch {
            couponDao.claimCoupon(couponId, System.currentTimeMillis())
            val clock = currentLamportClock.incrementAndGet()
            dispatchPacket(
                TetherPacket(
                    id = UUID.randomUUID().toString(),
                    type = "COUPON_CLAIM",
                    senderId = "ME",
                    content = couponId,
                    lamportTimestamp = clock
                )
            )
            hapticEngine.playTugHaptic()
        }
    }

    private suspend fun handleIncomingPacket(packet: TetherPacket) {
        val updatedClock = currentLamportClock.updateAndGet { current ->
            maxOf(current, packet.lamportTimestamp) + 1L
        }

        when (packet.type) {
            "TEXT", "POLAROID", "WHISPER" -> {
                soundManager.playReceiveSound()
                val msg = MessageEntity(
                    id = packet.id,
                    senderId = "PARTNER",
                    type = packet.type,
                    content = packet.content,
                    caption = packet.caption,
                    duration = packet.duration,
                    lamportTimestamp = updatedClock,
                    localTimestamp = System.currentTimeMillis(),
                    isFoggy = packet.type == "POLAROID",
                    rotation = (-10..10).random() / 10f
                )
                messageDao.insertMessage(msg)
                hapticEngine.playHeartbeatResonance()
            }
            "TUG" -> {
                soundManager.playReceiveSound()
                hapticEngine.playTugHaptic()
                val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                if (TetherOverlayManager.canDrawOverlays(getApplication())) {
                    TetherOverlayManager.showTugOverlay(
                        context = getApplication(),
                        partnerName = partnerName,
                        partnerAvatarUri = partnerProfile.value?.partnerAvatarUri
                    )
                }
                val noteStr = if (packet.content.isNotBlank() && packet.content != "Tether Tug!") "\n\"${packet.content}\"" else ""
                val msg = MessageEntity(
                    id = packet.id,
                    senderId = "PARTNER",
                    type = "SYSTEM",
                    content = "⚡ $partnerName menarik Tether kamu! ⚡$noteStr",
                    caption = packet.caption,
                    duration = packet.duration,
                    lamportTimestamp = updatedClock,
                    localTimestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(msg)
            }
            "SYSTEM" -> {
                soundManager.playReceiveSound()
                val msg = MessageEntity(
                    id = packet.id,
                    senderId = "PARTNER",
                    type = "SYSTEM",
                    content = packet.content,
                    caption = packet.caption,
                    duration = packet.duration,
                    lamportTimestamp = updatedClock,
                    localTimestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(msg)
                hapticEngine.playHeartbeatResonance()
            }
            "COUPON_CLAIM" -> {
                couponDao.claimCoupon(packet.content, System.currentTimeMillis())
                hapticEngine.playTugHaptic()
            }
            "HAPTIC_TOUCH" -> {
                hapticEngine.playTouchPulse()
            }
            "HEARTBEAT" -> {
                hapticEngine.playHeartbeatResonance()
                val profile = partnerProfile.value
                if (TetherOverlayManager.canDrawOverlays(getApplication())) {
                    TetherOverlayManager.showTugOverlay(
                        context = getApplication(),
                        partnerName = profile?.partnerName ?: "Pasangan",
                        partnerAvatarUri = profile?.partnerAvatarUri
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pManager.stop()
        relayManager.stop()
        proximitySensor.stopListening()
        soundManager.release()
    }
}
