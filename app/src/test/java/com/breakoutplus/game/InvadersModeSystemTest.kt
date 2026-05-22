package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvadersModeSystemTest {
    @Test
    fun paceAdjustments_speedUpAsFleetShrinks() {
        val full = InvadersModeSystem.paceAdjustments(
            aliveCount = 12,
            totalCount = 12,
            baseSpeed = 6f,
            baseShotCooldown = 1.6f
        )
        val reduced = InvadersModeSystem.paceAdjustments(
            aliveCount = 3,
            totalCount = 12,
            baseSpeed = 6f,
            baseShotCooldown = 1.6f
        )

        assertEquals(6f, full.speed, 0.0001f)
        assertEquals(1.6f, full.shotCooldown, 0.0001f)
        assertTrue(reduced.speed > full.speed)
        assertTrue(reduced.shotCooldown < full.shotCooldown)
    }

    @Test
    fun nextFormationOffset_bouncesDirectionAtBounds() {
        val step = InvadersModeSystem.nextFormationOffset(
            currentOffset = 9.5f,
            direction = 1f,
            speed = 10f,
            dt = 0.1f,
            minOffsetAllowed = 0f,
            maxOffsetAllowed = 10f
        )

        assertTrue(step.playTurnSound)
        assertEquals(-1f, step.direction, 0.0001f)
        assertEquals(9.5f, step.offset, 0.0001f)
    }

    @Test
    fun volleyShotCount_scalesWithLevel() {
        assertEquals(2, InvadersModeSystem.volleyShotCount(0))
        assertEquals(3, InvadersModeSystem.volleyShotCount(3))
        assertEquals(4, InvadersModeSystem.volleyShotCount(9))
    }

    @Test
    fun canSpawnShot_respectsCap() {
        assertTrue(InvadersModeSystem.canSpawnShot(levelIndex = 0, currentShots = 5))
        assertFalse(InvadersModeSystem.canSpawnShot(levelIndex = 0, currentShots = 6))
        assertTrue(InvadersModeSystem.canSpawnShot(levelIndex = 8, currentShots = 9))
        assertFalse(InvadersModeSystem.canSpawnShot(levelIndex = 8, currentShots = 12))
    }
}
