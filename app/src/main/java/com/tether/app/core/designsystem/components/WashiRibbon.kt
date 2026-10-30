package com.tether.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelWashiTint

@Composable
fun WashiRibbon(
    text: String,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation }
    ) {
        // Solid Pixel Drop Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(PixelDarkInk)
        )
        // Foreground Pixel Ribbon
        Box(
            modifier = Modifier
                .background(PixelWashiTint)
                .border(2.dp, PixelDarkInk)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = PixelDarkInk,
                letterSpacing = 0.5.sp
            )
        }
    }
}
