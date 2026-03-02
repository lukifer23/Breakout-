package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ModeStatusTextTest {
    @Test
    fun volleyStatusIncludesLaneDepthAndCounts() {
        val text = ModeStatusText.volley(
            volleyBallCount = 7,
            turnNumber = 5,
            depthRows = 4,
            aliveBreakables = 39,
            laneClearance = 12.34f,
            locale = Locale.US
        )
        assertEquals("Volley balls: 7 • Turn 5 • Depth 4 • Bricks 39 • Lane 12.3", text)
    }

    @Test
    fun tunnelStatusIncludesComboWhenActive() {
        val text = ModeStatusText.tunnel(
            shotsFired = 18,
            gateIntegrityPercent = 64,
            breachPercent = 41,
            combo = 3,
            supplyReadinessPercent = 72
        )
        assertEquals("Shots: 18 • Gate 64% • Breach 41% • Supply 72% • Combo x3", text)
    }

    @Test
    fun tunnelStatusOmitsComboWhenInactive() {
        val text = ModeStatusText.tunnel(
            shotsFired = 8,
            gateIntegrityPercent = 88,
            breachPercent = 12,
            combo = 1
        )
        assertEquals("Shots: 8 • Gate 88% • Breach 12%", text)
    }

    @Test
    fun tunnelStatusShowsSupplyReadyAtFullThreshold() {
        val text = ModeStatusText.tunnel(
            shotsFired = 11,
            gateIntegrityPercent = 52,
            breachPercent = 48,
            combo = 1,
            supplyReadinessPercent = 100
        )
        assertEquals("Shots: 11 • Gate 52% • Breach 48% • Supply READY", text)
    }

    @Test
    fun powerupStatusFormatsShieldCharges() {
        val text = ModeStatusText.powerups(
            listOf(
                ModeStatusText.EffectStatus(
                    type = PowerUpType.SHIELD,
                    remainingSeconds = 9,
                    charges = 2
                ),
                ModeStatusText.EffectStatus(
                    type = PowerUpType.LASER,
                    remainingSeconds = 6
                )
            )
        )
        assertEquals("Powerups: Shield x2 9s • Laser 6s", text)
    }

    @Test
    fun powerupStatusEmptyState() {
        assertEquals("Powerups: none", ModeStatusText.powerups(emptyList()))
    }
}
