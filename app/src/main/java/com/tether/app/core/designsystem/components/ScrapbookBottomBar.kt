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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.domain.command.TetherCommandParser
import com.tether.app.ui.chat.components.CommandSuggestionBar
import java.util.Locale

@Composable
fun ScrapbookBottomBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCouponClick: () -> Unit,
    onAiPixelClick: () -> Unit = {},
    isRecordingAudio: Boolean = false,
    recordingDurationSeconds: Int = 0,
    onStartRecordAudio: () -> Unit = {},
    onStopRecordAudio: (send: Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val suggestions = remember(inputText) {
        TetherCommandParser.getSuggestions(inputText)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .background(PixelPaperBg)
    ) {
        if (suggestions.isNotEmpty()) {
            CommandSuggestionBar(
                suggestions = suggestions,
                onSelectCommand = { cmd ->
                    onInputChange(cmd.command + " ")
                }
            )
        }

        // Garis Pembatas Struktural Atas Bottom Bar (2.dp Solid Pixel Dark Ink)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PixelDarkInk)
        )

        if (isRecordingAudio) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Batal Merekam
                Box(
                    modifier = Modifier
                        .pixelShadow(2.dp, 2.dp)
                        .background(Color.White)
                        .border(2.dp, PixelDarkInk)
                        .clickable { onStopRecordAudio(false) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "BATAL",
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelDarkInk
                    )
                }

                // Indikator Merekam dengan Titik Merah Berkedip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val blinkTransition = rememberInfiniteTransition(label = "recBlink")
                    val blinkAlpha by blinkTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer { alpha = blinkAlpha }
                            .background(PixelHeartRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val minutes = recordingDurationSeconds / 60
                    val seconds = recordingDurationSeconds % 60
                    Text(
                        text = String.format(Locale.getDefault(), "MEREKAM %d:%02d", minutes, seconds),
                        fontFamily = PixelFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelHeartRed
                    )
                }

                // Tombol Kirim Voice Note
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
                            .background(PixelHeartRed)
                            .border(2.dp, PixelDarkInk)
                            .clickable { onStopRecordAudio(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_send),
                            contentDescription = "Kirim VN",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Kamera Pixel Kotak
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
                            .clickable { onCameraClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_camera),
                            contentDescription = "Camera",
                            modifier = Modifier.size(18.dp),
                            tint = PixelDarkInk
                        )
                    }
                }

                // Tombol AI Generate Gambar Pixel
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
                            .clickable { onAiPixelClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_sparkle),
                            contentDescription = "AI Pixel",
                            modifier = Modifier.size(18.dp),
                            tint = PixelHeartRed
                        )
                    }
                }

                // Tombol Buku Kupon Pixel Kotak
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
                            .clickable { onCouponClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_coupon),
                            contentDescription = "Coupons",
                            modifier = Modifier.size(18.dp),
                            tint = PixelDarkInk
                        )
                    }
                }

                // Kotak Input Memo Pixel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
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
                            .border(2.dp, PixelDarkInk)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Tulis memo...",
                                fontFamily = PixelFont,
                                fontSize = 11.sp,
                                color = PixelMutedInk
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            textStyle = TextStyle(
                                fontFamily = PixelFont,
                                fontSize = 12.sp,
                                color = PixelDarkInk
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Tombol Aksi Kanan: Kirim Text atau Mulai Voice Note
                if (inputText.isNotBlank()) {
                    Box(modifier = Modifier.size(40.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .background(Color(0xFF38354A))
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(PixelDarkInk)
                                .border(2.dp, PixelDarkInk)
                                .clickable { onSendClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pixel_send),
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
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
                                .border(2.dp, PixelDarkInk)
                                .clickable { onStartRecordAudio() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pixel_sound),
                                contentDescription = "Voice Note",
                                tint = PixelHeartRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
