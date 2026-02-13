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
        val occupancy = l.bricks.size.toFloat() / (l.rows * l.cols).toFloat()
        assertTrue("Procedural levels should keep dense gameplay lanes", occupancy >= 0.56f)

        l.bricks.forEach { b ->
            assertTrue(b.row in 0 until l.rows)
            assertTrue(b.col in 0 until l.cols)
            assertTrue(b.hitPoints >= 1)
        }
    }

    @Test
    fun buildLevel_patternFillKeepsDenseBoards() {
        val l = LevelFactory.buildLevel(index = 0, difficulty = 1.2f, endless = false)
        val occupancy = l.bricks.size.toFloat() / (l.rows * l.cols).toFloat()
        assertTrue("Pattern levels should avoid sparse boards", occupancy >= 0.72f)
    }

    @Test
    fun buildLevel_midgamePatternFillKeepsPressure() {
        val l = LevelFactory.buildLevel(index = 7, difficulty = 1.35f, endless = false)
        val occupancy = l.bricks.size.toFloat() / (l.rows * l.cols).toFloat()
        assertTrue("Midgame boards should maintain pressure density", occupancy >= 0.62f)
    }

    @Test
    fun buildLevel_respectsForcedTheme() {
        val level = LevelFactory.buildLevel(
            index = 5,
            difficulty = 1.3f,
            endless = false,
            forcedTheme = LevelThemes.AURORA
        )
        assertEquals("Aurora", level.theme.name)
    }

    @Test
    fun buildTunnelLevel_keepsBottomGateOpen() {
        val level = LevelFactory.buildTunnelLevel(
            index = 5,
            difficulty = 1.35f,
            theme = LevelThemes.COBALT
        )
        val fortressBottomRow = level.rows - 4
        val occupied = level.bricks
            .filter { it.row == fortressBottomRow }
            .map { it.col }
            .toSet()
        val gaps = (0 until level.cols).count { it !in occupied }
        assertTrue("Tunnel gate row should keep an open lane", gaps >= 1)
    }

    @Test
    fun buildTunnelLevel_lateGameAddsSiegePressure() {
        val level = LevelFactory.buildTunnelLevel(
            index = 14,
            difficulty = 1.75f,
            theme = LevelThemes.COBALT
        )
        val breakables = level.bricks.filter { it.type != BrickType.UNBREAKABLE }
        assertTrue("Tunnel should remain dense in late game", breakables.size >= 50)
        assertTrue(
            "Late tunnel should include advanced interior threats",
            breakables.any { it.type == BrickType.MOVING || it.type == BrickType.PHASE }
        )
    }
}
