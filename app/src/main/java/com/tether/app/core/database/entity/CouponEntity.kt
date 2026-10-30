package com.tether.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val isClaimed: Boolean = false,
    val claimedAt: Long? = null,
    val rotation: Float = -1.2f
)
