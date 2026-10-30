package com.tether.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tether.app.core.database.entity.PartnerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerProfileDao {
    @Query("SELECT * FROM partner_profile LIMIT 1")
    fun getProfileFlow(): Flow<PartnerProfileEntity?>

    @Query("SELECT * FROM partner_profile LIMIT 1")
    suspend fun getProfile(): PartnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PartnerProfileEntity)

    @Query("UPDATE partner_profile SET moodCycle = :mood WHERE partnerId = :partnerId")
    suspend fun updateMood(partnerId: String, mood: String)

    @Query("UPDATE partner_profile SET p2pIpAddress = :ip WHERE partnerId = :partnerId")
    suspend fun updateP2pAddress(partnerId: String, ip: String?)

    @Query("UPDATE partner_profile SET partnerName = :partnerName, myName = :myName, moodCycle = :mood, partnerAvatarUri = :partnerAvatarUri, myAvatarUri = :myAvatarUri WHERE partnerId = :partnerId")
    suspend fun updateProfileData(partnerId: String, partnerName: String, myName: String, mood: String, partnerAvatarUri: String?, myAvatarUri: String?)
}
