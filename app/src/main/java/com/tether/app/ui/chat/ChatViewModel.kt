package com.tether.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tether.app.core.ai.PixelAvatarGenerator
import com.tether.app.core.crypto.SessionCryptoEngine
import com.tether.app.core.database.TetherDatabase
import com.tether.app.core.database.entity.CoupleLevelEntity
import com.tether.app.core.database.entity.CouponEntity
import com.tether.app.core.database.entity.MessageEntity
import com.tether.app.core.database.entity.PartnerProfileEntity
import com.tether.app.core.network.drive.DriveUploadManager
import com.tether.app.core.network.firebase.FirebaseRealtimeManager
import com.tether.app.core.network.model.TetherPacket
import com.tether.app.core.network.p2p.TetherP2pManager
import com.tether.app.core.network.relay.TetherRelayManager
import com.tether.app.core.overlay.TetherOverlayManager
import com.tether.app.core.pairing.PairingPayload
import com.tether.app.core.security.KeystoreManager
import com.tether.app.core.sensor.HapticResonanceEngine
import com.tether.app.core.sensor.ProximitySensorManager
import com.tether.app.core.sound.SoundEffectManager
import com.tether.app.domain.command.TetherCommand
import com.tether.app.domain.command.TetherCommandParser
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.SecretKey

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TetherDatabase.getInstance(application)
    private val messageDao = db.messageDao()
    private val couponDao = db.couponDao()
    private val profileDao = db.partnerProfileDao()
    private val coupleLevelDao = db.coupleLevelDao()
    val pixelAvatarGenerator = PixelAvatarGenerator(application)

    @Volatile
    private var activeSessionKey: SecretKey? = null

    val p2pManager = TetherP2pManager(
        context = application,
        sessionKeyProvider = { activeSessionKey }
    )
    val relayManager = TetherRelayManager(
        sessionKeyProvider = { activeSessionKey }
    )
    val firebaseManager = FirebaseRealtimeManager(
        roomCodeProvider = { KeystoreManager.getRoomCode(application) },
        sessionKeyProvider = { activeSessionKey }
    )
    val proximitySensor = ProximitySensorManager(application)
    val hapticEngine = HapticResonanceEngine(application)
    val soundManager = SoundEffectManager(application)

    private val _isRoomConfigured: MutableStateFlow<Boolean> = MutableStateFlow(KeystoreManager.isRoomConfigured(application))
    val isRoomConfigured: StateFlow<Boolean> = _isRoomConfigured.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val partnerProfile: StateFlow<PartnerProfileEntity?> = profileDao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val coupleLevel: StateFlow<CoupleLevelEntity?> = coupleLevelDao.getLevelFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _isGeneratingAvatar = MutableStateFlow(false)
    val isGeneratingAvatar = _isGeneratingAvatar.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private var audioPlaybackJob: Job? = null
    private val currentLamportClock = AtomicLong(0L)

    init {
        p2pManager.startP2pEngine()
        relayManager.startRelayEngine()
        firebaseManager.start()

        viewModelScope.launch {
            seedDefaultCouponsAndProfile()
            refreshSessionKey()
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

        // Tangkap paket Firebase Realtime Database yang masuk secara real-time
        viewModelScope.launch {
            firebaseManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        // Pantau status online untuk mengirim ulang pesan tertunda (outbox retry)
        viewModelScope.launch {
            combine(
                p2pManager.isP2pConnected,
                firebaseManager.isConnected,
                relayManager.isRelayConnected
            ) { p2p, fb, relay -> p2p || fb || relay }
                .collect { isOnline ->
                    if (isOnline) {
                        flushPendingMessages()
                    }
                }
        }
    }

    private suspend fun flushPendingMessages() {
        val undelivered = messageDao.getUndeliveredMessages()
        for (msg in undelivered) {
            val packet = TetherPacket(
                id = msg.id,
                type = msg.type,
                senderId = msg.senderId,
                content = msg.content,
                caption = msg.caption,
                duration = msg.duration,
                lamportTimestamp = msg.lamportTimestamp
            )
            dispatchPacket(packet)
        }
    }

    fun setupRoom(myName: String, partnerName: String, roomCode: String, passcode: String) {
        val app = getApplication<Application>()
        KeystoreManager.saveRoomConfig(app, roomCode, passcode)
        viewModelScope.launch {
            val current = profileDao.getProfile()
            val partnerId = current?.partnerId ?: "partner_1"
            profileDao.saveProfile(
                PartnerProfileEntity(
                    partnerId = partnerId,
                    partnerName = partnerName.trim(),
                    myName = myName.trim(),
                    moodCycle = current?.moodCycle ?: "Happy",
                    publicKey = "",
                    isPaired = true,
                    pairedAt = System.currentTimeMillis()
                )
            )
            refreshSessionKey()
            _isRoomConfigured.value = true
            firebaseManager.start()
        }
    }

    private suspend fun refreshSessionKey() {
        val app = getApplication<Application>()
        val roomCode = KeystoreManager.getRoomCode(app)
        val passcode = KeystoreManager.getRoomPasscode(app)
        if (!roomCode.isNullOrBlank() && !passcode.isNullOrBlank()) {
            activeSessionKey = SessionCryptoEngine.deriveRoomAesKey(roomCode, passcode)
            return
        }

        val profile = profileDao.getProfile() ?: return
        val partnerKeyB64 = profile.publicKey
        if (partnerKeyB64.isBlank() || partnerKeyB64 == "pk_test_sample" || partnerKeyB64 == "tether_ed25519_pub_key_offline") {
            activeSessionKey = null
            return
        }
        try {
            val (myPubB64, myPrivBytes) = KeystoreManager.getOrCreateIdentityKeyPair(getApplication())
            val myPubBytes = SessionCryptoEngine.decodeBase64(myPubB64)
            activeSessionKey = SessionCryptoEngine.deriveSharedAesKey(
                myPrivateKeyBytes = myPrivBytes,
                partnerPublicKeyBase64 = partnerKeyB64,
                myPublicKeyBytes = myPubBytes
            )
            myPrivBytes.fill(0)
        } catch (_: Exception) {
            activeSessionKey = null
        }
    }

    fun getLocalPairingPayload(): PairingPayload {
        val app = getApplication<Application>()
        val deviceId = KeystoreManager.getOrCreateDevicePartnerId(app)
        val (myPubB64, myPrivBytes) = KeystoreManager.getOrCreateIdentityKeyPair(app)
        myPrivBytes.fill(0)
        val myName = partnerProfile.value?.myName ?: "Saya"
        return PairingPayload(
            partnerId = deviceId,
            partnerName = myName,
            publicKey = myPubB64,
            timestamp = System.currentTimeMillis()
        )
    }

    fun completePairing(payload: PairingPayload) {
        viewModelScope.launch {
            val current = profileDao.getProfile()
            profileDao.saveProfile(
                PartnerProfileEntity(
                    partnerId = payload.partnerId,
                    partnerName = payload.partnerName,
                    myName = current?.myName ?: "Saya",
                    moodCycle = current?.moodCycle ?: "Happy",
                    publicKey = payload.publicKey,
                    p2pIpAddress = current?.p2pIpAddress,
                    isPaired = true,
                    pairedAt = payload.timestamp,
                    partnerAvatarUri = current?.partnerAvatarUri,
                    myAvatarUri = current?.myAvatarUri
                )
            )
            refreshSessionKey()
            hapticEngine.playTugHaptic()
        }
    }

    private suspend fun seedDefaultCouponsAndProfile() {
        val app = getApplication<Application>()
        val configured = KeystoreManager.isRoomConfigured(app)
        if (profileDao.getProfile() == null) {
            profileDao.saveProfile(
                PartnerProfileEntity(
                    partnerId = "partner_1",
                    partnerName = "Pasangan",
                    myName = "Saya",
                    moodCycle = "Happy",
                    publicKey = "",
                    isPaired = configured,
                    pairedAt = if (configured) System.currentTimeMillis() else 0L
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

        if (coupleLevelDao.getLevel() == null) {
            coupleLevelDao.saveLevel(CoupleLevelEntity())
        }
    }

    private suspend fun addInteractionXp(xpDelta: Long, isMsg: Boolean = false, isTug: Boolean = false, isCoupon: Boolean = false) {
        val current = coupleLevelDao.getLevel() ?: CoupleLevelEntity()
        val newXp = current.currentXp + xpDelta

        var targetLevel = current.currentLevel
        while (newXp >= CoupleLevelEntity.xpRequiredForLevel(targetLevel)) {
            targetLevel++
        }

        val updated = current.copy(
            currentXp = newXp,
            currentLevel = targetLevel,
            totalMessagesSent = current.totalMessagesSent + if (isMsg) 1 else 0,
            totalTugsSent = current.totalTugsSent + if (isTug) 1 else 0,
            totalCouponsClaimed = current.totalCouponsClaimed + if (isCoupon) 1 else 0,
            lastInteractionTimestamp = System.currentTimeMillis()
        )
        coupleLevelDao.saveLevel(updated)
    }

    fun generateAiPixelAvatar(prompt: String, isForPartner: Boolean) {
        viewModelScope.launch {
            _isGeneratingAvatar.value = true
            try {
                val savedPath = pixelAvatarGenerator.generatePixelAvatar(prompt)
                if (savedPath != null) {
                    val current = profileDao.getProfile()
                    if (current != null) {
                        if (isForPartner) {
                            profileDao.updateProfileData(
                                partnerId = current.partnerId,
                                partnerName = current.partnerName,
                                myName = current.myName,
                                mood = current.moodCycle,
                                partnerAvatarUri = savedPath,
                                myAvatarUri = current.myAvatarUri
                            )
                        } else {
                            profileDao.updateProfileData(
                                partnerId = current.partnerId,
                                partnerName = current.partnerName,
                                myName = current.myName,
                                mood = current.moodCycle,
                                partnerAvatarUri = current.partnerAvatarUri,
                                myAvatarUri = savedPath
                            )
                        }
                    }
                }
            } finally {
                _isGeneratingAvatar.value = false
            }
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
            addInteractionXp(15L, isMsg = true)
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

    private suspend fun dispatchPacket(packet: TetherPacket): Boolean {
        val peerIp = p2pManager.connectedPeerIp.value
        return if (p2pManager.isP2pConnected.value && peerIp != null) {
            val ok = p2pManager.sendPacket(peerIp, packet)
            messageDao.updateDeliveredStatus(packet.id, ok)
            ok
        } else if (firebaseManager.isConnected.value) {
            val ok = firebaseManager.sendPacket(packet)
            messageDao.updateDeliveredStatus(packet.id, ok)
            ok
        } else if (relayManager.isRelayConnected.value) {
            val ok = relayManager.sendPacket(packet)
            messageDao.updateDeliveredStatus(packet.id, ok)
            ok
        } else {
            false
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
                        caption = moodCapitalized,
                        lamportTimestamp = clock,
                        localTimestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                    dispatchPacket(
                        TetherPacket(
                            id = msg.id,
                            type = "MOOD_UPDATE",
                            senderId = "ME",
                            content = msg.content,
                            caption = moodCapitalized,
                            lamportTimestamp = msg.lamportTimestamp
                        )
                    )
                }
                is TetherCommand.Tug -> {
                    sendTugSignal(command.message, showLocalOverlay = true)
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

    fun sendTugSignal(customMessage: String? = null, showLocalOverlay: Boolean = false) {
        viewModelScope.launch {
            val clock = currentLamportClock.incrementAndGet()
            hapticEngine.playTugHaptic()
            val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
            if (showLocalOverlay) {
                TetherOverlayManager.showTugOverlay(
                    context = getApplication(),
                    partnerName = partnerName,
                    partnerAvatarUri = partnerProfile.value?.partnerAvatarUri,
                    onTugBack = { sendTugSignal("Tarik balik! ⚡", showLocalOverlay = false) }
                )
            }
            val noteStr = if (!customMessage.isNullOrBlank()) "\n\"$customMessage\"" else ""
            val msg = MessageEntity(
                id = UUID.randomUUID().toString(),
                senderId = "ME",
                type = "SYSTEM",
                content = "⚡ KAMU MENARIK TETHER! ⚡$noteStr",
                lamportTimestamp = clock,
                localTimestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(msg)
            addInteractionXp(25L, isTug = true)

            dispatchPacket(
                TetherPacket(
                    id = msg.id,
                    type = "TUG",
                    senderId = "ME",
                    content = customMessage ?: "Tether Tug!",
                    lamportTimestamp = msg.lamportTimestamp
                )
            )
        }
    }

    fun sendTouchPulse(touchX: Float, touchY: Float) {
        hapticEngine.playHeartbeatResonance()
        viewModelScope.launch {
            addInteractionXp(5L)
            val clock = currentLamportClock.incrementAndGet()
            dispatchPacket(
                TetherPacket(
                    id = UUID.randomUUID().toString(),
                    type = "HAPTIC_TOUCH",
                    senderId = "ME",
                    content = "TOUCH",
                    lamportTimestamp = clock,
                    touchX = touchX,
                    touchY = touchY
                )
            )
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
            addInteractionXp(25L, isMsg = true)
            dispatchPacket(
                TetherPacket(
                    id = msg.id,
                    type = "POLAROID",
                    senderId = "ME",
                    content = msg.content,
                    caption = msg.caption,
                    lamportTimestamp = msg.lamportTimestamp
                )
            )
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
            addInteractionXp(25L, isMsg = true)
            dispatchPacket(
                TetherPacket(
                    id = msg.id,
                    type = "WHISPER",
                    senderId = "ME",
                    content = msg.content,
                    duration = msg.duration,
                    lamportTimestamp = msg.lamportTimestamp
                )
            )
        }
    }

    fun toggleAudioPlayback(id: String) {
        audioPlaybackJob?.cancel()
        if (_playingAudioId.value == id) {
            _playingAudioId.value = null
            proximitySensor.stopListening()
        } else {
            _playingAudioId.value = id
            proximitySensor.startListening()
            hapticEngine.playTouchPulse()
            audioPlaybackJob = viewModelScope.launch {
                delay(14_000L)
                if (_playingAudioId.value == id) {
                    _playingAudioId.value = null
                    proximitySensor.stopListening()
                }
            }
        }
    }

    fun claimCoupon(couponId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            couponDao.claimCoupon(couponId, now)
            val clock = currentLamportClock.incrementAndGet()
            val couponMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                senderId = "ME",
                type = "COUPON",
                content = couponId,
                caption = "Kupon Kasih Sayang Diklaim",
                lamportTimestamp = clock,
                localTimestamp = now
            )
            messageDao.insertMessage(couponMsg)
            addInteractionXp(50L, isCoupon = true)

            dispatchPacket(
                TetherPacket(
                    id = couponMsg.id,
                    type = "COUPON_CLAIM",
                    senderId = "ME",
                    content = couponId,
                    caption = couponMsg.caption,
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
                addInteractionXp(15L, isMsg = true)
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
            "MOOD_UPDATE" -> {
                val current = profileDao.getProfile()
                val newMood = packet.caption ?: packet.content
                if (current != null && newMood.isNotBlank()) {
                    profileDao.updateMood(current.partnerId, newMood)
                }
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
            "TUG" -> {
                soundManager.playReceiveSound()
                hapticEngine.playTugHaptic()
                addInteractionXp(25L, isTug = true)
                val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                if (TetherOverlayManager.canDrawOverlays(getApplication())) {
                    TetherOverlayManager.showTugOverlay(
                        context = getApplication(),
                        partnerName = partnerName,
                        partnerAvatarUri = partnerProfile.value?.partnerAvatarUri,
                        onTugBack = { sendTugSignal("Tarik balik! ⚡", showLocalOverlay = false) }
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
                val now = System.currentTimeMillis()
                couponDao.claimCoupon(packet.content, now)
                addInteractionXp(50L, isCoupon = true)
                val msg = MessageEntity(
                    id = packet.id,
                    senderId = "PARTNER",
                    type = "COUPON",
                    content = packet.content,
                    caption = packet.caption ?: "Kupon Kasih Sayang Diklaim",
                    lamportTimestamp = updatedClock,
                    localTimestamp = now
                )
                messageDao.insertMessage(msg)
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
                        partnerAvatarUri = profile?.partnerAvatarUri,
                        onTugBack = { sendTugSignal("Tarik balik! ⚡", showLocalOverlay = false) }
                    )
                }
            }
        }
    }

    fun uploadAndSendMedia(context: Context, uri: Uri, caption: String? = null, isFoggy: Boolean = true) {
        viewModelScope.launch {
            val uploadedUrl = DriveUploadManager.uploadFile(context, uri)
            if (uploadedUrl != null) {
                soundManager.playSendSound()
                val clock = currentLamportClock.incrementAndGet()
                val msg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = "ME",
                    type = "POLAROID",
                    content = uploadedUrl,
                    caption = caption ?: "Momen kita",
                    lamportTimestamp = clock,
                    localTimestamp = System.currentTimeMillis(),
                    isFoggy = isFoggy,
                    rotation = (-5..5).random() / 10f
                )
                messageDao.insertMessage(msg)
                addInteractionXp(25L, isMsg = true)
                dispatchPacket(
                    TetherPacket(
                        id = msg.id,
                        type = "POLAROID",
                        senderId = "ME",
                        content = uploadedUrl,
                        caption = msg.caption,
                        lamportTimestamp = msg.lamportTimestamp
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackJob?.cancel()
        p2pManager.stop()
        relayManager.stop()
        firebaseManager.stop()
        proximitySensor.stopListening()
        soundManager.release()
    }
}
