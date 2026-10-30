package com.tether.app.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tether.app.core.designsystem.components.pixelShadow
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoCaptionDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirmSend: (caption: String?) -> Unit
) {
    var captionText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .pixelShadow(4.dp, 4.dp)
                    .background(PixelPaperBg)
                    .border(2.5.dp, PixelDarkInk)
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "◆ KIRIM FOTO POLAROID ◆",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PixelDarkInk
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Thumbnail Preview
                    val thumbBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = imageUri) {
                        value = withContext(Dispatchers.IO) {
                            BitmapLoader.decodeSampledBitmap(imageUri.toString(), reqSizePx = 400)
                        }
                    }.value

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color(0xFFE5DBC2))
                            .border(2.dp, PixelDarkInk),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbBitmap != null) {
                            Image(
                                bitmap = thumbBitmap,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "[ MEMUAT FOTO ]",
                                fontFamily = PixelFont,
                                fontSize = 10.sp,
                                color = PixelMutedInk
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Caption Bebas (Opsional)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
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
                            if (captionText.isEmpty()) {
                                Text(
                                    text = "Tulis caption bebas (opsional)...",
                                    fontFamily = PixelFont,
                                    fontSize = 11.sp,
                                    color = PixelMutedInk
                                )
                            }
                            BasicTextField(
                                value = captionText,
                                onValueChange = { captionText = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 11.5.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Aksi Batal & Kirim
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Batal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .pixelShadow(2.dp, 2.dp)
                                .background(Color.White)
                                .border(2.dp, PixelDarkInk)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BATAL",
                                fontFamily = PixelFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PixelDarkInk
                            )
                        }

                        // Kirim
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .pixelShadow(2.dp, 2.dp)
                                .background(PixelHeartRed)
                                .border(2.dp, PixelDarkInk)
                                .clickable {
                                    val cleanCaption = captionText.trim().takeIf { it.isNotEmpty() }
                                    onConfirmSend(cleanCaption)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KIRIM FOTO",
                                fontFamily = PixelFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDetailViewerDialog(
    imagePath: String,
    caption: String? = null,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0141312))
        ) {
            // Tombol Tutup (X) di Kanan Atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(40.dp)
                    .pixelShadow(2.dp, 2.dp)
                    .background(Color.White)
                    .border(2.dp, PixelDarkInk)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = PixelDarkInk,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Kanvas Foto dengan Pinch-to-Zoom dan Pan
            val photoBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = imagePath) {
                value = withContext(Dispatchers.IO) {
                    BitmapLoader.decodeSampledBitmap(imagePath, reqSizePx = 1080)
                }
            }.value

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = caption ?: "Detail Foto",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                } else {
                    Text(
                        text = "[ MEMUAT FOTO RESOLUSI PENUH... ]",
                        fontFamily = PixelFont,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }

            // Caption di Bawah jika Ada
            if (!caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .pixelShadow(2.dp, 2.dp)
                        .background(PixelPaperBg)
                        .border(2.dp, PixelDarkInk)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"$caption\"",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PixelDarkInk,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
