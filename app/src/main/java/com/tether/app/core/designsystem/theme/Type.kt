package com.tether.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PixelFont = FontFamily.Monospace

val TetherTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        color = PixelDarkInk
    ),
    titleMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = PixelDarkInk
    ),
    bodyLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        color = PixelDarkInk
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PixelDarkInk
    ),
    labelSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        color = PixelMutedInk
    ),
    headlineSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = PixelDarkInk
    )
)
