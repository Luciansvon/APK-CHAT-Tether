package com.tether.app.core.pairing

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PairingPayload(
    val partnerId: String,
    val partnerName: String,
    val publicKey: String,
    val timestamp: Long
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("id", partnerId)
            put("name", partnerName)
            put("key", publicKey)
            put("ts", timestamp)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): PairingPayload? {
            return try {
                val obj = JSONObject(jsonStr)
                PairingPayload(
                    partnerId = obj.getString("id"),
                    partnerName = obj.getString("name"),
                    publicKey = obj.getString("key"),
                    timestamp = obj.getLong("ts")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object QrCodeHelper {
    suspend fun generateQrBitmap(content: String, size: Int = 512): Bitmap = withContext(Dispatchers.Default) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
