package com.tether.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.domain.command.CommandSuggestion

@Composable
fun CommandSuggestionBar(
    suggestions: List<CommandSuggestion>,
    onSelectCommand: (CommandSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelPaperBg)
            .border(width = 1.dp, color = PixelDarkInk.copy(alpha = 0.2f))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            suggestions.forEach { item ->
                Box {
                    // Pixel Hard Drop Shadow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(PixelDarkInk)
                    )
                    // Chip Container
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.5.dp, PixelDarkInk)
                            .clickable { onSelectCommand(item) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.command,
                                fontFamily = PixelFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = PixelHeartRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• ${item.description}",
                                fontFamily = PixelFont,
                                fontSize = 9.sp,
                                color = PixelMutedInk
                            )
                        }
                    }
                }
            }
        }
    }
}
