package com.tether.app.core.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.tether.app.core.designsystem.util.BitmapLoader

@Composable
fun TetherTugOverlayView(
    partnerName: String,
    partnerAvatarUri: String? = null,
    onTugBack: () -> Unit,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Animasi partikel melayang (Floating Pixel Hearts)
    val particleY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleY"
    )
    val particleAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleAlpha"
    )

    val avatarBitmap = remember(partnerAvatarUri) {
        BitmapLoader.decodeSampledAvatar(partnerAvatarUri, reqSizePx = 128)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Partikel Hati Piksel Melayang Bebas di Atas Layar (Visual Magic Compose!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-18).dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pixel_heart),
                contentDescription = null,
                tint = PixelHeartRed,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        translationY = particleY
                        alpha = particleAlpha
                    }
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_pixel_heart),
                contentDescription = null,
                tint = Color(0xFFE5A93C),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        translationY = particleY * 1.3f
                        alpha = particleAlpha
                    }
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_pixel_heart),
                contentDescription = null,
                tint = PixelHeartRed,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        translationY = particleY * 0.8f
                        alpha = particleAlpha
                    }
            )
        }

        // Solid Pixel Hard Drop Shadow (4.dp offset)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(PixelDarkInk)
        )

        // Kontainer Utama Pop-up (16-bit Neo-Brutalist Frame)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPaperBg)
                .border(2.5.dp, PixelDarkInk)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Pop-up: Pita Ribbon & Tombol Tutup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(PixelHeartRed)
                            .border(1.5.dp, PixelDarkInk)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "◆ ⚡ TETHER TUG! ⚡ ◆",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    }

                    // Tombol Tutup Kotak Pixel [ X ]
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White)
                            .border(1.5.dp, PixelDarkInk)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontFamily = PixelFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PixelDarkInk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Baris Info Pengirim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Patch Kotak
                    Box(modifier = Modifier.size(40.dp)) {
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
                                .border(1.5.dp, PixelDarkInk),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_tether_patch),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = partnerName.uppercase(),
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PixelDarkInk
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pixel_heart),
                                contentDescription = null,
                                tint = PixelHeartRed,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MENARIK TETHER!",
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = PixelHeartRed
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Getaran detak terhubung • Langsung",
                            fontFamily = PixelFont,
                            fontSize = 8.5.sp,
                            color = PixelMutedInk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Aksi Nyata (Fitts's Law: Area sentuh nyaman)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol 1: Balas Tarikan
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
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
                                .background(PixelHeartRed)
                                .border(1.5.dp, PixelDarkInk)
                            .clickable {
                                onTugBack()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[ ⚡ TARIK BALIK! ]",
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Tombol 2: Buka Chat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
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
                                .background(Color.White)
                                .border(1.5.dp, PixelDarkInk)
                            .clickable {
                                onOpenChat()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[ 💬 BUKA CHAT ]",
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = PixelDarkInk
                            )
                        }
                    }
                }
            }
        }
    }
}
