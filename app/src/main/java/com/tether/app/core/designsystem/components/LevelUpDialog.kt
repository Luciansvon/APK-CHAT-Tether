package com.tether.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import kotlinx.coroutines.delay

@Composable
fun LevelUpDialog(
    visible: Boolean,
    level: Int,
    title: String,
    onDismiss: () -> Unit
) {
    if (!visible) return

    LaunchedEffect(Unit) {
        delay(3500)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val pulseTransition = rememberInfiniteTransition(label = "levelPulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconPulse"
            )

            Box(
                modifier = Modifier
                    .width(280.dp)
                    .pixelShadow(4.dp, 4.dp)
                    .background(Color(0xFFFFF6D6))
                    .border(2.dp, PixelDarkInk)
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WashiRibbon(text = "★ LEVEL UP! ★")

                    Spacer(modifier = Modifier.height(14.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_heart),
                        contentDescription = "Level Up Heart",
                        tint = PixelHeartRed,
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "LV. $level",
                        fontFamily = PixelFont,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelHeartRed
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title.uppercase(),
                        fontFamily = PixelFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelDarkInk,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ikatan kalian berdua semakin erat!",
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        color = PixelSecondaryInk,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.5.dp, PixelDarkInk)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "[ TAP UNTUK LANJUT ]",
                            fontFamily = PixelFont,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PixelDarkInk
                        )
                    }
                }
            }
        }
    }
}
