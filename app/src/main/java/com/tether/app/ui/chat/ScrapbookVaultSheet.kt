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
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.components.WhisperCassetteCard
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember(context) { TetherDatabase.getInstance(context) }
    val coupons by db.couponDao().getAllCouponsFlow().collectAsState(initial = emptyList())
    val messages by db.messageDao().getAllMessagesFlow().collectAsState(initial = emptyList())

    // 0: Kupon, 1: Memori (Polaroid & Kaset), 2: Kunci P2P Pairing
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val payload = withContext(Dispatchers.IO) {
            if (pairingPayloadProvider != null) {
                pairingPayloadProvider()
            } else {
                val (pubB64, privBytes) = KeystoreManager.getOrCreateIdentityKeyPair(context)
                privBytes.fill(0)
                PairingPayload(
                    partnerId = KeystoreManager.getOrCreateDevicePartnerId(context),
                    partnerName = "Bima",
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
                val tabs = listOf("🎟️ KUPON", "📷 MEMORI", "🔐 PAIRING")
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
                            WashiRibbon(text = "◆ DINDING FOTO & BISIKAN ◆")
                        }

                        if (polaroidAndAudioMessages.isEmpty()) {
                            item {
                                // Default Polaroid & Kaset Contoh
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

                                WhisperCassetteCard(
                                    duration = "0:14",
                                    isPlaying = playingAudioId == "vault_default_sample",
                                    onPlayClick = { onToggleAudio?.invoke("vault_default_sample") }
                                )
                            }
                        } else {
                            items(polaroidAndAudioMessages, key = { it.id }) { msg ->
                                if (msg.type == "POLAROID") {
                                    PolaroidFrame(
                                        caption = msg.caption ?: "Momen kita",
                                        timestamp = "Tersimpan",
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
                                                painter = painterResource(id = R.drawable.ic_pixel_camera),
                                                contentDescription = "Camera",
                                                modifier = Modifier.size(32.dp),
                                                tint = PixelDarkInk
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "[ GOSOK BUKA FOTO ]",
                                                fontFamily = PixelFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PixelDarkInk
                                            )
                                        }
                                    }
                                } else {
                                    WhisperCassetteCard(
                                        duration = msg.duration ?: "0:10",
                                        isPlaying = playingAudioId == msg.id,
                                        onPlayClick = { onToggleAudio?.invoke(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: KUNCI PAIRING & MILESTONE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WashiRibbon(text = "◆ KUNCI PASANGAN OFFLINE ◆")

                        Spacer(modifier = Modifier.height(20.dp))

                        // Box QR Pixel
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
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                qrBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "QR Pairing",
                                        modifier = Modifier.size(190.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Pindai kode ini langsung dari kamera pasangan untuk menghubungkan obrolan privat tanpa server.",
                            fontFamily = PixelFont,
                            fontSize = 10.5.sp,
                            color = PixelSecondaryInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
