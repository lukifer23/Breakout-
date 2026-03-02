package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunnelModeSystemTest {
    @Test
    fun breakthroughPacingUsesPressureAndStallBoost() {
        val pacing = TunnelModeSystem.breakthroughPacing(
            gatePressure = 0.7f,
            tunnelShotsFired = 12,
            gateBrickHit = false
        )

        assertEquals(0.049f, pacing.dynamicBoost, 0.0001f)
        assertTrue(pacing.preferBreakthroughHint)
    }

    @Test
    fun breakthroughPacingAddsGateBrickBonus() {
        val pacing = TunnelModeSystem.breakthroughPacing(
            gatePressure = 0.22f,
            tunnelShotsFired = 6,
            gateBrickHit = true
        )

        assertEquals(0.0832f, pacing.dynamicBoost, 0.0001f)
        assertTrue(pacing.preferBreakthroughHint)
    }

    @Test
    fun supplyDropGateRespectsThresholdBands() {
        val high = TunnelModeSystem.supplyDropGate(0.8f, 75, hasBreakthroughActive = false, hasBreakthroughDropQueued = false)
        val mid = TunnelModeSystem.supplyDropGate(0.6f, 75, hasBreakthroughActive = false, hasBreakthroughDropQueued = false)
        val low = TunnelModeSystem.supplyDropGate(0.59f, 75, hasBreakthroughActive = false, hasBreakthroughDropQueued = false)

        assertEquals(5, high.requiredShots)
        assertEquals(7, mid.requiredShots)
        assertEquals(9, low.requiredShots)
    }

    @Test
    fun supplyDropGateAppliesBonusesPenaltiesAndClamp() {
        val open = TunnelModeSystem.supplyDropGate(
            gatePressure = 0.75f,
            gateIntegrityPercent = 80,
            hasBreakthroughActive = false,
            hasBreakthroughDropQueued = false
        )
        val penalized = TunnelModeSystem.supplyDropGate(
            gatePressure = 0.75f,
            gateIntegrityPercent = 80,
            hasBreakthroughActive = true,
            hasBreakthroughDropQueued = true
        )
        val clampedMin = TunnelModeSystem.supplyDropGate(
            gatePressure = 0f,
            gateIntegrityPercent = 0,
            hasBreakthroughActive = true,
            hasBreakthroughDropQueued = true
        )

        assertEquals(0.635f, open.chance, 0.0001f)
        assertEquals(0.375f, penalized.chance, 0.0001f)
        assertEquals(0.16f, clampedMin.chance, 0.0001f)
    }

    @Test
    fun supplyLaneCentersOnGateZoneWhenPresent() {
        val lane = TunnelModeSystem.supplyLane(
            worldWidth = 100f,
            boardCols = 10,
            gateZone = ModeBoardMetrics.TunnelGateZone(
                minCol = 2,
                maxCol = 4,
                rows = 3..6
            )
        )

        assertEquals(3, lane.centerCol)
        assertEquals(35f, lane.laneX, 0.0001f)
        assertEquals(9f, lane.spread, 0.0001f)
    }

    @Test
    fun supplyLaneDefaultsToBoardCenterWithoutGateZone() {
        val lane = TunnelModeSystem.supplyLane(
            worldWidth = 100f,
            boardCols = 12,
            gateZone = null
        )

        assertEquals(6, lane.centerCol)
        assertEquals(54.166668f, lane.laneX, 0.0001f)
        assertEquals(9f, lane.spread, 0.0001f)
    }

    @Test
    fun supplySpawnPointUsesPressureBandAndClampsX() {
        val safeSpawn = TunnelModeSystem.supplySpawnPoint(
            worldWidth = 100f,
            worldHeight = 100f,
            paddleY = 18f,
            laneX = 50f,
            spread = 10f,
            gatePressure = 0.4f,
            xJitterUnit = 1f
        )
        val criticalSpawn = TunnelModeSystem.supplySpawnPoint(
            worldWidth = 20f,
            worldHeight = 100f,
            paddleY = 18f,
            laneX = 10f,
            spread = 30f,
            gatePressure = 0.7f,
            xJitterUnit = 1f
        )

        assertEquals(55f, safeSpawn.x, 0.0001f)
        assertEquals(60f, safeSpawn.y, 0.0001f)
        assertEquals(12f, criticalSpawn.x, 0.0001f)
        assertEquals(52f, criticalSpawn.y, 0.0001f)
        assertTrue(criticalSpawn.y < safeSpawn.y)
    }

    @Test
    fun breakthroughHintStaysOffWhenPressureLowAndNoGateBrick() {
        val pacing = TunnelModeSystem.breakthroughPacing(
            gatePressure = 0.3f,
            tunnelShotsFired = 3,
            gateBrickHit = false
        )
        assertFalse(pacing.preferBreakthroughHint)
    }

    @Test
    fun supplyDropDecision_requiresShotThresholdAndRollPass() {
        val gate = TunnelModeSystem.SupplyDropGate(requiredShots = 7, chance = 0.45f)

        val tooEarly = TunnelModeSystem.supplyDropDecision(
            shotsSinceDrop = 6,
            gate = gate,
            roll = 0.1f
        )
        val failedRoll = TunnelModeSystem.supplyDropDecision(
            shotsSinceDrop = 7,
            gate = gate,
            roll = 0.7f
        )
        val success = TunnelModeSystem.supplyDropDecision(
            shotsSinceDrop = 8,
            gate = gate,
            roll = 0.2f
        )

        assertFalse(tooEarly.shouldDrop)
        assertFalse(failedRoll.shouldDrop)
        assertTrue(success.shouldDrop)
        assertFalse(success.forcedByPity)
        assertEquals(7, success.requiredShots)
        assertEquals(0.45f, success.chance, 0.0001f)
    }

    @Test
    fun supplyDropDecision_forcesDropAfterPityThreshold() {
        val gate = TunnelModeSystem.SupplyDropGate(requiredShots = 6, chance = 0.2f)

        val pity = TunnelModeSystem.supplyDropDecision(
            shotsSinceDrop = 10,
            gate = gate,
            roll = 0.99f
        )

        assertTrue(pity.shouldDrop)
        assertTrue(pity.forcedByPity)
    }

    @Test
    fun supplyReadinessPercent_tracksThresholdProgress() {
        assertEquals(0, TunnelModeSystem.supplyReadinessPercent(shotsSinceDrop = 0, requiredShots = 7))
        assertEquals(57, TunnelModeSystem.supplyReadinessPercent(shotsSinceDrop = 4, requiredShots = 7))
        assertEquals(100, TunnelModeSystem.supplyReadinessPercent(shotsSinceDrop = 9, requiredShots = 7))
    }
}
