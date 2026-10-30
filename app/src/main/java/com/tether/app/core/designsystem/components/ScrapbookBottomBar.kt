package com.tether.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.domain.command.TetherCommandParser
import com.tether.app.ui.chat.components.CommandSuggestionBar

@Composable
fun ScrapbookBottomBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCouponClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(inputText) {
        TetherCommandParser.getSuggestions(inputText)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Kamera Pixel Kotak
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
                        .clickable { onCameraClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_camera),
                        contentDescription = "Camera",
                        modifier = Modifier.size(20.dp),
                        tint = PixelDarkInk
                    )
                }
            }

            // Tombol Buku Kupon Pixel Kotak
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
                        .clickable { onCouponClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_coupon),
                        contentDescription = "Coupons",
                        modifier = Modifier.size(20.dp),
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

            // Tombol Kirim Pixel Kotak dengan Icon Pixel Send
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
                        .clickable {
                            if (inputText.isNotBlank()) onSendClick()
                        },
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
        }
    }
}
