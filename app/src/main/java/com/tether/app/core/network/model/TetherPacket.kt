package com.tether.app.core.network.model

import org.json.JSONObject

data class TetherPacket(
    val id: String,
    val type: String, // "TEXT", "POLAROID", "WHISPER", "COUPON_CLAIM", "HAPTIC_TOUCH", "HEARTBEAT", "TUG", "MOOD_UPDATE", "SYSTEM"
    val senderId: String,
    val content: String,
    val caption: String? = null,
    val duration: String? = null,
    val lamportTimestamp: Long,
    val touchX: Float? = null,
    val touchY: Float? = null
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("type", type)
            put("senderId", senderId)
            put("content", content)
            if (caption != null) put("caption", caption)
            if (duration != null) put("duration", duration)
            put("lamportTimestamp", lamportTimestamp)
            if (touchX != null) put("touchX", touchX.toDouble())
            if (touchY != null) put("touchY", touchY.toDouble())
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): TetherPacket? {
            return try {
                val obj = JSONObject(jsonStr)
                TetherPacket(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    senderId = obj.getString("senderId"),
                    content = obj.getString("content"),
                    caption = if (obj.has("caption") && !obj.isNull("caption")) obj.getString("caption") else null,
                    duration = if (obj.has("duration") && !obj.isNull("duration")) obj.getString("duration") else null,
                    lamportTimestamp = obj.getLong("lamportTimestamp"),
                    touchX = if (obj.has("touchX") && !obj.isNull("touchX")) obj.getDouble("touchX").toFloat() else null,
                    touchY = if (obj.has("touchY") && !obj.isNull("touchY")) obj.getDouble("touchY").toFloat() else null
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
