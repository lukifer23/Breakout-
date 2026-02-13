package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeTest {
    @Test
    fun launchSpeedsArePositiveAndBalanced() {
        val speeds = GameMode.values().map { it.launchSpeed }
        assertTrue(speeds.all { it > 0f })
        assertTrue(GameMode.RUSH.launchSpeed > GameMode.GOD.launchSpeed)
    }

    @Test
    fun rushHasPlayableTimerWindow() {
        assertTrue(GameMode.RUSH.timeLimitSeconds >= 50)
    }

    @Test
    fun volleyModeConfiguredAsTurnBasedSingleLife() {
        assertTrue(GameMode.VOLLEY.baseLives == 1)
        assertTrue(GameMode.VOLLEY.timeLimitSeconds == 0)
        assertTrue(GameMode.VOLLEY.launchSpeed > GameMode.GOD.launchSpeed)
    }

    @Test
    fun godMode_isEndlessPractice() {
        assertTrue(GameMode.GOD.godMode)
        assertTrue(GameMode.GOD.endless)
        assertTrue(GameMode.GOD.baseLives >= 99)
    }

    @Test
    fun modeCatalogRemainsComplete() {
        val modes = GameMode.values().toSet()
        assertTrue(modes.size == 10)
        assertTrue(GameMode.TUNNEL in modes)
        assertTrue(GameMode.ZEN in modes)
    }
}
