package com.tether.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["lamportTimestamp"]),
        Index(value = ["localTimestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String, // "ME" atau "PARTNER"
    val type: String,     // "TEXT", "POLAROID", "WHISPER", "COUPON", "SYSTEM"
    val content: String,
    val caption: String? = null,
    val duration: String? = null, // Untuk whisper voice note
    val lamportTimestamp: Long,   // Lamport Clock untuk mengurutkan pesan lintas jaringan
    val localTimestamp: Long,
    val isDelivered: Boolean = false,
    val isRead: Boolean = true,
    val isFoggy: Boolean = false,
    val rotation: Float = 0.0f
)
