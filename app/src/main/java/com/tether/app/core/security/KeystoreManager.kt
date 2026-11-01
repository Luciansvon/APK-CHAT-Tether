package com.tether.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.tether.app.core.crypto.SessionCryptoEngine
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "tether_db_master_key"
    private const val PREFS_NAME = "tether_secure_meta"
    private const val KEY_ENCRYPTED_PASSPHRASE = "enc_db_pass"
    private const val KEY_IV = "iv_db_pass"
    private const val KEY_DEVICE_ID = "device_partner_id"
    private const val KEY_IDENTITY_PUB = "identity_ec_pub"
    private const val KEY_ENC_IDENTITY_PRIV = "enc_identity_ec_priv"
    private const val KEY_IV_IDENTITY_PRIV = "iv_identity_ec_priv"
    private const val KEY_ROOM_CODE = "room_code"
    private const val KEY_ROOM_PASSCODE = "room_passcode"
    private const val KEY_ENC_ROOM_PASSCODE = "enc_room_passcode"
    private const val KEY_IV_ROOM_PASSCODE = "iv_room_passcode"

    fun saveRoomConfig(context: Context, roomCode: String, passcode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val secretKey = getOrCreateMasterSecretKey(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encPass = cipher.doFinal(passcode.trim().toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_ROOM_CODE, roomCode.trim().lowercase())
            .putString(KEY_ENC_ROOM_PASSCODE, Base64.encodeToString(encPass, Base64.NO_WRAP))
            .putString(KEY_IV_ROOM_PASSCODE, Base64.encodeToString(iv, Base64.NO_WRAP))
            .remove(KEY_ROOM_PASSCODE)
            .commit()
    }

    fun getRoomCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROOM_CODE, null)
    }

    fun getRoomPasscode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encPass = prefs.getString(KEY_ENC_ROOM_PASSCODE, null)
        val ivString = prefs.getString(KEY_IV_ROOM_PASSCODE, null)
        if (encPass != null && ivString != null) {
            try {
                val secretKey = getOrCreateMasterSecretKey(context)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivString, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                val decrypted = cipher.doFinal(Base64.decode(encPass, Base64.NO_WRAP))
                return String(decrypted, Charsets.UTF_8)
            } catch (_: Exception) {}
        }
        val legacy = prefs.getString(KEY_ROOM_PASSCODE, null)
        if (!legacy.isNullOrBlank()) {
            val code = prefs.getString(KEY_ROOM_CODE, null) ?: ""
            saveRoomConfig(context, code, legacy)
            return legacy
        }
        return null
    }

    fun isRoomConfigured(context: Context): Boolean {
        return !getRoomCode(context).isNullOrBlank() && !getRoomPasscode(context).isNullOrBlank()
    }

    fun clearRoomConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ROOM_CODE)
            .remove(KEY_ROOM_PASSCODE)
            .remove(KEY_ENC_ROOM_PASSCODE)
            .remove(KEY_IV_ROOM_PASSCODE)
            .commit()
    }

    private fun getOrCreateMasterSecretKey(context: Context): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val specBuilder = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                if (context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                    try {
                        specBuilder.setIsStrongBoxBacked(true)
                    } catch (_: Exception) {}
                }
            }

            try {
                keyGenerator.init(specBuilder.build())
                keyGenerator.generateKey()
            } catch (_: Exception) {
                // Fallback ke TEE standar jika StrongBox gagal saat runtime
                val fallbackBuilder = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                keyGenerator.init(fallbackBuilder.build())
                keyGenerator.generateKey()
            }
        }
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPass = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivString = prefs.getString(KEY_IV, null)
        val secretKey = getOrCreateMasterSecretKey(context)

        if (encryptedPass != null && ivString != null) {
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivString, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                cipher.doFinal(Base64.decode(encryptedPass, Base64.NO_WRAP))
            } catch (e: Exception) {
                // Jangan menimpa passphrase yang sudah ada karena akan merusak database SQLCipher lama
                throw IllegalStateException("Gagal mendekripsi passphrase database SQLCipher: ${e.message}", e)
            }
        } else {
            return generateAndStorePassphrase(prefs, secretKey)
        }
    }

    private fun generateAndStorePassphrase(
        prefs: android.content.SharedPreferences,
        secretKey: SecretKey
    ): ByteArray {
        val rawPass = ByteArray(32).apply {
            SecureRandom().nextBytes(this)
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(rawPass)

        prefs.edit()
            .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(cipherText, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .commit()

        return rawPass
    }

    @Synchronized
    fun getOrCreateDevicePartnerId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "tether_" + UUID.randomUUID().toString().replace("-", "").take(12)
        prefs.edit().putString(KEY_DEVICE_ID, generated).commit()
        return generated
    }

    /**
     * Menghasilkan atau memuat pasangan kunci ECDH (secp256r1) perangkat yang dienkripsi dengan Android Keystore.
     * Mengembalikan Pair(publicKeyBase64, privateKeyBytes).
     */
    @Synchronized
    fun getOrCreateIdentityKeyPair(context: Context): Pair<String, ByteArray> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pubB64 = prefs.getString(KEY_IDENTITY_PUB, null)
        val encPrivB64 = prefs.getString(KEY_ENC_IDENTITY_PRIV, null)
        val ivPrivB64 = prefs.getString(KEY_IV_IDENTITY_PRIV, null)
        val secretKey = getOrCreateMasterSecretKey(context)

        if (pubB64 != null && encPrivB64 != null && ivPrivB64 != null) {
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivPrivB64, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                val privBytes = cipher.doFinal(Base64.decode(encPrivB64, Base64.NO_WRAP))
                return Pair(pubB64, privBytes)
            } catch (_: Exception) {
                // Regenerate jika korup atau kunci Keystore direset
            }
        }

        val (pubBytes, privBytes) = SessionCryptoEngine.generateEphemeralKeyPair()
        val newPubB64 = SessionCryptoEngine.encodeBase64(pubBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encPriv = cipher.doFinal(privBytes)

        prefs.edit()
            .putString(KEY_IDENTITY_PUB, newPubB64)
            .putString(KEY_ENC_IDENTITY_PRIV, Base64.encodeToString(encPriv, Base64.NO_WRAP))
            .putString(KEY_IV_IDENTITY_PRIV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .commit()

        return Pair(newPubB64, privBytes)
    }
}
