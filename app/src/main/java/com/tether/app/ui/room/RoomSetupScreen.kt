package com.tether.app.ui.room

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.components.WashiRibbon
import com.tether.app.core.designsystem.components.pixelGridBackground
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelPeachLight
import com.tether.app.core.designsystem.theme.PixelSecondaryInk

@Composable
fun RoomSetupScreen(
    initialMyName: String = "",
    initialPartnerName: String = "",
    onJoinRoom: (myName: String, partnerName: String, roomCode: String, passcode: String) -> Unit
) {
    var myName by remember { mutableStateOf(initialMyName) }
    var partnerName by remember { mutableStateOf(initialPartnerName) }
    var roomCode by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelPaperBg)
            .pixelGridBackground()
            .statusBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WashiRibbon(
                text = "◆ KAMAR RAHASIA BERDUA ◆"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_pixel_heart),
                contentDescription = "Heart",
                tint = PixelHeartRed,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Masukkan Kode Kamar dan Kata Sandi yang sama di kedua HP kalian agar otomatis terhubung.",
                fontFamily = PixelFont,
                fontSize = 11.sp,
                color = PixelSecondaryInk,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Kotak Form Pixel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelShadow(4.dp, 4.dp)
                    .background(Color.White)
                    .border(2.dp, PixelDarkInk)
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Nama Kamu
                    Column {
                        Text(
                            text = "NAMA KAMU:",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PixelDarkInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PixelPaperBg)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            BasicTextField(
                                value = myName,
                                onValueChange = { myName = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (myName.isEmpty()) {
                                Text(
                                    text = "Tulis namamu",
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelSecondaryInk
                                )
                            }
                        }
                    }

                    // 2. Nama Pasangan
                    Column {
                        Text(
                            text = "NAMA PASANGAN:",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PixelDarkInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PixelPaperBg)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            BasicTextField(
                                value = partnerName,
                                onValueChange = { partnerName = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (partnerName.isEmpty()) {
                                Text(
                                    text = "Tulis nama pasangan",
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelSecondaryInk
                                )
                            }
                        }
                    }

                    // 3. Nomor / Kode Kamar
                    Column {
                        Text(
                            text = "NOMOR / KODE KAMAR:",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PixelDarkInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PixelPeachLight)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            BasicTextField(
                                value = roomCode,
                                onValueChange = { roomCode = it.replace(" ", "").uppercase() },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (roomCode.isEmpty()) {
                                Text(
                                    text = "Misal: KITA-1234 atau RAHASIA",
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelSecondaryInk
                                )
                            }
                        }
                    }

                    // 4. Kata Sandi Kamar
                    Column {
                        Text(
                            text = "KATA SANDI KAMAR:",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PixelDarkInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PixelPaperBg)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            BasicTextField(
                                value = passcode,
                                onValueChange = { passcode = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (passcode.isEmpty()) {
                                Text(
                                    text = "Kata sandi rahasia berdua",
                                    fontFamily = PixelFont,
                                    fontSize = 12.sp,
                                    color = PixelSecondaryInk
                                )
                            }
                        }
                    }

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            fontFamily = PixelFont,
                            fontSize = 10.sp,
                            color = PixelHeartRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tombol Submit
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pixelShadow(2.dp, 2.dp)
                            .background(PixelHeartRed)
                            .border(2.dp, PixelDarkInk)
                            .clickable {
                                if (myName.isBlank()) {
                                    errorMessage = "Mohon isi namamu"
                                } else if (partnerName.isBlank()) {
                                    errorMessage = "Mohon isi nama pasangan"
                                } else if (roomCode.isBlank()) {
                                    errorMessage = "Mohon tentukan kode kamar"
                                } else if (passcode.isBlank()) {
                                    errorMessage = "Mohon tentukan kata sandi kamar"
                                } else {
                                    errorMessage = null
                                    onJoinRoom(myName, partnerName, roomCode, passcode)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MASUK KE KAMAR KITA ❤",
                            fontFamily = PixelFont,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
