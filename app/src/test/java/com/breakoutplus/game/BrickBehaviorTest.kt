package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test

class BrickBehaviorTest {
    @Test
    fun phaseBrick_keepsPositiveHpBetweenPhaseTransitions() {
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 0f,
            y = 0f,
            width = 1f,
            height = 1f,
            hitPoints = 2,
            maxHitPoints = 2,
            type = BrickType.PHASE,
            alive = true
        )
        brick.maxPhase = 3
        brick.phase = 0

        // Hit 1: regular damage.
        brick.applyHit(forceBreak = false)
        // Hit 2: transition into phase 1 with refreshed HP.
        brick.applyHit(forceBreak = false)
        assertTrue("Phase transition HP should stay positive", brick.hitPoints >= 1)
        assertTrue("Brick should remain alive before final phase", brick.alive)

        // Hit 3: transition into phase 2 with refreshed HP.
        brick.applyHit(forceBreak = false)
        assertTrue("Phase transition HP should stay positive", brick.hitPoints >= 1)
        assertTrue("Brick should remain alive before final phase", brick.alive)
    }
}
