package com.tether.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_profile")
data class PartnerProfileEntity(
    @PrimaryKey
    val partnerId: String,
    val partnerName: String,
    val myName: String = "Saya",
    val moodCycle: String = "Happy",
    val publicKey: String = "",
    val p2pIpAddress: String? = null,
    val isPaired: Boolean = false,
    val pairedAt: Long = 0L,
    val partnerAvatarUri: String? = null,
    val myAvatarUri: String? = null
)
