package com.tether.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PixelAvatarGenerator(
    private val context: Context,
    private val baseUrl: String = "",
    private val apiKey: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun generatePixelAvatar(userPrompt: String): String? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext generateFallbackPreset(userPrompt)
        }
        val systemPrompt = """
            You are a 16-bit retro pixel art character generator for Tether cozy couple app.
            Generate a 16x16 pixel art face/bust avatar based on the user request.
            Respond ONLY with raw JSON without markdown codeblock, backticks, or extra text:
            {
              "name": "character title",
              "palette": {
                ".": "#00000000",
                "H": "#6B4435",
                "S": "#FCD5B5",
                "E": "#2B1B17",
                "W": "#FFFFFF",
                "R": "#E63946"
              },
              "grid": [
                "................",
                "....HHHHHHHH....",
                "...HHHHHHHHHH...",
                "...HHEEHHSHHH...",
                "...HHSSSSSSHH...",
                "...HHSSRRSSHH...",
                "...HHHHHHHHHH...",
                "....SSSSSSSS....",
                ".....SSSSSS.....",
                "....RRRRRRRR....",
                "...RRRRRRRRRR...",
                "...RRRRRRRRRR...",
                "...RRRRRRRRRR...",
                "...RRRRRRRRRR...",
                "................",
                "................"
              ]
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", "ag/gemini-3.8-flash")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.7)
        }

        val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "${baseUrl.trimEnd('/')}/chat/completions"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val parsedBmp = parseAndRenderPixelGrid(responseBody)
                        if (parsedBmp != null) {
                            return@withContext saveAvatarToFile(parsedBmp)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        generateFallbackPreset(userPrompt)
    }

    private fun generateFallbackPreset(prompt: String): String? {
        val isMale = prompt.contains("cowok", ignoreCase = true) ||
                prompt.contains("boy", ignoreCase = true) ||
                prompt.contains("pria", ignoreCase = true)

        val palette = if (isMale) {
            mapOf(
                '.' to Color.TRANSPARENT,
                'H' to Color.parseColor("#2B2B2B"), // Rambut hitam rapi
                'S' to Color.parseColor("#FCD5B5"), // Kulit cerah
                'E' to Color.parseColor("#1A1A1A"), // Mata
                'C' to Color.parseColor("#4A6B82"), // Jaket denim
                'W' to Color.parseColor("#FFFFFF")
            )
        } else {
            mapOf(
                '.' to Color.TRANSPARENT,
                'H' to Color.parseColor("#6B4435"), // Rambut coklat
                'S' to Color.parseColor("#FCD5B5"), // Kulit cerah
                'R' to Color.parseColor("#E63946"), // Pita merah
                'E' to Color.parseColor("#2B1B17"), // Mata
                'P' to Color.parseColor("#F5A396"), // Blush pipi
                'C' to Color.parseColor("#F4B8C5"), // Baju pastel
                'W' to Color.parseColor("#FFFFFF")
            )
        }

        val grid = if (isMale) {
            listOf(
                "................",
                "....HHHHHHHH....",
                "...HHHHHHHHHH...",
                "...HHSHSHSHHH...",
                "...HHESSSESHH...",
                "...HHSSSSSSHH...",
                "...HHSSWWSSHH...",
                "...HHHHHHHHHH...",
                "....SSSSSSSS....",
                ".....SSSSSS.....",
                "....CCCCCCCC....",
                "...CCCCCCCCCC...",
                "...CCCCCCCCCC...",
                "...CCCCCCCCCC...",
                "................",
                "................"
            )
        } else {
            listOf(
                "................",
                "....RRRRHHHH....",
                "...RRRRRHHHHH...",
                "...RHHSHSHSHH...",
                "...HHESSSESHH...",
                "...HHPSSSSPHH...",
                "...HHSSWWSSHH...",
                "...HHHHHHHHHH...",
                "....SSSSSSSS....",
                ".....SSSSSS.....",
                "....CCCCCCCC....",
                "...CCCCCCCCCC...",
                "...CCCCCCCCCC...",
                "...CCCCCCCCCC...",
                "................",
                "................"
            )
        }

        val width = 16
        val height = 16
        val scale = 16
        val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)

        for (y in 0 until minOf(height, grid.size)) {
            val row = grid[y]
            for (x in 0 until minOf(width, row.length)) {
                val char = row[x]
                val color = palette[char] ?: Color.TRANSPARENT
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        bitmap.setPixel(x * scale + dx, y * scale + dy, color)
                    }
                }
            }
        }
        return saveAvatarToFile(bitmap)
    }

    private fun parseAndRenderPixelGrid(rawResponse: String): Bitmap? {
        return try {
            val jsonText = if (rawResponse.trimStart().startsWith("data:")) {
                // Handle SSE format chunks if streaming or chunked
                var fullContent = ""
                rawResponse.lines().forEach { line ->
                    val clean = line.trim()
                    if (clean.startsWith("data:") && !clean.contains("[DONE]")) {
                        try {
                            val chunkObj = JSONObject(clean.removePrefix("data:").trim())
                            val choices = chunkObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrEmpty()) fullContent += content
                            }
                        } catch (_: Exception) {}
                    }
                }
                fullContent
            } else {
                val obj = JSONObject(rawResponse)
                val choices = obj.getJSONArray("choices")
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            }

            val cleanedJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val startIndex = cleanedJson.indexOf('{')
            val endIndex = cleanedJson.lastIndexOf('}')
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return null
            val finalJson = cleanedJson.substring(startIndex, endIndex + 1)

            val parsedObj = JSONObject(finalJson)
            val paletteObj = parsedObj.getJSONObject("palette")
            val gridArr = parsedObj.getJSONArray("grid")

            val paletteMap = mutableMapOf<Char, Int>()
            paletteObj.keys().forEach { key ->
                val hex = paletteObj.getString(key)
                val colorInt = Color.parseColor(hex)
                if (key.isNotEmpty()) {
                    paletteMap[key[0]] = colorInt
                }
            }

            val width = 16
            val height = 16
            val scale = 16 // 16x scale -> 256x256 px crisp pixel art
            val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)

            for (y in 0 until minOf(height, gridArr.length())) {
                val row = gridArr.getString(y)
                for (x in 0 until minOf(width, row.length)) {
                    val char = row[x]
                    val color = paletteMap[char] ?: Color.TRANSPARENT

                    // Gambar blok piksel skala besar (crisp nearest-neighbor)
                    for (dy in 0 until scale) {
                        for (dx in 0 until scale) {
                            bitmap.setPixel(x * scale + dx, y * scale + dy, color)
                        }
                    }
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun saveAvatarToFile(bitmap: Bitmap): String? {
        return try {
            val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
            val file = File(avatarsDir, "pixel_avatar_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
