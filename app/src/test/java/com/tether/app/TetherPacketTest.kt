package com.tether.app

import com.tether.app.core.network.model.TetherPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TetherPacketTest {

    @Test
    fun testPacketSerializationAndDeserialization() {
        val original = TetherPacket(
            id = "test_uuid_123",
            type = "POLAROID",
            senderId = "ME",
            content = "cafe_image_data",
            caption = "kopi hangat berdua ☕",
            duration = null,
            lamportTimestamp = 42L,
            touchX = 120.5f,
            touchY = 340.8f
        )

        val json = original.toJson()
        val parsed = TetherPacket.fromJson(json)

        assertNotNull(parsed)
        assertEquals(original.id, parsed?.id)
        assertEquals(original.type, parsed?.type)
        assertEquals(original.senderId, parsed?.senderId)
        assertEquals(original.content, parsed?.content)
        assertEquals(original.caption, parsed?.caption)
        assertEquals(original.lamportTimestamp, parsed?.lamportTimestamp)
        assertEquals(original.touchX, parsed?.touchX)
        assertEquals(original.touchY, parsed?.touchY)
    }

    @Test
    fun testWhisperPacketSerialization() {
        val packet = TetherPacket(
            id = "whisper_456",
            type = "WHISPER",
            senderId = "PARTNER",
            content = "audio_aac_stream",
            duration = "0:14",
            lamportTimestamp = 100L
        )

        val json = packet.toJson()
        val parsed = TetherPacket.fromJson(json)

        assertNotNull(parsed)
        assertEquals("0:14", parsed?.duration)
        assertEquals("WHISPER", parsed?.type)
    }
}
