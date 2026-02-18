package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerupDropModelTest {
    @Test
    fun baseChanceMappingRemainsStable() {
        assertEquals(0.30f, PowerupDropModel.baseChance(BrickType.EXPLOSIVE), 0.0001f)
        assertEquals(0.2f, PowerupDropModel.baseChance(BrickType.REINFORCED), 0.0001f)
        assertEquals(0.2f, PowerupDropModel.baseChance(BrickType.ARMORED), 0.0001f)
        assertEquals(0.27f, PowerupDropModel.baseChance(BrickType.BOSS), 0.0001f)
        assertEquals(0.27f, PowerupDropModel.baseChance(BrickType.PHASE), 0.0001f)
        assertEquals(0.18f, PowerupDropModel.baseChance(BrickType.SPAWNING), 0.0001f)
        assertEquals(0.15f, PowerupDropModel.baseChance(BrickType.MOVING), 0.0001f)
        assertEquals(0.16f, PowerupDropModel.baseChance(BrickType.INVADER), 0.0001f)
        assertEquals(0.12f, PowerupDropModel.baseChance(BrickType.NORMAL), 0.0001f)
        assertEquals(0.12f, PowerupDropModel.baseChance(BrickType.UNBREAKABLE), 0.0001f)
    }

    @Test
    fun levelBoostCapsAtConfiguredMaximum() {
        assertEquals(0f, PowerupDropModel.levelBoost(0), 0.0001f)
        assertEquals(0.035f, PowerupDropModel.levelBoost(10), 0.0001f)
        assertEquals(0.06f, PowerupDropModel.levelBoost(30), 0.0001f)
        assertEquals(0.06f, PowerupDropModel.levelBoost(999), 0.0001f)
    }

    @Test
    fun dropChanceIsClampedWithinHealthBounds() {
        val minClamp = PowerupDropModel.dropChance(
            baseChance = 0.01f,
            levelIndex = 0,
            modeBoost = -0.2f,
            dynamicBoost = -0.2f
        )
        val maxClamp = PowerupDropModel.dropChance(
            baseChance = 0.4f,
            levelIndex = 50,
            modeBoost = 0.2f,
            dynamicBoost = 0.2f
        )
        assertEquals(0.08f, minClamp, 0.0001f)
        assertEquals(0.48f, maxClamp, 0.0001f)
    }

    @Test
    fun dropChanceRespondsToModeAndTunnelPressure() {
        val base = PowerupDropModel.baseChance(BrickType.NORMAL)
        val classic = PowerupDropModel.dropChance(
            baseChance = base,
            levelIndex = 8,
            modeBoost = ModeBalance.pacingFor(GameMode.CLASSIC).dropChanceModeBoost,
            dynamicBoost = 0f
        )
        val tunnelPacing = TunnelModeSystem.breakthroughPacing(
            gatePressure = 0.7f,
            tunnelShotsFired = 12,
            gateBrickHit = true
        )
        val tunnelBoosted = PowerupDropModel.dropChance(
            baseChance = base,
            levelIndex = 8,
            modeBoost = ModeBalance.pacingFor(GameMode.TUNNEL).dropChanceModeBoost,
            dynamicBoost = tunnelPacing.dynamicBoost
        )

        assertTrue(tunnelBoosted > classic)
    }

    @Test
    fun dropChanceStaysInRangeAcrossModeProfiles() {
        val testBricks = listOf(
            BrickType.NORMAL,
            BrickType.REINFORCED,
            BrickType.EXPLOSIVE,
            BrickType.BOSS
        )
        GameMode.values().forEach { mode ->
            val modeBoost = ModeBalance.pacingFor(mode).dropChanceModeBoost
            testBricks.forEach { type ->
                val chance = PowerupDropModel.dropChance(
                    baseChance = PowerupDropModel.baseChance(type),
                    levelIndex = 12,
                    modeBoost = modeBoost,
                    dynamicBoost = 0.08f
                )
                assertTrue(chance in 0.08f..0.48f)
            }
        }
    }
}
