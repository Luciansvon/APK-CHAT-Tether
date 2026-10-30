package com.tether.app.core.crypto

import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SessionCryptoEngine {
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val ENVELOPE_VERSION = 1

    // Enkripsi/dekripsi diserialkan agar lonjakan memori Cipher tidak bertumpuk saat foto besar masuk bersamaan.
    private val cryptoDispatcher = Dispatchers.IO.limitedParallelism(1)

    fun generateEphemeralKeyPair(): Pair<ByteArray, ByteArray> {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val kp = kpg.generateKeyPair()
        return Pair(kp.public.encoded, kp.private.encoded)
    }

    fun deriveSharedAesKey(
        myPrivateKeyBytes: ByteArray,
        partnerPublicKeyBase64: String,
        myPublicKeyBytes: ByteArray
    ): SecretKey {
        val partnerPublicKeyBytes = decodeBase64(partnerPublicKeyBase64)
        val keyFactory = KeyFactory.getInstance("EC")
        val privSpec = PKCS8EncodedKeySpec(myPrivateKeyBytes)
        val pubSpec = X509EncodedKeySpec(partnerPublicKeyBytes)

        val privateKey = keyFactory.generatePrivate(privSpec)
        val publicKey = keyFactory.generatePublic(pubSpec)

        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val rawSharedSecret = agreement.generateSecret()

        // HKDF / SHA-256 Key Derivation dengan pengikatan deterministik kedua kunci publik (anti-UKS)
        val myPubB64 = encodeBase64(myPublicKeyBytes)
        val partnerPubB64 = encodeBase64(partnerPublicKeyBytes)
        val sortedPubKeys = if (myPubB64 <= partnerPubB64) {
            listOf(myPublicKeyBytes, partnerPublicKeyBytes)
        } else {
            listOf(partnerPublicKeyBytes, myPublicKeyBytes)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("TETHER_E2EE_V1".toByteArray(Charsets.UTF_8))
        digest.update(rawSharedSecret)
        sortedPubKeys.forEach { digest.update(it) }
        val keyMaterial = digest.digest()

        rawSharedSecret.fill(0)
        return SecretKeySpec(keyMaterial, "AES").also {
            keyMaterial.fill(0)
        }
    }

    fun encryptPayload(plainJson: String, sessionKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return encodeBase64(combined)
    }

    fun decryptPayload(encodedEnvelope: String, sessionKey: SecretKey): String? {
        return try {
            val combined = decodeBase64(encodedEnvelope)
            if (combined.size <= GCM_IV_BYTES) return null
            val iv = combined.copyOfRange(0, GCM_IV_BYTES)
            val cipherText = combined.copyOfRange(GCM_IV_BYTES, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun deriveRoomAesKey(roomCode: String, passcode: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("TETHER_ROOM_E2EE_V1".toByteArray(Charsets.UTF_8))
        digest.update(roomCode.trim().lowercase().toByteArray(Charsets.UTF_8))
        digest.update(passcode.trim().toByteArray(Charsets.UTF_8))
        val keyMaterial = digest.digest()
        return SecretKeySpec(keyMaterial, "AES")
    }

    /**
     * Membungkus payload JSON ke dalam amplop E2EE AES-256-GCM saat sessionKey aktif.
     */
    fun wrapWireEnvelope(plainJson: String, sessionKey: SecretKey?): String {
        if (sessionKey == null) return plainJson
        val ciphertextB64 = encryptPayload(plainJson, sessionKey)
        return JSONObject().apply {
            put("v", ENVELOPE_VERSION)
            put("enc", ciphertextB64)
        }.toString()
    }

    /**
     * Membuka amplop E2EE dari jaringan P2P/Relay.
     * Mendukung amplop terenkripsi ("enc") maupun fallback paket plaintext saat belum memiliki shared key.
     */
    fun unwrapWireEnvelope(wireLine: String, sessionKey: SecretKey?): String? {
        return try {
            val obj = JSONObject(wireLine)
            if (obj.has("enc") && !obj.isNull("enc")) {
                val key = sessionKey ?: return null
                decryptPayload(obj.getString("enc"), key)
            } else {
                wireLine
            }
        } catch (_: Exception) {
            null
        }
    }

    fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    suspend fun wrapWireEnvelopeSerialized(plainJson: String, sessionKey: SecretKey?): String =
        withContext(cryptoDispatcher) { wrapWireEnvelope(plainJson, sessionKey) }

    suspend fun unwrapWireEnvelopeSerialized(wireLine: String, sessionKey: SecretKey?): String? =
        withContext(cryptoDispatcher) { unwrapWireEnvelope(wireLine, sessionKey) }

    fun decodeBase64(str: String): ByteArray {
        return Base64.getDecoder().decode(str.trim())
    }
}
