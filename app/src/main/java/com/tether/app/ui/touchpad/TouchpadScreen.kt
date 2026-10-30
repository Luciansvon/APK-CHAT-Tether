package com.tether.app.ui.touchpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelGridDot
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import com.tether.app.core.sensor.HapticResonanceEngine

@Composable
fun TouchpadScreen(
    hapticEngine: HapticResonanceEngine,
    onBackClick: () -> Unit
) {
    var touchPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelPaperBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPoint = offset
                        hapticEngine.playHeartbeatResonance()
                        tryAwaitRelease()
                        touchPoint = null
                    }
                )
            }
    ) {
        // Pixel Background Grid
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

        // Tombol Kembali Pixel Kotak
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 10.dp, start = 16.dp)
                .size(38.dp)
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

        // Card Petunjuk di Tengah
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(28.dp)
        ) {
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
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_heart),
                        contentDescription = "Heart",
                        tint = PixelHeartRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TETHER HANDHOLD",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PixelDarkInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tempel dan tahan jarimu di layar untuk mengirimkan getaran detak jantung ke pasanganmu secara langsung.",
                        fontFamily = PixelFont,
                        fontSize = 10.5.sp,
                        color = PixelSecondaryInk,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Efek Ripple Pixel Berundak saat disentuh
        touchPoint?.let { pos ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSizes = listOf(140.dp.toPx(), 90.dp.toPx(), 40.dp.toPx(), 16.dp.toPx())
                val alphas = listOf(0.15f, 0.35f, 0.6f, 1.0f)

                boxSizes.forEachIndexed { idx, s ->
                    drawRect(
                        color = PixelHeartRed.copy(alpha = alphas[idx]),
                        topLeft = Offset(pos.x - s / 2, pos.y - s / 2),
                        size = Size(s, s)
                    )
                }
            }
        }
    }
}
