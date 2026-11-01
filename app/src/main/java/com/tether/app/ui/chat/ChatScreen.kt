package com.tether.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.components.FavorCouponTicket
import com.tether.app.core.designsystem.components.LevelUpDialog
import com.tether.app.core.designsystem.components.PolaroidFrame
import com.tether.app.core.designsystem.components.ScrapbookBottomBar
import com.tether.app.core.designsystem.components.TetherStatusHeader
import com.tether.app.core.designsystem.components.VoiceNoteBubble
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.components.pixelGridBackground
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelGridDot
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.util.BitmapLoader
import com.tether.app.ui.chat.components.GeneratePixelArtDialog
import com.tether.app.ui.chat.components.PhotoCaptionDialog
import com.tether.app.ui.chat.components.PhotoDetailViewerDialog
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToTouchpad: () -> Unit,
    onNavigateToCoupons: () -> Unit = {},
    onNavigateToPairing: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val partnerProfile by viewModel.partnerProfile.collectAsState()
    val coupleLevel by viewModel.coupleLevel.collectAsState()
    val levelUpEvent by viewModel.levelUpEvent.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isP2pActive by viewModel.p2pManager.isP2pConnected.collectAsState()
    val isFirebaseActive by viewModel.firebaseManager.isConnected.collectAsState()
    val isRelayActive by viewModel.relayManager.isRelayConnected.collectAsState()

    val connectionLabel = when {
        isP2pActive -> "Langsung"
        isFirebaseActive || isRelayActive -> "Tersambung"
        else -> "Standby"
    }
    val isConnected = isP2pActive || isFirebaseActive || isRelayActive
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
    val recordingDurationSeconds by viewModel.recordingDurationSeconds.collectAsState()
    val canLoadOlder by viewModel.canLoadOlderMessages.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var vaultInitialTab by remember { mutableIntStateOf(0) }
    var selectedPhotoUriForCaption by remember { mutableStateOf<Uri?>(null) }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    var viewingPhotoCaption by remember { mutableStateOf<String?>(null) }
    var showAiPixelDialog by remember { mutableStateOf(false) }
    val isGeneratingAi by viewModel.isGeneratingAvatar.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUriForCaption = uri
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording(context)
        }
    }

    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            // Item 0 adalah header, sehingga indeks item pesan terakhir = messages.size
            listState.animateScrollToItem(messages.size)
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialPartnerName = partnerProfile?.partnerName ?: "Pasangan",
            initialMyName = partnerProfile?.myName ?: "Saya",
            initialMood = partnerProfile?.moodCycle ?: "Happy",
            initialAvatarUri = partnerProfile?.partnerAvatarUri,
            onDismiss = { showEditProfileDialog = false },
            onSave = { pName, mName, pMood, pAvatar ->
                viewModel.updateProfile(
                    partnerName = pName,
                    myName = mName,
                    mood = pMood,
                    partnerAvatarUri = pAvatar
                )
            },
            onUpdateRoom = { roomCode, passcode ->
                viewModel.updateRoomConfig(roomCode, passcode)
            },
            onLeaveRoom = {
                showEditProfileDialog = false
                viewModel.leaveRoom()
            }
        )
    }

    if (showVaultSheet) {
        ScrapbookVaultSheet(
            initialTab = vaultInitialTab,
            playingAudioId = playingAudioId,
            onToggleAudio = { audioId -> viewModel.toggleAudioPlayback(audioId) },
            pairingPayloadProvider = { viewModel.getLocalPairingPayload() },
            onClaimCoupon = { couponId -> viewModel.claimCoupon(couponId) },
            onCreateCoupon = { title, desc -> viewModel.createCustomCoupon(title, desc) },
            onPhotoClick = { path, caption ->
                viewingPhotoPath = path
                viewingPhotoCaption = caption
            },
            onLeaveRoom = {
                showVaultSheet = false
                viewModel.leaveRoom()
            },
            onDismiss = { showVaultSheet = false }
        )
    }

    LevelUpDialog(
        visible = levelUpEvent != null,
        level = levelUpEvent?.level ?: 1,
        title = levelUpEvent?.title ?: "",
        onDismiss = { viewModel.dismissLevelUp() }
    )

    Scaffold(
        topBar = {
            TetherStatusHeader(
                partnerName = partnerProfile?.partnerName ?: "Pasangan",
                connectionStatus = connectionLabel,
                isP2pActive = isConnected,
                moodPhase = partnerProfile?.moodCycle ?: "Happy",
                avatarUri = partnerProfile?.partnerAvatarUri,
                level = coupleLevel?.currentLevel ?: 1,
                levelTitle = coupleLevel?.title() ?: "Benang Baru",
                levelProgressRatio = coupleLevel?.levelProgressRatio() ?: 0f,
                onProfileClick = { showEditProfileDialog = true },
                onTouchpadClick = onNavigateToTouchpad,
                onVaultClick = {
                    vaultInitialTab = 0
                    showVaultSheet = true
                }
            )
        },
        bottomBar = {
            ScrapbookBottomBar(
                inputText = inputText,
                onInputChange = viewModel::onInputTextChange,
                onSendClick = viewModel::sendTextMessage,
                onCameraClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onCouponClick = {
                    vaultInitialTab = 0
                    showVaultSheet = true
                },
                onAiPixelClick = {
                    showAiPixelDialog = true
                },
                isRecordingAudio = isRecordingAudio,
                recordingDurationSeconds = recordingDurationSeconds,
                onStartRecordAudio = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.startAudioRecording(context)
                    } else {
                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecordAudio = { send ->
                    viewModel.stopAudioRecording(send)
                }
            )
        },
        containerColor = PixelPaperBg
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pixelGridBackground(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pita Tanggal Pixel
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WashiRibbon(
                            text = "◆ 24 OKTOBER — HARI KE-412 BERSAMA ◆"
                        )
                        if (canLoadOlder) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "[ MUAT PESAN LAMA ]",
                                fontFamily = PixelFont,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PixelSecondaryInk,
                                modifier = Modifier
                                    .clickable { viewModel.loadOlderMessages() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Pesan Default Awal
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            ChatBubbleThem(
                                text = "Sayang, liat kafe ini deh pas aku jalan pulang tadi.",
                                time = "14:12"
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            PolaroidFrame(
                                caption = "nanti malem ke sini yuk?",
                                timestamp = "14:13 • [TERHUBUNG]",
                                isFoggy = true
                            ) {
                                // Ilustrasi Pixel Kafe Vektor
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE5DBC2)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_pixel_coffee),
                                        contentDescription = "Coffee",
                                        modifier = Modifier.size(36.dp),
                                        tint = PixelDarkInk
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "SUDUT BACA & KOPI",
                                        fontFamily = PixelFont,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PixelDarkInk
                                    )
                                    Text(
                                        text = "16-BIT COZY CORNER",
                                        fontFamily = PixelFont,
                                        fontSize = 8.sp,
                                        color = PixelSecondaryInk
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            ChatBubbleMe(
                                text = "Bagus banget! Udah aku simpen di daftar kencan kita yaa.",
                                time = "14:15",
                                isDelivered = true
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            VoiceNoteBubble(
                                duration = "0:14",
                                isPlaying = playingAudioId == "default_sample",
                                onPlayClick = { viewModel.toggleAudioPlayback("default_sample") }
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            FavorCouponTicket(
                                title = "KUPON KASIH SAYANG #04",
                                description = "1x Pijat Punggung Bebas Request",
                                isClaimed = true,
                                onClaimClick = {}
                            )
                        }
                    }
                }

                // Render Pesan Dinamis
                items(messages, key = { it.id }) { message ->
                    val timeStr = remember(message.localTimestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.localTimestamp))
                    }
                    val bubbleAlignment = if (message.senderId == "ME") Alignment.CenterEnd else Alignment.CenterStart

                    when (message.type) {
                        "TEXT" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = bubbleAlignment) {
                                if (message.senderId == "ME") {
                                    ChatBubbleMe(
                                        text = message.content,
                                        time = timeStr,
                                        caption = message.caption,
                                        isDelivered = message.isDelivered
                                    )
                                } else {
                                    ChatBubbleThem(text = message.content, time = timeStr, caption = message.caption)
                                }
                            }
                        }
                        "SYSTEM" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SystemAnnouncementCard(text = message.content)
                            }
                        }
                        "POLAROID" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = bubbleAlignment) {
                                PolaroidFrame(
                                    caption = message.caption,
                                    timestamp = timeStr,
                                    isFoggy = message.isFoggy,
                                    rotation = message.rotation,
                                    modifier = Modifier.clickable {
                                        viewingPhotoPath = message.content
                                        viewingPhotoCaption = message.caption
                                    }
                                ) {
                                    val photoBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = message.content) {
                                        value = withContext(Dispatchers.IO) {
                                            BitmapLoader.decodeSampledBitmap(message.content, reqSizePx = 512)
                                        }
                                    }.value

                                    if (photoBitmap != null) {
                                        Image(
                                            bitmap = photoBitmap,
                                            contentDescription = message.caption ?: "Foto Polaroid",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFE5DBC2)),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_pixel_camera),
                                                contentDescription = "Photo",
                                                modifier = Modifier.size(32.dp),
                                                tint = PixelDarkInk
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "[ FOTO POLAROID ]",
                                                fontFamily = PixelFont,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PixelDarkInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "WHISPER" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = bubbleAlignment) {
                                VoiceNoteBubble(
                                    duration = message.duration ?: "0:10",
                                    isPlaying = playingAudioId == message.id,
                                    onPlayClick = { viewModel.toggleAudioPlayback(message.id) },
                                    isMe = message.senderId == "ME",
                                    isDelivered = message.isDelivered,
                                    rotation = message.rotation
                                )
                            }
                        }
                        "COUPON" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = bubbleAlignment) {
                                FavorCouponTicket(
                                    title = message.caption ?: "KUPON KASIH SAYANG",
                                    description = "Tiket #${message.content.uppercase()} telah diklaim",
                                    isClaimed = true,
                                    onClaimClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }

    selectedPhotoUriForCaption?.let { uri ->
        PhotoCaptionDialog(
            imageUri = uri,
            onDismiss = { selectedPhotoUriForCaption = null },
            onConfirmSend = { caption ->
                viewModel.sendPhotoMessage(context, uri, caption)
                selectedPhotoUriForCaption = null
            }
        )
    }

    viewingPhotoPath?.let { path ->
        PhotoDetailViewerDialog(
            imagePath = path,
            caption = viewingPhotoCaption,
            onDismiss = {
                viewingPhotoPath = null
                viewingPhotoCaption = null
            }
        )
    }

    if (showAiPixelDialog) {
        GeneratePixelArtDialog(
            isLoading = isGeneratingAi,
            onDismiss = { showAiPixelDialog = false },
            onGenerate = { prompt ->
                viewModel.generateAiPixelArt(context, prompt)
                showAiPixelDialog = false
            }
        )
    }
}

@Composable
fun SystemAnnouncementCard(
    text: String
) {
    Box(
        modifier = Modifier
            .pixelShadow(2.dp, 2.dp)
            .background(Color(0xFFFFF6D6))
            .border(1.5.dp, PixelDarkInk)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            color = PixelDarkInk,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun ChatBubbleThem(
    text: String,
    time: String,
    caption: String? = null
) {
    Box(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 270.dp)
            .pixelShadow(3.dp, 3.dp)
            .background(Color.White)
            .border(2.dp, PixelDarkInk)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.widthIn(min = 96.dp, max = 246.dp)) {
            if (!caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFECEB))
                        .border(1.dp, PixelDarkInk)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = caption,
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = PixelHeartRed
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = text,
                fontFamily = PixelFont,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = PixelDarkInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                fontFamily = PixelFont,
                fontSize = 8.5.sp,
                color = PixelMutedInk,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun ChatBubbleMe(
    text: String,
    time: String,
    caption: String? = null,
    isDelivered: Boolean = false
) {
    Box(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 270.dp)
            .pixelShadow(3.dp, 3.dp)
            .background(PixelHeartRed)
            .border(2.dp, PixelDarkInk)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.widthIn(min = 96.dp, max = 246.dp)) {
            if (!caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color.White)
                        .border(1.dp, PixelDarkInk)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = caption,
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = PixelDarkInk
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = text,
                fontFamily = PixelFont,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDelivered) "$time  ✓✓" else "$time  ✓",
                fontFamily = PixelFont,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFAF6F0),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
