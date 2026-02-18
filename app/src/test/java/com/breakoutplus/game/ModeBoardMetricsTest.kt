package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeBoardMetricsTest {
    @Test
    fun breakableCountsExcludesUnbreakables() {
        val bricks = listOf(
            brick(y = 30f, type = BrickType.NORMAL, alive = true),
            brick(y = 40f, type = BrickType.NORMAL, alive = false),
            brick(y = 20f, type = BrickType.UNBREAKABLE, alive = true),
            brick(y = 10f, type = BrickType.UNBREAKABLE, alive = false)
        )

        val counts = ModeBoardMetrics.breakableCounts(bricks)

        assertEquals(2, counts.totalBreakables)
        assertEquals(1, counts.aliveBreakables)
    }

    @Test
    fun volleyBreachYMatchesLaunchLineFormula() {
        val breachY = ModeBoardMetrics.volleyBreachY(paddleY = 10f, paddleHeight = 4f)
        assertEquals(13.8f, breachY, 0.0001f)
    }

    @Test
    fun volleyMetricsCalculatesPressureAndLaneClearance() {
        val bricks = listOf(
            brick(y = 20f, type = BrickType.NORMAL, alive = true),
            brick(y = 25f, type = BrickType.UNBREAKABLE, alive = true),
            brick(y = 45f, type = BrickType.NORMAL, alive = false)
        )

        val metrics = ModeBoardMetrics.volleyMetrics(
            bricks = bricks,
            paddleY = 10f,
            paddleHeight = 4f,
            worldHeight = 100f
        )

        assertEquals(1, metrics.aliveBreakables)
        assertEquals(20f, metrics.closestAliveY, 0.0001f)
        assertEquals(13.8f, metrics.breachY, 0.0001f)
        assertEquals(6.2f, metrics.laneClearance, 0.0001f)
        assertEquals(0.69f, metrics.pressure, 0.0001f)
    }

    @Test
    fun volleyMetricsUsesWorldHeightWhenNoAliveBricks() {
        val bricks = listOf(
            brick(y = 20f, type = BrickType.NORMAL, alive = false),
            brick(y = 25f, type = BrickType.UNBREAKABLE, alive = false)
        )

        val metrics = ModeBoardMetrics.volleyMetrics(
            bricks = bricks,
            paddleY = 10f,
            paddleHeight = 4f,
            worldHeight = 100f
        )

        assertEquals(0, metrics.aliveBreakables)
        assertEquals(100f, metrics.closestAliveY, 0.0001f)
        assertEquals(86.2f, metrics.laneClearance, 0.0001f)
        assertEquals(0f, metrics.pressure, 0.0001f)
    }

    @Test
    fun hasVolleyBreachRequiresAliveBrickInsideBreachLine() {
        val breachY = 13.8f
        val safeBricks = listOf(
            brick(y = 12f, type = BrickType.NORMAL, alive = false),
            brick(y = 20f, type = BrickType.NORMAL, alive = true)
        )
        val breachedBricks = safeBricks + brick(y = 13.8f, type = BrickType.UNBREAKABLE, alive = true)

        assertFalse(ModeBoardMetrics.hasVolleyBreach(safeBricks, breachY))
        assertTrue(ModeBoardMetrics.hasVolleyBreach(breachedBricks, breachY))
    }

    @Test
    fun tunnelGateZoneAdaptsToLevelAndLayoutBoost() {
        val early = ModeBoardMetrics.tunnelGateZone(
            layoutCols = 12,
            layoutRows = 9,
            layoutColBoost = 2,
            levelIndex = 1
        )
        val later = ModeBoardMetrics.tunnelGateZone(
            layoutCols = 12,
            layoutRows = 9,
            layoutColBoost = 2,
            levelIndex = 7
        )

        assertEquals(5, early.minCol)
        assertEquals(9, early.maxCol)
        assertEquals(4..7, early.rows)
        assertEquals(6, later.minCol)
        assertEquals(8, later.maxCol)
        assertEquals(4..7, later.rows)
    }

    @Test
    fun tunnelGateMetricsCountsOnlyBreakablesInGateZone() {
        val gateZone = ModeBoardMetrics.TunnelGateZone(
            minCol = 5,
            maxCol = 7,
            rows = 4..7
        )
        val bricks = listOf(
            gateBrick(gridX = 5, gridY = 4, type = BrickType.NORMAL, alive = true),
            gateBrick(gridX = 6, gridY = 6, type = BrickType.REINFORCED, alive = false),
            gateBrick(gridX = 7, gridY = 6, type = BrickType.UNBREAKABLE, alive = true),
            gateBrick(gridX = 3, gridY = 6, type = BrickType.NORMAL, alive = true),
            gateBrick(gridX = 5, gridY = 8, type = BrickType.NORMAL, alive = true)
        )

        val metrics = ModeBoardMetrics.tunnelGateMetrics(bricks, gateZone)

        assertEquals(2, metrics.totalBreakables)
        assertEquals(1, metrics.aliveBreakables)
        assertEquals(50, metrics.integrityPercent)
    }

    @Test
    fun tunnelGateMetricsDefaultsToFullIntegrityWhenNoGateBreakables() {
        val gateZone = ModeBoardMetrics.TunnelGateZone(
            minCol = 5,
            maxCol = 7,
            rows = 4..7
        )
        val bricks = listOf(
            gateBrick(gridX = 7, gridY = 5, type = BrickType.UNBREAKABLE, alive = true),
            gateBrick(gridX = 4, gridY = 5, type = BrickType.NORMAL, alive = true)
        )

        val metrics = ModeBoardMetrics.tunnelGateMetrics(bricks, gateZone)

        assertEquals(0, metrics.totalBreakables)
        assertEquals(0, metrics.aliveBreakables)
        assertEquals(100, metrics.integrityPercent)
    }

    @Test
    fun tunnelBreakthroughPressureTracksIntegrityAndShotCadence() {
        val pressure = ModeBoardMetrics.tunnelBreakthroughPressure(
            gateIntegrityPercent = 80,
            tunnelShotsFired = 16
        )
        assertEquals(0.712f, pressure, 0.0001f)
    }

    private fun brick(
        y: Float,
        type: BrickType,
        alive: Boolean
    ): Brick {
        return Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = y,
            width = 6f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = type,
            alive = alive
        )
    }

    private fun gateBrick(
        gridX: Int,
        gridY: Int,
        type: BrickType,
        alive: Boolean
    ): Brick {
        return Brick(
            gridX = gridX,
            gridY = gridY,
            x = 10f,
            y = 20f,
            width = 6f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = type,
            alive = alive
        )
    }
}
