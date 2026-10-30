package com.tether.app.core.designsystem.util

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object BitmapLoader {
    // Cache di RAM berbasis ukuran nyata (1/8 heap, maks 32 MB) untuk mencegah disk/network I/O berulang
    private val memoryCache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(32L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
    }

    fun decodeSampledAvatar(path: String?, reqSizePx: Int = 128): ImageBitmap? {
        return decodeSampledBitmap(path, reqSizePx)
    }

    fun decodeSampledBitmap(source: String?, reqSizePx: Int = 512): ImageBitmap? {
        if (source.isNullOrBlank()) return null
        val cacheKey = "$source-$reqSizePx"
        memoryCache.get(cacheKey)?.let { return it }

        return try {
            val bmp = when {
                source.startsWith("data:image/") -> {
                    val b64 = source.substringAfter("base64,")
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    options.inSampleSize = calculateInSampleSize(options, reqSizePx, reqSizePx)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
                source.startsWith("http://") || source.startsWith("https://") -> {
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val url = URL(source)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        doInput = true
                    }
                    conn.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }
                else -> {
                    val file = File(source)
                    if (!file.exists()) return null
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(source, options)
                    options.inSampleSize = calculateInSampleSize(options, reqSizePx, reqSizePx)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeFile(source, options)
                }
            }

            val imgBitmap = bmp?.asImageBitmap()
            if (imgBitmap != null) {
                memoryCache.put(cacheKey, imgBitmap)
            }
            imgBitmap
        } catch (_: Exception) {
            null
        }
    }

    fun clearCache() {
        memoryCache.evictAll()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
