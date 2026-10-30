package com.tether.app.ui.pairing

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelGridDot
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import com.tether.app.core.pairing.PairingPayload
import com.tether.app.core.pairing.QrCodeHelper
import com.tether.app.core.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PairingScreen(
    pairingPayloadProvider: (() -> PairingPayload)? = null,
    onCompletePairing: ((PairingPayload) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
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
        qrBitmap = QrCodeHelper.generateQrBitmap(payload.toJson(), 480)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(38.dp)) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(PixelDarkInk)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White)
                            .border(2.dp, PixelDarkInk)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = PixelDarkInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "SAMBUNGAN TETHER",
                    fontFamily = PixelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PixelDarkInk
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        },
        containerColor = PixelPaperBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 16.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawRect(
                            color = PixelGridDot,
                            topLeft = Offset(x, y),
                            size = Size(1.5f, 1.5f)
                        )
                        y += step
                    }
                    x += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WashiRibbon(
                    text = "◆ KUNCI PASANGAN EKSKLUSIF 1-ON-1 ◆"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Kartu Polaroid Pembungkus QR Code Pixel
                Box {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(PixelDarkInk)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .border(2.dp, PixelDarkInk)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code Pairing",
                                modifier = Modifier.size(230.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Pindai kode ini pada perangkat pasanganmu untuk menghubungkan Tether P2P tanpa akun publik.",
                    fontFamily = PixelFont,
                    fontSize = 11.sp,
                    color = PixelSecondaryInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Kembali Pixel
                Box(modifier = Modifier.height(42.dp)) {
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
                            .clickable { onBackClick() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KEMBALI KE SCRAPBOOK",
                            fontFamily = PixelFont,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
