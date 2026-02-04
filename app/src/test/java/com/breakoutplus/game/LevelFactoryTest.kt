package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelFactoryTest {
    @Test
    fun buildLevel_cyclesThemesDeterministically() {
        val l0 = LevelFactory.buildLevel(index = 0, difficulty = 1.0f, endless = false)
        val l1 = LevelFactory.buildLevel(index = 1, difficulty = 1.0f, endless = false)
        val l2 = LevelFactory.buildLevel(index = 2, difficulty = 1.0f, endless = false)

        assertEquals("Neon", l0.theme.name)
        assertEquals("Sunset", l1.theme.name)
        assertEquals("Cobalt", l2.theme.name)
    }

    @Test
    fun buildLevel_scalesHitPointsButKeepsUnbreakableConstant() {
        val l = LevelFactory.buildLevel(index = 0, difficulty = 2.0f, endless = false)
        assertTrue(l.bricks.isNotEmpty())

        val unbreakables = l.bricks.filter { it.type == BrickType.UNBREAKABLE }
        unbreakables.forEach { assertEquals(999, it.hitPoints) }

        val reinforced = l.bricks.firstOrNull { it.type == BrickType.REINFORCED }
        if (reinforced != null) {
            assertTrue("Expected scaled hit points for reinforced brick", reinforced.hitPoints >= 3)
        }
    }

    @Test
    fun buildLevel_endlessProceduralLevelHasBoundsAndBricks() {
        val l = LevelFactory.buildLevel(index = 50, difficulty = 1.7f, endless = true)
        assertTrue(l.rows in 6..12)
        assertTrue(l.cols in 10..15)
        assertTrue("Procedural levels should spawn some bricks", l.bricks.isNotEmpty())

        l.bricks.forEach { b ->
            assertTrue(b.row in 0 until l.rows)
            assertTrue(b.col in 0 until l.cols)
            assertTrue(b.hitPoints >= 1)
        }
    }
}

