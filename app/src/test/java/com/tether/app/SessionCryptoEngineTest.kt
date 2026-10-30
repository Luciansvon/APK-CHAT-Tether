package com.tether.app

import com.tether.app.core.crypto.SessionCryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCryptoEngineTest {

    @Test
    fun testEphemeralKeyPairGeneration() {
        val (pub, priv) = SessionCryptoEngine.generateEphemeralKeyPair()
        assertNotNull(pub)
        assertNotNull(priv)
        assert(pub.isNotEmpty())
        assert(priv.isNotEmpty())
    }

    @Test
    fun testEcdhKeyAgreementAndSharedSecretMatch() {
        // Alice & Bob key pairs
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, bobPriv) = SessionCryptoEngine.generateEphemeralKeyPair()

        val alicePubBase64 = SessionCryptoEngine.encodeBase64(alicePub)
        val bobPubBase64 = SessionCryptoEngine.encodeBase64(bobPub)

        // Alice derives key with Bob's public key
        val aliceSessionKey = SessionCryptoEngine.deriveSharedAesKey(
            myPrivateKeyBytes = alicePriv,
            partnerPublicKeyBase64 = bobPubBase64,
            myPublicKeyBytes = alicePub
        )

        // Bob derives key with Alice's public key
        val bobSessionKey = SessionCryptoEngine.deriveSharedAesKey(
            myPrivateKeyBytes = bobPriv,
            partnerPublicKeyBase64 = alicePubBase64,
            myPublicKeyBytes = bobPub
        )

        assertArrayEquals(aliceSessionKey.encoded, bobSessionKey.encoded)
    }

    @Test
    fun testAesGcmEncryptionAndDecryption() {
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, bobPriv) = SessionCryptoEngine.generateEphemeralKeyPair()

        val aliceKey = SessionCryptoEngine.deriveSharedAesKey(
            alicePriv,
            SessionCryptoEngine.encodeBase64(bobPub),
            alicePub
        )
        val bobKey = SessionCryptoEngine.deriveSharedAesKey(
            bobPriv,
            SessionCryptoEngine.encodeBase64(alicePub),
            bobPub
        )

        val secretMessage = "{\"type\":\"TEXT\",\"content\":\"Sayang, nanti pulang bareng yuk! ❤️\"}"
        val encryptedEnvelope = SessionCryptoEngine.encryptPayload(secretMessage, aliceKey)

        assertNotNull(encryptedEnvelope)
        assert(encryptedEnvelope != secretMessage)

        val decrypted = SessionCryptoEngine.decryptPayload(encryptedEnvelope, bobKey)
        assertEquals(secretMessage, decrypted)
    }

    @Test
    fun testWireEnvelopeEncryptionRoundtrip() {
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, bobPriv) = SessionCryptoEngine.generateEphemeralKeyPair()

        val aliceKey = SessionCryptoEngine.deriveSharedAesKey(
            alicePriv,
            SessionCryptoEngine.encodeBase64(bobPub),
            alicePub
        )
        val bobKey = SessionCryptoEngine.deriveSharedAesKey(
            bobPriv,
            SessionCryptoEngine.encodeBase64(alicePub),
            bobPub
        )

        val rawPacketJson = "{\"id\":\"p1\",\"type\":\"TEXT\",\"senderId\":\"ME\",\"content\":\"Rahasia P2P\",\"lamportTimestamp\":7}"
        val wireEnvelope = SessionCryptoEngine.wrapWireEnvelope(rawPacketJson, aliceKey)

        assertTrue(wireEnvelope.contains("\"enc\""))
        assertTrue(!wireEnvelope.contains("Rahasia P2P"))

        val unwrapped = SessionCryptoEngine.unwrapWireEnvelope(wireEnvelope, bobKey)
        assertEquals(rawPacketJson, unwrapped)

        // Tanpa sessionKey, amplop terenkripsi wajib ditolak (null)
        assertNull(SessionCryptoEngine.unwrapWireEnvelope(wireEnvelope, null))
    }

    @Test
    fun testDecryptionFailsOnTamperedPayload() {
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, _) = SessionCryptoEngine.generateEphemeralKeyPair()

        val aliceKey = SessionCryptoEngine.deriveSharedAesKey(
            alicePriv,
            SessionCryptoEngine.encodeBase64(bobPub),
            alicePub
        )

        val secretMessage = "Rahasia kita berdua"
        val encryptedEnvelope = SessionCryptoEngine.encryptPayload(secretMessage, aliceKey)

        // Tamper with envelope
        val raw = SessionCryptoEngine.decodeBase64(encryptedEnvelope)
        raw[raw.size - 1] = (raw[raw.size - 1].toInt() xor 0xFF).toByte()
        val tamperedEnvelope = SessionCryptoEngine.encodeBase64(raw)

        val decrypted = SessionCryptoEngine.decryptPayload(tamperedEnvelope, aliceKey)
        assertNull(decrypted)
    }

    @Test
    fun testDecryptionFailsWithWrongKey() {
        val (alicePub, alicePriv) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (bobPub, _) = SessionCryptoEngine.generateEphemeralKeyPair()
        val (evePub, evePriv) = SessionCryptoEngine.generateEphemeralKeyPair()

        val aliceKey = SessionCryptoEngine.deriveSharedAesKey(
            alicePriv,
            SessionCryptoEngine.encodeBase64(bobPub),
            alicePub
        )
        val eveKey = SessionCryptoEngine.deriveSharedAesKey(
            evePriv,
            SessionCryptoEngine.encodeBase64(evePub),
            evePub
        )

        val secretMessage = "Pesan rahasia"
        val encrypted = SessionCryptoEngine.encryptPayload(secretMessage, aliceKey)

        val decryptedByEve = SessionCryptoEngine.decryptPayload(encrypted, eveKey)
        assertNull(decryptedByEve)
    }
}
