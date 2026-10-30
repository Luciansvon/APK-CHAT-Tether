package com.tether.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "couple_level")
data class CoupleLevelEntity(
    @PrimaryKey val id: Int = 1,
    val currentXp: Long = 0L,
    val currentLevel: Int = 1,
    val totalMessagesSent: Int = 0,
    val totalTugsSent: Int = 0,
    val totalCouponsClaimed: Int = 0,
    val lastInteractionTimestamp: Long = System.currentTimeMillis()
) {
    fun title(): String = when (currentLevel) {
        1 -> "Benang Baru"
        2 -> "Saling Sapa"
        3 -> "Dekapan Hangat"
        4 -> "Sahabat Sejati"
        5 -> "Ikatan Abadi"
        else -> "Soulmate Bintang Lv.$currentLevel"
    }

    fun currentLevelBaseXp(): Long = xpForPreviousLevel(currentLevel)

    fun nextLevelTargetXp(): Long = xpRequiredForLevel(currentLevel)

    fun levelProgressRatio(): Float {
        val base = currentLevelBaseXp()
        val target = nextLevelTargetXp()
        val range = (target - base).coerceAtLeast(1L)
        val progress = (currentXp - base).coerceAtLeast(0L)
        return (progress.toFloat() / range.toFloat()).coerceIn(0f, 1f)
    }

    companion object {
        fun xpRequiredForLevel(level: Int): Long {
            return when {
                level <= 1 -> 100L
                level == 2 -> 250L
                level == 3 -> 500L
                level == 4 -> 850L
                level == 5 -> 1300L
                else -> 1300L + (level - 5) * 600L
            }
        }

        fun xpForPreviousLevel(level: Int): Long {
            return if (level <= 1) 0L else xpRequiredForLevel(level - 1)
        }
    }
}
