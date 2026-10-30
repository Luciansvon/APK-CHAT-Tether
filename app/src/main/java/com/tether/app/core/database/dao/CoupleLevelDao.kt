package com.tether.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tether.app.core.database.entity.CoupleLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoupleLevelDao {
    @Query("SELECT * FROM couple_level WHERE id = 1 LIMIT 1")
    fun getLevelFlow(): Flow<CoupleLevelEntity?>

    @Query("SELECT * FROM couple_level WHERE id = 1 LIMIT 1")
    suspend fun getLevel(): CoupleLevelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLevel(level: CoupleLevelEntity)
}
