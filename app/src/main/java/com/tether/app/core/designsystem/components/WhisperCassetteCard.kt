package com.tether.app.core.designsystem.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPeachLight
import com.tether.app.core.designsystem.theme.PixelSecondaryInk

@Composable
fun WhisperCassetteCard(
    duration: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    Box(
        modifier = modifier.width(250.dp)
    ) {
        // Solid Pixel Drop Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(PixelDarkInk)
        )

        // Cassette Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(2.dp, PixelDarkInk)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Kaset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_cassette),
                            contentDescription = "Cassette",
                            modifier = Modifier.size(15.dp),
                            tint = PixelSecondaryInk
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PESAN BISIK",
                            fontFamily = PixelFont,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PixelSecondaryInk
                        )
                    }
                    Text(
                        text = duration,
                        fontFamily = PixelFont,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelMutedInk
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Body Kaset: Tombol Play & Gelombang Suara (Waveform)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Play Kotak Pixel
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PixelHeartRed)
                            .border(2.dp, PixelDarkInk)
                            .clickable { onPlayClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "■" else "▶",
                            fontFamily = PixelFont,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Waveform Bars Stepped
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(6, 12, 20, 16, 22, 14, 8, 18, 10, 5)
                        heights.forEachIndexed { index, h ->
                            val barColor = if (index in 2..5 && isPlaying) PixelHeartRed else PixelDarkInk
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(h.dp)
                                    .background(barColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Label Sensor Proximity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixelPeachLight)
                        .border(1.5.dp, PixelHeartRed)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_sound),
                        contentDescription = "Speaker",
                        modifier = Modifier.size(12.dp),
                        tint = PixelHeartRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEKATKAN KE TELINGA",
                        fontFamily = PixelFont,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelHeartRed
                    )
                }
            }
        }
    }
}
