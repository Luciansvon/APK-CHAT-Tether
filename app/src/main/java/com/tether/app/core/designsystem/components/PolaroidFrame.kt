package com.tether.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelFrameCard
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelWashiTint

@Composable
fun PolaroidFrame(
    caption: String,
    timestamp: String,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    isFoggy: Boolean = false,
    content: @Composable () -> Unit
) {
    var scratchProgress by remember { mutableFloatStateOf(if (isFoggy) 0.0f else 1.0f) }
    val animatedFogAlpha by animateFloatAsState(
        targetValue = (1.0f - scratchProgress).coerceIn(0.0f, 1.0f),
        label = "fogAlpha"
    )

    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .width(235.dp)
    ) {
        // Solid Pixel Drop Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(PixelDarkInk)
        )

        // Polaroid Frame Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelFrameCard)
                .border(2.dp, PixelDarkInk)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Washi Tape Pixel
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .offset(y = (-14).dp)
                        .background(PixelWashiTint)
                        .border(1.5.dp, PixelDarkInk)
                )

                // Bingkai Foto Pixel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .background(Color(0xFFE5DBC2))
                        .border(2.dp, PixelDarkInk)
                ) {
                    content()

                    // Fog Reveal (Usap Embun)
                    if (isFoggy && animatedFogAlpha > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xE6E5DBC2))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        scratchProgress = (scratchProgress + (dragAmount.getDistance() / 600f)).coerceAtMost(1f)
                                    }
                                }
                        ) {
                            Text(
                                text = "[ GOSOK BUKA FOTO ]",
                                fontFamily = PixelFont,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PixelDarkInk,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(PixelFrameCard)
                                    .border(1.5.dp, PixelDarkInk)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Caption Pixel Monospace
                Text(
                    text = "\"$caption\"",
                    fontFamily = PixelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PixelDarkInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timestamp,
                    fontFamily = PixelFont,
                    fontSize = 9.sp,
                    color = PixelMutedInk,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
