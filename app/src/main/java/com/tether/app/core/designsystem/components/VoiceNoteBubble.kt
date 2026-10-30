package com.tether.app.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun VoiceNoteBubble(
    duration: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMe: Boolean = false,
    isDelivered: Boolean = false,
    rotation: Float = 0f
) {
    val bubbleBg = if (isMe) Color(0xFFFFF0ED) else Color.White
    val accentColor = if (isMe) PixelHeartRed else PixelDarkInk

    val waveTransition = rememberInfiniteTransition(label = "vnWave")
    val pulseAlpha by waveTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePulse"
    )

    Box(
        modifier = modifier
            .widthIn(min = 180.dp, max = 260.dp)
            .graphicsLayer { rotationZ = rotation }
            .pixelShadow(2.dp, 2.dp)
            .background(bubbleBg)
            .border(2.dp, PixelDarkInk)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Play / Pause Retro Pixel
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor)
                    .border(2.dp, PixelDarkInk)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "■" else "▶",
                    fontFamily = PixelFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Kolom Waveform & Durasi
            Column(modifier = Modifier.weight(1f)) {
                // Waveform Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barHeights = listOf(6, 14, 18, 10, 20, 16, 8, 14, 18, 12, 6)
                    barHeights.forEachIndexed { index, h ->
                        val barColor = if (isPlaying) {
                            if (index % 2 == 0) accentColor else accentColor.copy(alpha = pulseAlpha)
                        } else {
                            PixelDarkInk.copy(alpha = 0.75f)
                        }
                        val dynamicH = if (isPlaying && index % 2 == 1) {
                            (h * pulseAlpha).coerceAtLeast(4f)
                        } else {
                            h.toFloat()
                        }
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(dynamicH.dp)
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Baris Info Durasi & Status Terkirim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = duration,
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelMutedInk
                    )

                    if (isMe) {
                        Text(
                            text = if (isDelivered) "✓✓" else "✓",
                            fontFamily = PixelFont,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDelivered) Color(0xFF2E7D32) else PixelMutedInk
                        )
                    }
                }
            }
        }
    }
}
