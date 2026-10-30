package com.tether.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.database.entity.MessageEntity
import com.tether.app.core.designsystem.components.FavorCouponTicket
import com.tether.app.core.designsystem.components.PolaroidFrame
import com.tether.app.core.designsystem.components.ScrapbookBottomBar
import com.tether.app.core.designsystem.components.TetherStatusHeader
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.components.WhisperCassetteCard
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelGridDot
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelSecondaryInk

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToTouchpad: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val partnerProfile by viewModel.partnerProfile.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isP2pActive by viewModel.p2pManager.isP2pConnected.collectAsState()
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var vaultInitialTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialPartnerName = partnerProfile?.partnerName ?: "Lina",
            initialMyName = partnerProfile?.myName ?: "Bima",
            initialMood = partnerProfile?.moodCycle ?: "Luteal",
            initialAvatarUri = partnerProfile?.partnerAvatarUri,
            onDismiss = { showEditProfileDialog = false },
            onSave = { pName, mName, pMood, pAvatar ->
                viewModel.updateProfile(
                    partnerName = pName,
                    myName = mName,
                    mood = pMood,
                    partnerAvatarUri = pAvatar
                )
            }
        )
    }

    if (showVaultSheet) {
        ScrapbookVaultSheet(
            initialTab = vaultInitialTab,
            onDismiss = { showVaultSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TetherStatusHeader(
                partnerName = partnerProfile?.partnerName ?: "Pasangan",
                connectionStatus = if (isP2pActive) "P2P 0-Data" else "Standby",
                isP2pActive = isP2pActive,
                moodPhase = partnerProfile?.moodCycle ?: "Luteal",
                avatarUri = partnerProfile?.partnerAvatarUri,
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
                onCameraClick = viewModel::sendPolaroidSample,
                onCouponClick = {
                    vaultInitialTab = 0
                    showVaultSheet = true
                }
            )
        },
        containerColor = PixelPaperBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pola Pixel Dither Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 16.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawRect(
                            color = PixelGridDot,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(1.5f, 1.5f)
                        )
                        y += step
                    }
                    x += step
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pita Tanggal Pixel
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WashiRibbon(
                            text = "◆ 24 OKTOBER — HARI KE-412 BERSAMA ◆"
                        )
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
                                timestamp = "14:13 • [P2P-DIRECT]",
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
                                time = "14:15"
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
                            WhisperCassetteCard(
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
                    when (message.type) {
                        "TEXT" -> {
                            if (message.senderId == "ME") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    ChatBubbleMe(text = message.content, time = "baru saja", caption = message.caption)
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    ChatBubbleThem(text = message.content, time = "baru saja", caption = message.caption)
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
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                PolaroidFrame(
                                    caption = message.caption ?: "Momen kita",
                                    timestamp = "baru saja",
                                    isFoggy = message.isFoggy
                                ) {
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
                                            text = "[ GOSOK UNTUK BUKA FOTO ]",
                                            fontFamily = PixelFont,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PixelDarkInk
                                        )
                                    }
                                }
                            }
                        }
                        "WHISPER" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                WhisperCassetteCard(
                                    duration = message.duration ?: "0:10",
                                    isPlaying = playingAudioId == message.id,
                                    onPlayClick = { viewModel.toggleAudioPlayback(message.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemAnnouncementCard(
    text: String
) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(PixelDarkInk)
        )
        Box(
            modifier = Modifier
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
}

@Composable
fun ChatBubbleThem(
    text: String,
    time: String,
    caption: String? = null
) {
    Box(modifier = Modifier.widthIn(min = 120.dp, max = 270.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(PixelDarkInk)
        )
        Box(
            modifier = Modifier
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
}

@Composable
fun ChatBubbleMe(
    text: String,
    time: String,
    caption: String? = null
) {
    Box(modifier = Modifier.widthIn(min = 120.dp, max = 270.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(PixelDarkInk)
        )
        Box(
            modifier = Modifier
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
                    text = time,
                    fontFamily = PixelFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFAF6F0),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
