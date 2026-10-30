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
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.tether.app.core.network.config.CloudConfig
import com.tether.app.core.notification.TetherNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.SecretKey

private const val MESSAGE_PAGE_SIZE = 100
private const val MAX_MESSAGE_WINDOW = 1000

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

    // shortcut: jendela pesan dibatasi 1000; pindah ke Paging3 kalau riwayat lebih besar.
    private val messageWindow = MutableStateFlow(MESSAGE_PAGE_SIZE)

    val messages: StateFlow<List<MessageEntity>> = messageWindow
        .flatMapLatest { messageDao.getRecentMessagesFlow(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val canLoadOlderMessages: StateFlow<Boolean> = combine(messages, messageWindow) { list, window ->
        window < MAX_MESSAGE_WINDOW && list.size >= window
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun loadOlderMessages() {
        messageWindow.update { (it + MESSAGE_PAGE_SIZE).coerceAtMost(MAX_MESSAGE_WINDOW) }
    }

    val partnerProfile: StateFlow<PartnerProfileEntity?> = profileDao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val coupleLevel: StateFlow<CoupleLevelEntity?> = coupleLevelDao.getLevelFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    data class LevelUpInfo(val level: Int, val title: String)

    private val _levelUpEvent = MutableStateFlow<LevelUpInfo?>(null)
    val levelUpEvent: StateFlow<LevelUpInfo?> = _levelUpEvent.asStateFlow()

    fun dismissLevelUp() {
        _levelUpEvent.value = null
    }

    private val _isGeneratingAvatar = MutableStateFlow(false)
    val isGeneratingAvatar = _isGeneratingAvatar.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio = _isRecordingAudio.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds = _recordingDurationSeconds.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingTimerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
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

    fun updateRoomConfig(roomCode: String, passcode: String) {
        val app = getApplication<Application>()
        KeystoreManager.saveRoomConfig(app, roomCode, passcode)
        viewModelScope.launch {
            refreshSessionKey()
            _isRoomConfigured.value = true
            firebaseManager.start()
        }
    }

    fun leaveRoom() {
        val app = getApplication<Application>()
        KeystoreManager.clearRoomConfig(app)
        activeSessionKey = null
        _isRoomConfigured.value = false
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

        if (targetLevel > current.currentLevel) {
            hapticEngine.playTugHaptic()
            soundManager.playSendSound()
            _levelUpEvent.value = LevelUpInfo(targetLevel, updated.title())
        }
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
            p2pManager.sendPacket(peerIp, packet)
        } else if (firebaseManager.isConnected.value) {
            firebaseManager.sendPacket(packet)
        } else if (relayManager.isRelayConnected.value) {
            relayManager.sendPacket(packet)
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
                        content = "🎡 ROULETTE KEPUTUSAN:\nOpsi: [${command.options.joinToString(", ")}]\n▶ HASIL PUTARAN: [ ${command.chosen.uppercase()} ] ★",
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

    fun sendPhotoMessage(context: Context, uri: Uri, caption: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val polaroidDir = File(context.filesDir, "polaroids").apply { mkdirs() }
                val localFile = File(polaroidDir, "polaroid_${UUID.randomUUID()}.jpg")

                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val originalBmp = BitmapFactory.decodeStream(stream)
                stream.close()
                if (originalBmp == null) return@launch

                val maxDim = 800
                val longest = maxOf(originalBmp.width, originalBmp.height)
                val scale = if (longest > maxDim) maxDim.toFloat() / longest else 1.0f
                val targetBmp = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBmp,
                        (originalBmp.width * scale).toInt(),
                        (originalBmp.height * scale).toInt(),
                        true
                    )
                } else {
                    originalBmp
                }

                val bos = ByteArrayOutputStream()
                targetBmp.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                val jpegBytes = bos.toByteArray()
                if (targetBmp != originalBmp) targetBmp.recycle()
                originalBmp.recycle()

                FileOutputStream(localFile).use { it.write(jpegBytes) }
                val localPath = localFile.absolutePath

                val wireContent = if (CloudConfig.isDriveConfigured()) {
                    DriveUploadManager.uploadBytes(jpegBytes, "image/jpeg", localFile.name) ?: run {
                        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                        "data:image/jpeg;base64,$b64"
                    }
                } else {
                    val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                    "data:image/jpeg;base64,$b64"
                }

                val clock = currentLamportClock.incrementAndGet()
                val msgId = UUID.randomUUID().toString()
                val finalCaption = caption?.trim()?.takeIf { it.isNotEmpty() }
                val msg = MessageEntity(
                    id = msgId,
                    senderId = "ME",
                    type = "POLAROID",
                    content = localPath,
                    caption = finalCaption,
                    lamportTimestamp = clock,
                    localTimestamp = System.currentTimeMillis(),
                    isFoggy = false,
                    rotation = (-3..3).random().toFloat()
                )

                messageDao.insertMessage(msg)
                addInteractionXp(25L, isMsg = true)
                soundManager.playSendSound()

                dispatchPacket(
                    TetherPacket(
                        id = msg.id,
                        type = "POLAROID",
                        senderId = "ME",
                        content = wireContent,
                        caption = msg.caption,
                        lamportTimestamp = msg.lamportTimestamp
                    )
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gagal kirim foto: ${e.message}")
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

    fun startAudioRecording(context: Context) {
        if (_isRecordingAudio.value) return
        try {
            val whispersDir = File(context.filesDir, "whispers").apply { mkdirs() }
            val file = File(whispersDir, "whisper_${UUID.randomUUID()}.m4a")
            recordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecordingAudio.value = true
            _recordingDurationSeconds.value = 0
            hapticEngine.playTouchPulse()

            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (isActive && _isRecordingAudio.value) {
                    delay(1000L)
                    _recordingDurationSeconds.update { it + 1 }
                    if (_recordingDurationSeconds.value >= 60) {
                        stopAudioRecording(send = true)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Gagal memulai perekaman audio: ${e.message}")
            stopAudioRecording(send = false)
        }
    }

    fun stopAudioRecording(send: Boolean) {
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val recorder = mediaRecorder
        mediaRecorder = null
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        try {
            recorder?.release()
        } catch (_: Exception) {}

        _isRecordingAudio.value = false

        val file = recordingFile
        val durationSec = _recordingDurationSeconds.value
        _recordingDurationSeconds.value = 0
        val durationStr = String.format(Locale.getDefault(), "%d:%02d", durationSec / 60, durationSec % 60)

        if (!send || file == null || !file.exists() || file.length() < 100L || durationSec < 1) {
            file?.delete()
            recordingFile = null
            return
        }

        recordingFile = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = file.readBytes()
                val wireContent = if (CloudConfig.isDriveConfigured()) {
                    DriveUploadManager.uploadBytes(bytes, "audio/mp4", file.name) ?: run {
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        "data:audio/mp4;base64,$b64"
                    }
                } else {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    "data:audio/mp4;base64,$b64"
                }

                val clock = currentLamportClock.incrementAndGet()
                val msgId = UUID.randomUUID().toString()
                val msg = MessageEntity(
                    id = msgId,
                    senderId = "ME",
                    type = "WHISPER",
                    content = file.absolutePath,
                    duration = durationStr,
                    lamportTimestamp = clock,
                    localTimestamp = System.currentTimeMillis(),
                    isDelivered = false,
                    rotation = (-3..3).random().toFloat()
                )

                messageDao.insertMessage(msg)
                addInteractionXp(25L, isMsg = true)
                soundManager.playSendSound()

                dispatchPacket(
                    TetherPacket(
                        id = msg.id,
                        type = "WHISPER",
                        senderId = "ME",
                        content = wireContent,
                        duration = durationStr,
                        lamportTimestamp = msg.lamportTimestamp
                    )
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gagal mengirim voice note: ${e.message}")
            }
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
        audioPlaybackJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        if (_playingAudioId.value == id) {
            _playingAudioId.value = null
            proximitySensor.stopListening()
            return
        }

        val msg = messages.value.firstOrNull { it.id == id }
        val audioPath = msg?.content
        val audioFile = if (audioPath != null) File(audioPath) else null

        _playingAudioId.value = id
        proximitySensor.startListening()
        hapticEngine.playTouchPulse()

        if (audioFile != null && audioFile.exists()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        _playingAudioId.value = null
                        proximitySensor.stopListening()
                        try { it.release() } catch (_: Exception) {}
                        mediaPlayer = null
                    }
                    setOnErrorListener { _, _, _ ->
                        _playingAudioId.value = null
                        proximitySensor.stopListening()
                        try { mediaPlayer?.release() } catch (_: Exception) {}
                        mediaPlayer = null
                        true
                    }
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gagal memutar audio: ${e.message}")
                _playingAudioId.value = null
                proximitySensor.stopListening()
            }
        } else {
            // Fallback untuk audio streaming atau sample mock
            audioPlaybackJob = viewModelScope.launch {
                delay(6000L)
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
        if (packet.type == "DELIVERED_ACK") {
            messageDao.updateDeliveredStatus(packet.content, true)
            return
        }

        val updatedClock = currentLamportClock.updateAndGet { current ->
            maxOf(current, packet.lamportTimestamp) + 1L
        }

        when (packet.type) {
            "TEXT", "POLAROID", "WHISPER" -> {
                soundManager.playReceiveSound()
                addInteractionXp(15L, isMsg = true)
                var resolvedContent = packet.content
                if (packet.type == "POLAROID" && packet.content.startsWith("data:image/")) {
                    try {
                        val b64 = packet.content.substringAfter("base64,")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val polaroidDir = File(getApplication<Application>().filesDir, "polaroids").apply { mkdirs() }
                        val file = File(polaroidDir, "received_${packet.id}.jpg")
                        FileOutputStream(file).use { it.write(bytes) }
                        resolvedContent = file.absolutePath
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Gagal menyimpan foto received: ${e.message}")
                    }
                } else if (packet.type == "WHISPER" && packet.content.startsWith("data:audio/")) {
                    try {
                        val b64 = packet.content.substringAfter("base64,")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val whisperDir = File(getApplication<Application>().filesDir, "whispers").apply { mkdirs() }
                        val file = File(whisperDir, "received_${packet.id}.m4a")
                        FileOutputStream(file).use { it.write(bytes) }
                        resolvedContent = file.absolutePath
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Gagal menyimpan audio received: ${e.message}")
                    }
                }
                val msg = MessageEntity(
                    id = packet.id,
                    senderId = "PARTNER",
                    type = packet.type,
                    content = resolvedContent,
                    caption = packet.caption,
                    duration = packet.duration,
                    lamportTimestamp = updatedClock,
                    localTimestamp = System.currentTimeMillis(),
                    isDelivered = true,
                    isFoggy = packet.type == "POLAROID",
                    rotation = (-10..10).random() / 10f
                )
                messageDao.insertMessage(msg)
                hapticEngine.playHeartbeatResonance()

                val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                when (packet.type) {
                    "TEXT" -> TetherNotificationHelper.showNotification(
                        context = getApplication(),
                        title = partnerName,
                        body = packet.content
                    )
                    "POLAROID" -> TetherNotificationHelper.showNotification(
                        context = getApplication(),
                        title = partnerName,
                        body = if (!packet.caption.isNullOrBlank()) "📷 Foto: ${packet.caption}" else "📷 Mengirim foto polaroid"
                    )
                    "WHISPER" -> TetherNotificationHelper.showNotification(
                        context = getApplication(),
                        title = partnerName,
                        body = "🎤 Pesan suara (${packet.duration ?: "0:10"})"
                    )
                }
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
                    localTimestamp = System.currentTimeMillis(),
                    isDelivered = true
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
                    localTimestamp = System.currentTimeMillis(),
                    isDelivered = true
                )
                messageDao.insertMessage(msg)
                TetherNotificationHelper.showNotification(
                    context = getApplication(),
                    title = "⚡ Tether Sinyal",
                    body = "$partnerName menarik tali Tether kamu!"
                )
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
                    localTimestamp = System.currentTimeMillis(),
                    isDelivered = true
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
                    localTimestamp = now,
                    isDelivered = true
                )
                messageDao.insertMessage(msg)
                hapticEngine.playTugHaptic()
                val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                TetherNotificationHelper.showNotification(
                    context = getApplication(),
                    title = "🎟️ Kupon Diklaim",
                    body = "$partnerName mengklaim kupon: ${packet.caption ?: packet.content}"
                )
            }
            "COUPON_CREATE" -> {
                soundManager.playReceiveSound()
                hapticEngine.playHeartbeatResonance()
                addInteractionXp(30L, isCoupon = true)
                try {
                    val parts = packet.content.split("|")
                    if (parts.size >= 3) {
                        val coupon = CouponEntity(
                            id = parts[0],
                            title = parts[1],
                            description = parts[2],
                            isClaimed = false,
                            claimedAt = null,
                            rotation = (-3..3).random().toFloat()
                        )
                        couponDao.insertCoupon(coupon)
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Gagal menyimpan kupon baru: ${e.message}")
                }
                val partnerName = partnerProfile.value?.partnerName ?: "Pasangan"
                TetherNotificationHelper.showNotification(
                    context = getApplication(),
                    title = "🎟️ Kupon Baru",
                    body = "$partnerName membuat kupon baru: ${packet.caption ?: packet.content}"
                )
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

        if (packet.type != "HAPTIC_TOUCH" && packet.type != "HEARTBEAT") {
            dispatchPacket(
                TetherPacket(
                    id = UUID.randomUUID().toString(),
                    type = "DELIVERED_ACK",
                    senderId = "ME",
                    content = packet.id,
                    lamportTimestamp = updatedClock
                )
            )
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
                    caption = caption?.trim()?.takeIf { it.isNotEmpty() },
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

    fun createCustomCoupon(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val couponId = "c_${UUID.randomUUID().toString().take(8)}"
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val coupon = CouponEntity(
                id = couponId,
                title = cleanTitle,
                description = cleanDesc,
                isClaimed = false,
                claimedAt = null,
                rotation = (-3..3).random().toFloat()
            )
            couponDao.insertCoupon(coupon)
            addInteractionXp(30L, isCoupon = true)

            dispatchPacket(
                TetherPacket(
                    id = UUID.randomUUID().toString(),
                    type = "COUPON_CREATE",
                    senderId = "ME",
                    content = "$couponId|$cleanTitle|$cleanDesc",
                    caption = cleanTitle,
                    lamportTimestamp = currentLamportClock.incrementAndGet()
                )
            )
        }
    }

    fun generateAiPixelArt(context: Context, prompt: String) {
        if (prompt.isBlank() || _isGeneratingAvatar.value) return
        viewModelScope.launch {
            _isGeneratingAvatar.value = true
            try {
                val filePath = withContext(Dispatchers.IO) {
                    pixelAvatarGenerator.generatePixelAvatar(prompt.trim())
                }
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        sendPhotoMessage(
                            context = context,
                            uri = Uri.fromFile(file),
                            caption = prompt.trim()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gagal generate pixel art: ${e.message}")
            } finally {
                _isGeneratingAvatar.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioRecording(send = false)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        audioPlaybackJob?.cancel()
        p2pManager.stop()
        relayManager.stop()
        firebaseManager.stop()
        proximitySensor.stopListening()
        soundManager.release()
    }
}
