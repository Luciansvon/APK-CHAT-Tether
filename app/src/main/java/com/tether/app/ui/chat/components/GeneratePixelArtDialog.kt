package com.tether.app.ui.chat.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tether.app.R
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg

@Composable
fun GeneratePixelArtDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (prompt: String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }

    val presetIdeas = listOf(
        "Kucing oranye tidur",
        "Pemandangan senja pixel",
        "Secangkir kopi hangat",
        "Bunga mawar merah 16-bit"
    )

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = !isLoading) { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .pixelShadow(4.dp, 4.dp)
                    .background(PixelPaperBg)
                    .border(2.5.dp, PixelDarkInk)
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pixel_sparkle),
                            contentDescription = "AI Sparkle",
                            modifier = Modifier.size(16.dp),
                            tint = PixelHeartRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI GENERATOR GAMBAR PIXEL",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = PixelDarkInk
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Deskripsikan objek gambar pixel 16-bit yang ingin kamu buat & kirimkan ke scrapbook:",
                        fontFamily = PixelFont,
                        fontSize = 9.5.sp,
                        color = PixelDarkInk,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kotak Input Prompt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (promptText.isEmpty()) {
                                Text(
                                    text = "Tulis permintaan gambar...",
                                    fontFamily = PixelFont,
                                    fontSize = 10.5.sp,
                                    color = PixelMutedInk
                                )
                            }
                            BasicTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 11.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pilihan Preset Cepat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetIdeas.take(2).forEach { idea ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFEDE7D7))
                                    .border(1.dp, PixelDarkInk)
                                    .clickable(enabled = !isLoading) { promptText = idea }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = idea,
                                    fontFamily = PixelFont,
                                    fontSize = 8.sp,
                                    color = PixelDarkInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PixelHeartRed,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MERENDER PIXEL 16-BIT...",
                                fontFamily = PixelFont,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PixelHeartRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Tombol Aksi Batal & Buat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .pixelShadow(2.dp, 2.dp)
                                .background(Color.White)
                                .border(2.dp, PixelDarkInk)
                                .clickable(enabled = !isLoading) { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BATAL",
                                fontFamily = PixelFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PixelDarkInk
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .pixelShadow(2.dp, 2.dp)
                                .background(if (promptText.isNotBlank() && !isLoading) PixelHeartRed else Color(0xFFC4BDB0))
                                .border(2.dp, PixelDarkInk)
                                .clickable(enabled = promptText.isNotBlank() && !isLoading) {
                                    onGenerate(promptText.trim())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GENERATE",
                                fontFamily = PixelFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
