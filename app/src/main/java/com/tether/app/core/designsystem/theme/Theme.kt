package com.tether.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TetherColorScheme = lightColorScheme(
    primary = PixelHeartRed,
    onPrimary = PixelPaperSurface,
    primaryContainer = PixelPeachLight,
    onPrimaryContainer = PixelHeartDark,
    secondary = PixelSageGreen,
    onSecondary = PixelPaperSurface,
    background = PixelPaperBg,
    onBackground = PixelDarkInk,
    surface = PixelPaperSurface,
    onSurface = PixelDarkInk,
    surfaceVariant = PixelPaperBg,
    onSurfaceVariant = PixelSecondaryInk
)

@Composable
fun TetherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TetherColorScheme,
        typography = TetherTypography,
        content = content
    )
}
