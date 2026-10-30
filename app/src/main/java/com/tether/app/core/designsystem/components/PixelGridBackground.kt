package com.tether.app.core.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelGridDot

/**
 * Menggambar pola pixel dither grid menggunakan batch drawPoints ter-cache.
 * Bebas alokasi objek per-frame dan hanya 1 GPU draw call (vs ~1670 drawRect per-frame sebelumnya).
 */
fun Modifier.pixelGridBackground(
    dotColor: Color = PixelGridDot,
    stepDp: Dp = 16.dp,
    dotSizeDp: Dp = 1.8.dp
): Modifier = this.drawWithCache {
    val step = stepDp.toPx()
    val strokeWidth = dotSizeDp.toPx()
    val points = mutableListOf<Offset>()
    var x = 0f
    while (x < size.width) {
        var y = 0f
        while (y < size.height) {
            points.add(Offset(x, y))
            y += step
        }
        x += step
    }
    onDrawBehind {
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = dotColor,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }
}

/**
 * Menggambar bayangan solid retro pixel tanpa perlu membuat Box matchParentSize ganda di layout pass.
 */
fun Modifier.pixelShadow(
    offsetX: Dp = 3.dp,
    offsetY: Dp = 3.dp,
    shadowColor: Color = PixelDarkInk
): Modifier = this.drawBehind {
    drawRect(
        color = shadowColor,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size
    )
}
