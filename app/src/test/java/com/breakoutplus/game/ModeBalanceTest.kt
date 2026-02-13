package com.breakoutplus.game

import org.junit.Assert.assertTrue
import org.junit.Test

class ModeBalanceTest {
    @Test
    fun pacingProfilesExistForAllModes() {
        GameMode.values().forEach { mode ->
            val pacing = ModeBalance.pacingFor(mode)
            assertTrue(pacing.speedBoostSlope > 0f)
            assertTrue(pacing.speedBoostCap >= 1f)
            assertTrue(pacing.minSpeedFactor > 0f)
            assertTrue(pacing.maxSpeedFactor > pacing.minSpeedFactor)
            assertTrue(pacing.difficultyBase > 0f)
            assertTrue(pacing.difficultySlope >= 0f)
        }
    }

    @Test
    fun pacingProfilesPreserveModeIdentity() {
        val classic = ModeBalance.pacingFor(GameMode.CLASSIC)
        val timed = ModeBalance.pacingFor(GameMode.TIMED)
        val rush = ModeBalance.pacingFor(GameMode.RUSH)
        val volley = ModeBalance.pacingFor(GameMode.VOLLEY)
        val survival = ModeBalance.pacingFor(GameMode.SURVIVAL)
        val god = ModeBalance.pacingFor(GameMode.GOD)
        val zen = ModeBalance.pacingFor(GameMode.ZEN)

        assertTrue("God mode should remain easiest by base difficulty", god.difficultyBase < classic.difficultyBase)
        assertTrue("Zen should stay easier than God mode", zen.difficultyBase <= god.difficultyBase)
        assertTrue("Zen should cap speed below God mode", zen.speedBoostCap <= god.speedBoostCap)
        assertTrue("Survival should scale harder than classic", survival.difficultySlope > classic.difficultySlope)
        assertTrue("Rush should ramp speed faster than classic", rush.speedBoostSlope > classic.speedBoostSlope)
        assertTrue("Timed should drop slightly more powerups than classic", timed.dropChanceModeBoost > classic.dropChanceModeBoost)
        assertTrue("Rush should not out-scale survival's speed ceiling", rush.speedBoostCap <= survival.speedBoostCap)
        assertTrue("Rush should keep higher drop support than timed", rush.dropChanceModeBoost >= timed.dropChanceModeBoost)
        assertTrue("Volley should keep a gentler speed ramp than timed", volley.speedBoostSlope < timed.speedBoostSlope)
        assertTrue("Volley should cap speed below rush", volley.speedBoostCap < rush.speedBoostCap)
    }

    @Test
    fun invaderPacingRemainsWithinExpectedBounds() {
        val invader = ModeBalance.invaderPacing()
        assertTrue(invader.baseSpeed > 0f)
        assertTrue(invader.speedPerLevel > 0f)
        assertTrue(invader.speedCap >= invader.baseSpeed)
        assertTrue(invader.baseShotCooldown > 0f)
        assertTrue(invader.shotCooldownPerLevel > 0f)
        assertTrue(invader.shotCooldownMin > 0f)
        assertTrue(invader.baseShotCooldown > invader.shotCooldownMin)
        assertTrue(invader.shieldBase > 0f)
        assertTrue(invader.shieldPerLevel >= 0f)
        assertTrue(invader.shieldCap >= invader.shieldBase)
    }
}
