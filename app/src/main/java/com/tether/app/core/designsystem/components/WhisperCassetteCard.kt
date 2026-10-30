package com.tether.app.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WhisperCassetteCard(
    duration: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    VoiceNoteBubble(
        duration = duration,
        isPlaying = isPlaying,
        onPlayClick = onPlayClick,
        modifier = modifier,
        rotation = rotation
    )
}
