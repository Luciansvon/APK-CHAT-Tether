package com.tether.app.ui.chat

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.database.TetherDatabase
import com.tether.app.core.designsystem.components.FavorCouponTicket
import com.tether.app.core.designsystem.components.PolaroidFrame
import com.tether.app.core.designsystem.components.VoiceNoteBubble
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.ui.chat.components.CreateCouponDialog
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import com.tether.app.core.pairing.PairingPayload
import com.tether.app.core.pairing.QrCodeHelper
import com.tether.app.core.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapbookVaultSheet(
    initialTab: Int = 0,
    playingAudioId: String? = null,
    onToggleAudio: ((String) -> Unit)? = null,
    pairingPayloadProvider: (() -> PairingPayload)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onClaimCoupon: ((String) -> Unit)? = null,
    onCreateCoupon: ((title: String, description: String) -> Unit)? = null,
    onPhotoClick: ((imagePath: String, caption: String?) -> Unit)? = null,
    onLeaveRoom: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember(context) { TetherDatabase.getInstance(context) }
    val coupons by db.couponDao().getAllCouponsFlow().collectAsState(initial = emptyList())
    var mediaLimit by remember { mutableIntStateOf(100) }
    val messages by remember(mediaLimit) { db.messageDao().getMediaMessagesFlow(mediaLimit) }
        .collectAsState(initial = emptyList())

    // 0: Kupon, 1: Memori (Polaroid & Kaset), 2: Kamar & Kamera
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCreateCouponDialog by remember { mutableStateOf(false) }
    val roomCode = remember { KeystoreManager.getRoomCode(context)?.uppercase() ?: "-" }

    LaunchedEffect(Unit) {
        val payload = withContext(Dispatchers.IO) {
            if (pairingPayloadProvider != null) {
                pairingPayloadProvider()
            } else {
                val (pubB64, privBytes) = KeystoreManager.getOrCreateIdentityKeyPair(context)
                privBytes.fill(0)
                PairingPayload(
                    partnerId = KeystoreManager.getOrCreateDevicePartnerId(context),
                    partnerName = "Saya",
                    publicKey = pubB64,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
        qrBitmap = QrCodeHelper.generateQrBitmap(payload.toJson(), 400)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixelPaperBg,
        scrimColor = Color(0x661E1D1A),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .background(PixelPaperBg)
        ) {
            // Header Vault dengan Batas Garis Pixel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_grid),
                        contentDescription = "Vault",
                        modifier = Modifier.size(20.dp),
                        tint = PixelDarkInk
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BRANKAS SCRAPBOOK",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PixelDarkInk
                    )
                }

                // Tombol Tutup Kotak Pixel (48dp touch target)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White)
                        .border(1.5.dp, PixelDarkInk)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        modifier = Modifier.size(16.dp),
                        tint = PixelDarkInk
                    )
                }
            }

            // Garis Pembatas Pixel (2.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(PixelDarkInk)
            )

            // Bar Tab Pilihan (4-Point Grid, 12dp margin)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("🎟️ KUPON", "📷 MEMORI", "🔐 KAMAR")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .background(PixelDarkInk)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(if (isSelected) PixelHeartRed else Color.White)
                                .border(1.5.dp, PixelDarkInk)
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title.replace(Regex("[^A-Za-z0-9 ]"), "").trim(),
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else PixelDarkInk
                            )
                        }
                    }
                }
            }

            // Konten Tab
            when (selectedTab) {
                0 -> {
                    // TAB 1: DOMPET KUPON AKTIF & TERPAKAI
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            WashiRibbon(text = "◆ DOMPET KUPON BERDUA ◆")
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(38.dp)
                                    .pixelShadow(2.dp, 2.dp)
                                    .background(PixelHeartRed)
                                    .border(2.dp, PixelDarkInk)
                                    .clickable { showCreateCouponDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ BUAT KUPON BARU",
                                    fontFamily = PixelFont,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (coupons.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada kupon yang dibuat.",
                                        fontFamily = PixelFont,
                                        fontSize = 11.sp,
                                        color = PixelMutedInk
                                    )
                                }
                            }
                        } else {
                            items(coupons, key = { it.id }) { coupon ->
                                FavorCouponTicket(
                                    title = coupon.title,
                                    description = coupon.description,
                                    isClaimed = coupon.isClaimed,
                                    onClaimClick = {
                                        if (onClaimCoupon != null) {
                                            onClaimCoupon(coupon.id)
                                        } else {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    db.couponDao().claimCoupon(coupon.id, System.currentTimeMillis())
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 2: DINDING MEMORI (POLAROID & KASET BISIK)
                    val polaroidAndAudioMessages = messages.filter { it.type == "POLAROID" || it.type == "WHISPER" }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            WashiRibbon(text = "◆ DINDING FOTO & SUARA ◆")
                        }

                        if (polaroidAndAudioMessages.isEmpty()) {
                            item {
                                // Default Polaroid & Suara Contoh
                                PolaroidFrame(
                                    caption = "nanti malem ke sini yuk?",
                                    timestamp = "Tersimpan di Brankas",
                                    isFoggy = false,
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "SUDUT BACA & KOPI",
                                            fontFamily = PixelFont,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PixelDarkInk
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                VoiceNoteBubble(
                                    duration = "0:14",
                                    isPlaying = playingAudioId == "vault_default_sample",
                                    onPlayClick = { onToggleAudio?.invoke("vault_default_sample") }
                                )
                            }
                        } else {
                            items(polaroidAndAudioMessages, key = { it.id }) { msg ->
                                if (msg.type == "POLAROID") {
                                    PolaroidFrame(
                                        caption = msg.caption,
                                        timestamp = "Tersimpan",
                                        isFoggy = false,
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .clickable { onPhotoClick?.invoke(msg.content, msg.caption) }
                                    ) {
                                        val thumb = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                                            initialValue = null,
                                            key1 = msg.content
                                        ) {
                                            value = withContext(Dispatchers.IO) {
                                                com.tether.app.core.designsystem.util.BitmapLoader
                                                    .decodeSampledBitmap(msg.content, reqSizePx = 400)
                                            }
                                        }.value

                                        if (thumb != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = thumb,
                                                contentDescription = msg.caption ?: "Foto",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
                                                    contentDescription = "Camera",
                                                    modifier = Modifier.size(32.dp),
                                                    tint = PixelDarkInk
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "[ FOTO ]",
                                                    fontFamily = PixelFont,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PixelDarkInk
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    VoiceNoteBubble(
                                        duration = msg.duration ?: "0:10",
                                        isPlaying = playingAudioId == msg.id,
                                        onPlayClick = { onToggleAudio?.invoke(msg.id) }
                                    )
                                }
                            }

                            if (polaroidAndAudioMessages.size >= mediaLimit && mediaLimit < 1000) {
                                item {
                                    Text(
                                        text = "[ MUAT LEBIH BANYAK ]",
                                        fontFamily = PixelFont,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PixelSecondaryInk,
                                        modifier = Modifier
                                            .clickable { mediaLimit = (mediaLimit + 100).coerceAtMost(1000) }
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: KAMAR RAHASIA BERDUA
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WashiRibbon(text = "◆ KAMAR RAHASIA BERDUA ◆")

                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(2.dp, PixelDarkInk)
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "KODE KAMAR",
                                    fontFamily = PixelFont,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PixelSecondaryInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = roomCode,
                                    fontFamily = PixelFont,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PixelDarkInk
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pesan di kamar ini terenkripsi AES-256-GCM langsung dari HP ke HP.",
                                    fontFamily = PixelFont,
                                    fontSize = 9.5.sp,
                                    color = PixelSecondaryInk,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // QR Kunci Identitas
                        Box {
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
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                qrBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "QR Kunci Identitas",
                                        modifier = Modifier.size(150.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tunjukkan kode kamar di atas kepada pasanganmu. Kalian berdua cukup memasukkan kode dan kata sandi yang sama.",
                            fontFamily = PixelFont,
                            fontSize = 10.sp,
                            color = PixelSecondaryInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (onLeaveRoom != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(Color.White)
                                    .border(1.5.dp, PixelHeartRed)
                                    .clickable {
                                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                            onLeaveRoom()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "KELUAR & GANTI KAMAR",
                                    fontFamily = PixelFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PixelHeartRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCouponDialog) {
        CreateCouponDialog(
            onDismiss = { showCreateCouponDialog = false },
            onCreateCoupon = { title, desc ->
                showCreateCouponDialog = false
                if (onCreateCoupon != null) {
                    onCreateCoupon(title, desc)
                } else {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val id = "c_${java.util.UUID.randomUUID().toString().take(8)}"
                            db.couponDao().insertCoupon(
                                com.tether.app.core.database.entity.CouponEntity(
                                    id = id,
                                    title = title,
                                    description = desc
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}
