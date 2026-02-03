package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test

class LevelFactoryTest {
    @Test
    fun patternsIncludeAdvancedBricks() {
        val layout = LevelFactory.buildLevel(6, 1f, false)
        val types = layout.bricks.map { it.type }.toSet()
        assertTrue(types.contains(BrickType.MOVING))
        assertTrue(types.contains(BrickType.SPAWNING))
    }

    @Test
    fun bossAndPhaseAppearInPattern() {
        val layout = LevelFactory.buildLevel(7, 1f, false)
        val types = layout.bricks.map { it.type }.toSet()
        assertTrue(types.contains(BrickType.BOSS))
        assertTrue(types.contains(BrickType.PHASE))
    }

    @Test
    fun scalingIncreasesHitPoints() {
        val easy = LevelFactory.buildLevel(0, 1f, false)
        val hard = LevelFactory.buildLevel(0, 1.6f, false)
        val easyNormal = easy.bricks.first { it.type == BrickType.NORMAL }.hitPoints
        val hardNormal = hard.bricks.first { it.type == BrickType.NORMAL }.hitPoints
        assertTrue(hardNormal >= easyNormal)
    }
}
