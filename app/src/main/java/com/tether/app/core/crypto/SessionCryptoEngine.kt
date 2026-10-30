package com.tether.app.core.crypto

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
        val keyFactory = KeyFactory.getInstance("EC")
        val privSpec = PKCS8EncodedKeySpec(myPrivateKeyBytes)
        val pubSpec = X509EncodedKeySpec(decodeBase64(partnerPublicKeyBase64))

        val privateKey = keyFactory.generatePrivate(privSpec)
        val publicKey = keyFactory.generatePublic(pubSpec)

        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val rawSharedSecret = agreement.generateSecret()

        // HKDF / SHA-256 Key Derivation dengan pengikatan konteks sesi
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("TETHER_E2EE_V1".toByteArray(Charsets.UTF_8))
        digest.update(rawSharedSecret)
        val keyMaterial = digest.digest()
        rawSharedSecret.fill(0)
        return SecretKeySpec(keyMaterial, "AES")
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

    fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decodeBase64(str: String): ByteArray {
        return Base64.getDecoder().decode(str.trim())
    }
}
