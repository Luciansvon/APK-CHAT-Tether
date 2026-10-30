package com.tether.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tether.app.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY lamportTimestamp ASC, localTimestamp ASC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT MAX(lamportTimestamp) FROM messages")
    suspend fun getLatestLamportTimestamp(): Long?

    // Idempotensi: Jika pesan dengan ID sama sudah ada, abaikan tanpa menimpa
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET isDelivered = :delivered WHERE id = :id")
    suspend fun updateDeliveredStatus(id: String, delivered: Boolean)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
