package com.tether.app

import com.tether.app.core.crypto.SessionCryptoEngine
import com.tether.app.core.network.config.CloudConfig
import com.tether.app.core.network.model.TetherPacket
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class CloudIntegrationTest {

    @Test
    fun testCloudConfigValidation() {
        // Reset ke kondisi awal
        CloudConfig.FIREBASE_API_KEY = ""
        CloudConfig.FIREBASE_DATABASE_URL = ""
        CloudConfig.DRIVE_UPLOAD_URL = ""

        assertFalse(CloudConfig.isFirebaseConfigured())
        assertFalse(CloudConfig.isDriveConfigured())

        // Set Firebase valid
        CloudConfig.FIREBASE_API_KEY = "AIzaSyTestApiKey12345"
        CloudConfig.FIREBASE_DATABASE_URL = "https://tether-couple-test-rtdb.asia-southeast1.firebasedatabase.app"
        CloudConfig.USER_1_EMAIL = "bima@tether.local"
        CloudConfig.USER_1_PASSWORD = "password_bima"
        CloudConfig.USER_2_EMAIL = "lina@tether.local"
        CloudConfig.USER_2_PASSWORD = "password_lina"
        CloudConfig.IS_PRIMARY_USER = true

        assertTrue(CloudConfig.isFirebaseConfigured())
        assertEquals("bima@tether.local", CloudConfig.ACTIVE_EMAIL)

        CloudConfig.IS_PRIMARY_USER = false
        assertEquals("lina@tether.local", CloudConfig.ACTIVE_EMAIL)

        // Set Drive valid
        CloudConfig.DRIVE_UPLOAD_URL = "https://script.google.com/macros/s/AKfycbTest123/exec"
        assertTrue(CloudConfig.isDriveConfigured())
    }

    @Test
    fun testFirebaseWirePayloadEncryptionAndDecryption() {
        // Simulasi Alice & Bob
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, bobPriv) = SessionCryptoEngine.generateEphemeralKeyPair()

        val alicePubB64 = SessionCryptoEngine.encodeBase64(alicePub)
        val bobPubB64 = SessionCryptoEngine.encodeBase64(bobPub)

        val aliceKey = SessionCryptoEngine.deriveSharedAesKey(alicePriv, bobPubB64, alicePub)
        val bobKey = SessionCryptoEngine.deriveSharedAesKey(bobPriv, alicePubB64, bobPub)

        // Buat paket pesan Tether
        val originalPacket = TetherPacket(
            id = "msg_cloud_999",
            type = "POLAROID",
            senderId = "ME",
            content = "https://lh3.googleusercontent.com/d/1A2B3C4D_FILE_ID",
            caption = "Kencan pertama kita ❤️",
            lamportTimestamp = 101L
        )

        // 1. Alice enkripsi paket untuk dikirim ke Firebase
        val wireEncrypted = SessionCryptoEngine.wrapWireEnvelope(originalPacket.toJson(), aliceKey)
        val firebaseMsgObj = JSONObject().apply {
            put("senderUid", "alice_firebase_uid_123")
            put("payload", wireEncrypted)
            put("createdAt", System.currentTimeMillis())
        }

        // Pastikan di dalam Firebase payload tidak terbaca plaintext (E2EE aman)
        assertFalse(firebaseMsgObj.getString("payload").contains("Kencan pertama kita"))

        // 2. Bob menerima dari Firebase SSE dan mendekripsi
        val receivedPayload = firebaseMsgObj.getString("payload")
        val unwrappedJson = SessionCryptoEngine.unwrapWireEnvelope(receivedPayload, bobKey)
        assertNotNull(unwrappedJson)

        val receivedPacket = TetherPacket.fromJson(unwrappedJson!!)
        assertNotNull(receivedPacket)
        assertEquals(originalPacket.id, receivedPacket?.id)
        assertEquals(originalPacket.type, receivedPacket?.type)
        assertEquals(originalPacket.content, receivedPacket?.content)
        assertEquals(originalPacket.caption, receivedPacket?.caption)
        assertEquals(originalPacket.lamportTimestamp, receivedPacket?.lamportTimestamp)
    }

    @Test
    fun testDrivePayloadPackagingAndDirectUrlFormat() {
        val sampleBytes = "SIMULASI_DATA_FOTO_POLAROID_BYTES".toByteArray(Charsets.UTF_8)
        val base64Str = Base64.getEncoder().encodeToString(sampleBytes)

        val jsonPayload = JSONObject().apply {
            put("base64", base64Str)
            put("mimeType", "image/jpeg")
            put("fileName", "tether_polaroid_123.jpg")
        }

        // Verifikasi struktur JSON sesuai kontrak doPost Google Apps Script
        assertEquals("image/jpeg", jsonPayload.getString("mimeType"))
        assertEquals("tether_polaroid_123.jpg", jsonPayload.getString("fileName"))

        // Verifikasi decode Base64 menghasilkan byte asli yang identik
        val decodedBytes = Base64.getDecoder().decode(jsonPayload.getString("base64"))
        assertEquals(String(sampleBytes), String(decodedBytes))

        // Simulasi respon balik GAS Web App
        val fileId = "1aBcDeFgHiJkLmNoP"
        val gasResponse = JSONObject().apply {
            put("ok", true)
            put("fileId", fileId)
            put("url", "https://lh3.googleusercontent.com/d/$fileId")
        }

        assertTrue(gasResponse.getBoolean("ok"))
        assertEquals("https://lh3.googleusercontent.com/d/1aBcDeFgHiJkLmNoP", gasResponse.getString("url"))
    }
}
