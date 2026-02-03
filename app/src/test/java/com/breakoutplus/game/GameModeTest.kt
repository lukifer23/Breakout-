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
}
