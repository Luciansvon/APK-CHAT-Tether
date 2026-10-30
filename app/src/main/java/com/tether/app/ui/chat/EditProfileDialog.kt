package com.tether.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tether.app.R
import com.tether.app.core.ai.PixelAvatarGenerator
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelHeartRed
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.PixelPeachLight
import com.tether.app.core.designsystem.theme.PixelSecondaryInk
import com.tether.app.core.designsystem.theme.PixelWashiTint
import com.tether.app.core.designsystem.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditProfileDialog(
    initialPartnerName: String,
    initialMyName: String,
    initialMood: String,
    initialAvatarUri: String?,
    onDismiss: () -> Unit,
    onSave: (partnerName: String, myName: String, mood: String, avatarUri: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var partnerName by remember { mutableStateOf(initialPartnerName) }
    var myName by remember { mutableStateOf(initialMyName) }
    var mood by remember { mutableStateOf(initialMood) }
    var avatarUri by remember { mutableStateOf(initialAvatarUri) }
    var aiPromptText by remember { mutableStateOf("cewek rambut coklat senyum manis") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    val pixelGenerator = remember { PixelAvatarGenerator(context) }

    // Launcher galeri untuk memilih foto profil secara asinkron di IO thread & membersihkan file avatar lama
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    try {
                        val avatarDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
                        avatarDir.listFiles()?.forEach { oldFile ->
                            if (oldFile.name.startsWith("partner_avatar_")) {
                                oldFile.delete()
                            }
                        }
                        val targetFile = File(avatarDir, "partner_avatar_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        targetFile.absolutePath
                    } catch (_: Exception) {
                        null
                    }
                }
                if (savedPath != null) {
                    avatarUri = savedPath
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Drop Shadow Solid Pixel
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(PixelDarkInk)
            )

            // Kontainer Dialog Pixel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelPaperBg)
                    .border(2.dp, PixelDarkInk)
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Judul
                    Box(
                        modifier = Modifier
                            .background(PixelPeachLight)
                            .border(1.5.dp, PixelDarkInk)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "◆ EDIT PROFIL TETHER ◆",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PixelDarkInk
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preview Avatar Foto Profil
                    Box(
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(PixelDarkInk)
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White)
                                .border(2.dp, PixelDarkInk),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = remember(avatarUri) {
                                BitmapLoader.decodeSampledAvatar(avatarUri, reqSizePx = 256)
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(66.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_tether_patch),
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(60.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Aksi Avatar: Galeri & AI Pixel 9Router
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Pilih Foto dari Galeri
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .border(1.5.dp, PixelDarkInk)
                                .clickable { galleryLauncher.launch("image/*") }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pixel_camera),
                                    contentDescription = "Pilih Foto",
                                    modifier = Modifier.size(13.dp),
                                    tint = PixelDarkInk
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GALERI",
                                    fontFamily = PixelFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    color = PixelDarkInk
                                )
                            }
                        }

                        // Tombol AI Pixel Generator (9Router)
                        Box(
                            modifier = Modifier
                                .background(PixelPeachLight)
                                .border(1.5.dp, PixelDarkInk)
                                .clickable(enabled = !isGeneratingAi) {
                                    coroutineScope.launch {
                                        isGeneratingAi = true
                                        val newAvatarPath = pixelGenerator.generatePixelAvatar(aiPromptText)
                                        if (newAvatarPath != null) {
                                            avatarUri = newAvatarPath
                                        }
                                        isGeneratingAi = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = PixelDarkInk
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MERAJUT...",
                                        fontFamily = PixelFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp,
                                        color = PixelDarkInk
                                    )
                                } else {
                                    Text(
                                        text = "✨ AI PIXEL (9ROUTER)",
                                        fontFamily = PixelFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp,
                                        color = PixelDarkInk
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Deskripsi Prompt AI Pixel
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DESKRIPSI KARAKTER PIXEL AI",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = PixelSecondaryInk
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(Color.White)
                                .border(1.dp, PixelDarkInk)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = aiPromptText,
                                onValueChange = { aiPromptText = it },
                                textStyle = TextStyle(
                                    fontFamily = PixelFont,
                                    fontSize = 9.sp,
                                    color = PixelDarkInk
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Preset Cepat Karakter Pixel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "cewek pita merah" to "Cewek",
                                "cowok keren santai" to "Cowok",
                                "kucing cozy" to "Kucing"
                            ).forEach { (presetPrompt, label) ->
                                Box(
                                    modifier = Modifier
                                        .background(PixelWashiTint)
                                        .border(1.dp, PixelDarkInk)
                                        .clickable {
                                            aiPromptText = presetPrompt
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = label.uppercase(),
                                        fontFamily = PixelFont,
                                        fontSize = 7.5.sp,
                                        color = PixelDarkInk
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Nama Pasangan
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NAMA PASANGAN",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = PixelSecondaryInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color.White)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = partnerName,
                                onValueChange = { partnerName = it },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Nama Anda
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NAMA KAMU",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = PixelSecondaryInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color.White)
                                .border(1.5.dp, PixelDarkInk)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = myName,
                                onValueChange = { myName = it },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pilihan Mood / Fase Pasangan
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "STATUS / FASE MOOD PASANGAN",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = PixelSecondaryInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val presets = listOf("Luteal", "Follicular", "Ovulation", "Menstrual", "Happy", "Tired")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            presets.take(3).forEach { p ->
                                val isSelected = mood.equals(p, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) PixelHeartRed else Color.White)
                                        .border(1.5.dp, PixelDarkInk)
                                        .clickable { mood = p }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.uppercase(),
                                        fontFamily = PixelFont,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else PixelDarkInk
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            presets.drop(3).forEach { p ->
                                val isSelected = mood.equals(p, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) PixelHeartRed else Color.White)
                                        .border(1.5.dp, PixelDarkInk)
                                        .clickable { mood = p }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.uppercase(),
                                        fontFamily = PixelFont,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else PixelDarkInk
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tombol Aksi (Batal & Simpan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tombol Batal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BATAL",
                                    fontFamily = PixelFont,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PixelMutedInk
                                )
                            }
                        }

                        // Tombol Simpan
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .background(PixelHeartRed)
                                    .border(2.dp, PixelDarkInk)
                                    .clickable {
                                        onSave(partnerName, myName, mood, avatarUri)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SIMPAN",
                                    fontFamily = PixelFont,
                                    fontSize = 10.5.sp,
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
}
