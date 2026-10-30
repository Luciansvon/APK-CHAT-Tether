package com.tether.app

import com.tether.app.core.pairing.PairingPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PairingPayloadTest {

    @Test
    fun testPairingPayloadRoundtrip() {
        val payload = PairingPayload(
            partnerId = "user_892341",
            partnerName = "Bima",
            publicKey = "ed25519_pub_key_secure_xyz",
            timestamp = 1793320000000L
        )

        val json = payload.toJson()
        val parsed = PairingPayload.fromJson(json)

        assertNotNull(parsed)
        assertEquals(payload.partnerId, parsed?.partnerId)
        assertEquals(payload.partnerName, parsed?.partnerName)
        assertEquals(payload.publicKey, parsed?.publicKey)
        assertEquals(payload.timestamp, parsed?.timestamp)
    }
}
