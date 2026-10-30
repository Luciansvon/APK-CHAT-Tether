package com.tether.app.core.network.drive

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.tether.app.core.network.config.CloudConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DriveUploadManager {
    private const val TAG = "DriveUpload"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadBytes(
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
        fileName: String = "tether_${System.currentTimeMillis()}.jpg"
    ): String? = withContext(Dispatchers.IO) {
        val driveUrl = CloudConfig.DRIVE_UPLOAD_URL
        if (driveUrl.isBlank() || driveUrl.contains("XXXX")) {
            Log.w(TAG, "DRIVE_UPLOAD_URL belum dikonfigurasi di CloudConfig")
            return@withContext null
        }

        try {
            val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val jsonPayload = JSONObject().apply {
                put("base64", base64Str)
                put("mimeType", mimeType)
                put("fileName", fileName)
            }

            val body = jsonPayload.toString().toRequestBody("text/plain;charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(driveUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: return@withContext null
                val resJson = JSONObject(respStr)
                if (resJson.optBoolean("ok")) {
                    resJson.getString("url")
                } else {
                    Log.e(TAG, "Gagal upload Drive: ${resJson.optString("error")}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat upload ke Drive: ${e.message}")
            null
        }
    }

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        mimeType: String = "image/jpeg",
        fileName: String = "tether_${System.currentTimeMillis()}.jpg"
    ): String? = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca uri: ${e.message}")
            null
        } ?: return@withContext null

        uploadBytes(bytes, mimeType, fileName)
    }
}
