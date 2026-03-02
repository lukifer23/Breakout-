package com.breakoutplus.game

import kotlin.math.roundToInt

/**
 * Tunnel-specific pacing and supply-drop helpers extracted from GameEngine.
 * Pure calculations here keep Tunnel behavior consistent while reducing engine complexity.
 */
object TunnelModeSystem {
    data class BreakthroughPacing(
        val dynamicBoost: Float,
        val preferBreakthroughHint: Boolean
    )

    data class SupplyDropGate(
        val requiredShots: Int,
        val chance: Float
    )

    data class SupplyDropDecision(
        val shouldDrop: Boolean,
        val forcedByPity: Boolean,
        val requiredShots: Int,
        val chance: Float
    )

    data class SupplyLane(
        val centerCol: Int,
        val laneX: Float,
        val spread: Float
    )

    data class SupplySpawnPoint(
        val x: Float,
        val y: Float
    )

    fun breakthroughPacing(
        gatePressure: Float,
        tunnelShotsFired: Int,
        gateBrickHit: Boolean
    ): BreakthroughPacing {
        val pressure = gatePressure.coerceIn(0f, 1f)
        val stallBoost = ((tunnelShotsFired - 8).coerceAtLeast(0) / 16f).coerceAtMost(1f) * pressure * 0.04f
        var dynamicBoost = pressure * 0.06f + stallBoost
        if (gateBrickHit) {
            dynamicBoost += 0.07f
        }
        return BreakthroughPacing(
            dynamicBoost = dynamicBoost,
            preferBreakthroughHint = gateBrickHit || pressure >= 0.62f
        )
    }

    fun supplyDropGate(
        gatePressure: Float,
        gateIntegrityPercent: Int,
        hasBreakthroughActive: Boolean,
        hasBreakthroughDropQueued: Boolean
    ): SupplyDropGate {
        val pressure = gatePressure.coerceIn(0f, 1f)
        val requiredShots = when {
            pressure >= 0.78f -> 5
            pressure >= 0.6f -> 7
            else -> 9
        }
        val chance = (
            0.2f +
                pressure * 0.42f +
                (if (gateIntegrityPercent >= 70) 0.12f else 0f) -
                (if (hasBreakthroughActive) 0.16f else 0f) -
                (if (hasBreakthroughDropQueued) 0.1f else 0f)
            ).coerceIn(0.16f, 0.78f)
        return SupplyDropGate(
            requiredShots = requiredShots,
            chance = chance
        )
    }

    fun supplyLane(
        worldWidth: Float,
        boardCols: Int,
        gateZone: ModeBoardMetrics.TunnelGateZone?
    ): SupplyLane {
        val cols = boardCols.coerceAtLeast(1)
        val centerCol = if (gateZone != null) {
            ((gateZone.minCol + gateZone.maxCol) * 0.5f).roundToInt().coerceIn(0, cols - 1)
        } else {
            cols / 2
        }
        val colWidth = worldWidth / cols.toFloat()
        return SupplyLane(
            centerCol = centerCol,
            laneX = (centerCol + 0.5f) * colWidth,
            spread = (colWidth * 1.3f).coerceIn(2.5f, 9f)
        )
    }

    fun supplySpawnPoint(
        worldWidth: Float,
        worldHeight: Float,
        paddleY: Float,
        laneX: Float,
        spread: Float,
        gatePressure: Float,
        xJitterUnit: Float
    ): SupplySpawnPoint {
        val spawnX = (laneX + (xJitterUnit.coerceIn(0f, 1f) - 0.5f) * spread).coerceIn(8f, worldWidth - 8f)
        val spawnY = (worldHeight * if (gatePressure >= 0.68f) 0.52f else 0.6f)
            .coerceIn(paddleY + 12f, worldHeight * 0.82f)
        return SupplySpawnPoint(x = spawnX, y = spawnY)
    }

    fun supplyDropDecision(
        shotsSinceDrop: Int,
        gate: SupplyDropGate,
        roll: Float
    ): SupplyDropDecision {
        val pityThreshold = gate.requiredShots + 4
        val forcedByPity = shotsSinceDrop >= pityThreshold
        val shouldDrop = shotsSinceDrop > 0 &&
            (
                forcedByPity ||
                    (shotsSinceDrop >= gate.requiredShots && roll <= gate.chance)
                )
        return SupplyDropDecision(
            shouldDrop = shouldDrop,
            forcedByPity = forcedByPity,
            requiredShots = gate.requiredShots,
            chance = gate.chance
        )
    }

    fun supplyReadinessPercent(
        shotsSinceDrop: Int,
        requiredShots: Int
    ): Int {
        if (shotsSinceDrop <= 0 || requiredShots <= 0) return 0
        return ((shotsSinceDrop.toFloat() / requiredShots.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)
    }
}
