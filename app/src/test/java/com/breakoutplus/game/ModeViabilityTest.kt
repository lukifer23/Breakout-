package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test

class ModeViabilityTest {
    @Test
    fun rushTimerAndSpeedStayWithinPlayableWindow() {
        assertTrue("Rush timer should stay at or above 50 seconds", GameMode.RUSH.timeLimitSeconds >= 50)
        assertTrue("Rush launch speed should remain controlled", GameMode.RUSH.launchSpeed in 80f..100f)
    }

    @Test
    fun volleyAndClassicMaintainReadableControlSpeeds() {
        assertTrue("Volley launch speed should remain below rush for readability", GameMode.VOLLEY.launchSpeed < GameMode.RUSH.launchSpeed)
        assertTrue("Classic should stay slower than timed", GameMode.CLASSIC.launchSpeed < GameMode.TIMED.launchSpeed)
        assertTrue("Volley should stay above god mode to avoid sluggish turns", GameMode.VOLLEY.launchSpeed > GameMode.GOD.launchSpeed)
    }

    @Test
    fun survivalRemainsFastestMainlineMode() {
        assertTrue("Survival should be at least as fast as rush at launch", GameMode.SURVIVAL.launchSpeed >= GameMode.RUSH.launchSpeed)
    }
}
