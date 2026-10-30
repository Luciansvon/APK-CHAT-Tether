package com.tether.app

import com.tether.app.core.database.entity.CoupleLevelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupleLevelEntityTest {

    @Test
    fun testLevelProgressionAndProgressRatio() {
        val level1 = CoupleLevelEntity(
            currentXp = 50L,
            currentLevel = 1
        )
        assertEquals("Benang Baru", level1.title())
        assertEquals(0.5f, level1.levelProgressRatio(), 0.01f)

        val level2 = CoupleLevelEntity(
            currentXp = 175L,
            currentLevel = 2
        )
        assertEquals("Saling Sapa", level2.title())
        // base = 100, target = 250 -> range = 150. progress = 75 -> 0.5f
        assertEquals(0.5f, level2.levelProgressRatio(), 0.01f)

        val level5 = CoupleLevelEntity(
            currentXp = 1300L,
            currentLevel = 5
        )
        assertEquals("Ikatan Abadi", level5.title())
        assertTrue(level5.levelProgressRatio() >= 1.0f)
    }
}
