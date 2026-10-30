package com.tether.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tether.app.core.database.entity.CouponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY id ASC")
    fun getAllCouponsFlow(): Flow<List<CouponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<CouponEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Query("UPDATE coupons SET isClaimed = 1, claimedAt = :timestamp WHERE id = :id")
    suspend fun claimCoupon(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM coupons")
    suspend fun getCouponsCount(): Int
}
