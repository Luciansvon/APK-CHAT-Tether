package com.tether.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
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

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPass = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivString = prefs.getString(KEY_IV, null)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey

        if (encryptedPass != null && ivString != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(Base64.decode(encryptedPass, Base64.NO_WRAP))
            return decrypted
        } else {
            // Buat 32-byte (256-bit) passphrase acak
            val rawPass = ByteArray(32).apply {
                java.security.SecureRandom().nextBytes(this)
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(rawPass)

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(cipherText, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            return rawPass
        }
    }
}
