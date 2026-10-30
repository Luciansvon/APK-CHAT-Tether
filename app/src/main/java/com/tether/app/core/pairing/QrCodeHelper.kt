package com.tether.app.core.pairing

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
