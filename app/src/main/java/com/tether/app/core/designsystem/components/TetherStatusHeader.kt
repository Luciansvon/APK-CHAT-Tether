package com.tether.app.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelSageGreen
import com.tether.app.core.designsystem.theme.PixelWashiTint
import com.tether.app.core.designsystem.util.BitmapLoader

@Composable
fun TetherStatusHeader(
    partnerName: String,
    connectionStatus: String,
    isP2pActive: Boolean,
    moodPhase: String,
    avatarUri: String? = null,
    level: Int = 1,
    levelTitle: String = "Benang Baru",
    levelProgressRatio: Float = 0f,
    onProfileClick: () -> Unit,
    onTouchpadClick: () -> Unit,
    onVaultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAlpha = if (isP2pActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    } else {
        1.0f
    }

    val avatarBitmap = remember(avatarUri) {
        BitmapLoader.decodeSampledAvatar(avatarUri, reqSizePx = 128)
    }

    // Status connection colors: Olive/Sage for P2P, muted ink for standby (avoids mixing with user terracotta red)
    val statusColor = if (isP2pActive) Color(0xFF2E6930) else PixelMutedInk

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelPaperBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sisi Kiri: Avatar Patch & Status (Ketuk untuk edit profil)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .pixelShadow(2.dp, 2.dp)
                        .background(Color.White)
                        .border(2.dp, PixelDarkInk),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Partner Avatar",
                            modifier = Modifier.size(38.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tether_patch),
                            contentDescription = "Partner Avatar",
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Square Pixel Online Indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.BottomEnd)
                            .background(if (isP2pActive) PixelSageGreen else PixelMutedInk)
                            .border(1.5.dp, PixelDarkInk)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = partnerName.uppercase(),
                            fontFamily = PixelFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PixelDarkInk
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_heart),
                            contentDescription = "Love",
                            modifier = Modifier.size(11.dp),
                            tint = PixelHeartRed
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer { alpha = if (isP2pActive) pulseAlpha else 1f }
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = connectionStatus.uppercase(),
                            fontFamily = PixelFont,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Sisi Kanan: Mood Badge, Tombol QR Pairing & Tombol Benang (Tinggi seragam 34.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mood Badge Kotak (Height 34.dp)
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .pixelShadow(2.dp, 2.dp)
                        .background(PixelWashiTint)
                        .border(1.5.dp, PixelDarkInk)
                        .clickable { onProfileClick() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = moodPhase.replace(Regex("[^A-Za-z0-9 ]"), "").trim().uppercase(),
                        fontFamily = PixelFont,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelDarkInk
                    )
                }

                // Tombol Scrapbook Vault (⊞) Kotak (Size 34.dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .pixelShadow(2.dp, 2.dp)
                        .background(Color.White)
                        .border(2.dp, PixelDarkInk)
                        .clickable { onVaultClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_grid),
                        contentDescription = "Scrapbook Vault",
                        modifier = Modifier.size(16.dp),
                        tint = PixelDarkInk
                    )
                }

                // Tombol Benang Tether Vector Kotak (Size 34.dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .pixelShadow(2.dp, 2.dp)
                        .background(Color.White)
                        .border(2.dp, PixelDarkInk)
                        .clickable { onTouchpadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_thread),
                        contentDescription = "Tether Thread",
                        modifier = Modifier.size(18.dp),
                        tint = PixelDarkInk
                    )
                }
            }
        }

        // Garis Pembatas Struktural Sticky Header & Mini Level XP Bar
        Column(modifier = Modifier.fillMaxWidth().clickable { onProfileClick() }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelWashiTint)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LV.$level $levelTitle".uppercase(),
                    fontFamily = PixelFont,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelDarkInk
                )
                Text(
                    text = "${(levelProgressRatio * 100).toInt()}% XP",
                    fontFamily = PixelFont,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelMutedInk
                )
            }
            // XP Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(levelProgressRatio.coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .background(PixelHeartRed)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(PixelDarkInk)
            )
        }
    }
}
