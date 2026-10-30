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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg

@Composable
fun CreateCouponDialog(
    onDismiss: () -> Unit,
    onCreateCoupon: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() }
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
                    Text(
                        text = "◆ BUAT KUPON KASIH SAYANG ◆",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PixelDarkInk
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Judul Kupon
                    Text(
                        text = "JUDUL KUPON",
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelDarkInk,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Contoh: Bebas Cuci Piring...",
                                    fontFamily = PixelFont,
                                    fontSize = 11.sp,
                                    color = PixelMutedInk
                                )
                            }
                            BasicTextField(
                                value = title,
                                onValueChange = { title = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 11.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Deskripsi Kupon
                    Text(
                        text = "DESKRIPSI / KETENTUAN",
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelDarkInk,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Bisa dipakai kapan saja tanpa syarat!",
                                    fontFamily = PixelFont,
                                    fontSize = 10.5.sp,
                                    color = PixelMutedInk
                                )
                            }
                            BasicTextField(
                                value = description,
                                onValueChange = { description = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 11.sp,
                                    color = PixelDarkInk
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                .clickable { onDismiss() },
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
                                .background(if (title.isNotBlank()) PixelHeartRed else Color(0xFFC4BDB0))
                                .border(2.dp, PixelDarkInk)
                                .clickable(enabled = title.isNotBlank()) {
                                    val desc = if (description.isNotBlank()) description.trim() else "Kupon istimewa tanpa syarat!"
                                    onCreateCoupon(title.trim(), desc)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BUAT KUPON",
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
