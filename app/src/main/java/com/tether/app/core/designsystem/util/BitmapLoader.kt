package com.tether.app.core.designsystem.util

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object BitmapLoader {
    // Cache di RAM (maksimum 24 avatar bitmap terkini) untuk mencegah disk I/O berulang pada eMMC
    private val memoryCache = LruCache<String, ImageBitmap>(24)

    fun decodeSampledAvatar(path: String?, reqSizePx: Int = 128): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        val cacheKey = "$path-$reqSizePx"
        memoryCache.get(cacheKey)?.let { return it }

        val file = File(path)
        if (!file.exists()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options, reqSizePx, reqSizePx)
            options.inJustDecodeBounds = false
            val bmp = BitmapFactory.decodeFile(path, options)
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
