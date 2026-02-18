package com.breakoutplus.game

import kotlin.math.roundToInt

/**
 * Shared board-state metrics used by mode-specific systems (Volley, Tunnel, etc.).
 * Centralizing this math keeps behavior consistent and reduces duplicated scans.
 */
object ModeBoardMetrics {
    data class BreakableCounts(
        val totalBreakables: Int,
        val aliveBreakables: Int
    )

    data class VolleyBoardMetrics(
        val aliveBreakables: Int,
        val closestAliveY: Float,
        val breachY: Float,
        val laneClearance: Float,
        val pressure: Float
    )

    data class TunnelGateZone(
        val minCol: Int,
        val maxCol: Int,
        val rows: IntRange
    )

    data class TunnelGateMetrics(
        val totalBreakables: Int,
        val aliveBreakables: Int,
        val integrityPercent: Int
    )

    fun breakableCounts(bricks: List<Brick>): BreakableCounts {
        var total = 0
        var alive = 0
        for (brick in bricks) {
            if (brick.type == BrickType.UNBREAKABLE) continue
            total += 1
            if (brick.alive) {
                alive += 1
            }
        }
        return BreakableCounts(totalBreakables = total, aliveBreakables = alive)
    }

    fun volleyBreachY(paddleY: Float, paddleHeight: Float): Float {
        return paddleY + paddleHeight * 0.5f + 1.8f
    }

    fun volleyMetrics(
        bricks: List<Brick>,
        paddleY: Float,
        paddleHeight: Float,
        worldHeight: Float,
        laneWindowRatio: Float = 0.2f
    ): VolleyBoardMetrics {
        var aliveBreakables = 0
        var closestAliveY = Float.POSITIVE_INFINITY
        for (brick in bricks) {
            if (!brick.alive) continue
            if (brick.type != BrickType.UNBREAKABLE) {
                aliveBreakables += 1
            }
            if (brick.y < closestAliveY) {
                closestAliveY = brick.y
            }
        }
        if (!closestAliveY.isFinite()) {
            closestAliveY = worldHeight
        }
        val breachY = volleyBreachY(paddleY = paddleY, paddleHeight = paddleHeight)
        val laneClearance = (closestAliveY - breachY).coerceAtLeast(0f)
        val laneWindow = (worldHeight * laneWindowRatio).coerceAtLeast(0.0001f)
        val pressure = ((breachY + laneWindow - closestAliveY) / laneWindow).coerceIn(0f, 1f)
        return VolleyBoardMetrics(
            aliveBreakables = aliveBreakables,
            closestAliveY = closestAliveY,
            breachY = breachY,
            laneClearance = laneClearance,
            pressure = pressure
        )
    }

    fun hasVolleyBreach(bricks: List<Brick>, breachY: Float): Boolean {
        return bricks.any { it.alive && it.y <= breachY }
    }

    fun tunnelGateZone(
        layoutCols: Int,
        layoutRows: Int,
        layoutColBoost: Int,
        levelIndex: Int
    ): TunnelGateZone {
        val cols = layoutCols.coerceAtLeast(1)
        val boardCols = (cols + layoutColBoost).coerceAtLeast(cols)
        val rows = layoutRows.coerceAtLeast(1)
        val colOffset = layoutColBoost / 2
        val center = cols / 2 + colOffset
        val gateHalfWidth = when {
            levelIndex < 4 -> 2
            levelIndex < 10 -> 1
            else -> 1
        }
        val gateMinCol = (center - gateHalfWidth).coerceAtLeast(0)
        val gateMaxCol = (center + gateHalfWidth).coerceAtMost(boardCols - 1)
        val fortressBottomRow = (rows - 4).coerceAtLeast(0)
        val gateRowMin = (fortressBottomRow - 1).coerceAtLeast(0)
        val gateRowMax = (fortressBottomRow + 2).coerceAtMost(rows - 1)
        return TunnelGateZone(
            minCol = gateMinCol,
            maxCol = gateMaxCol,
            rows = gateRowMin..gateRowMax
        )
    }

    fun tunnelGateMetrics(
        bricks: List<Brick>,
        gateZone: TunnelGateZone
    ): TunnelGateMetrics {
        var totalGateBreakables = 0
        var aliveGateBreakables = 0
        for (brick in bricks) {
            if (brick.type == BrickType.UNBREAKABLE) continue
            if (brick.gridX !in gateZone.minCol..gateZone.maxCol) continue
            if (brick.gridY !in gateZone.rows) continue
            totalGateBreakables += 1
            if (brick.alive) {
                aliveGateBreakables += 1
            }
        }
        if (totalGateBreakables == 0) {
            return TunnelGateMetrics(
                totalBreakables = 0,
                aliveBreakables = 0,
                integrityPercent = 100
            )
        }
        val integrityPercent = ((aliveGateBreakables.toFloat() / totalGateBreakables.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        return TunnelGateMetrics(
            totalBreakables = totalGateBreakables,
            aliveBreakables = aliveGateBreakables,
            integrityPercent = integrityPercent
        )
    }

    fun tunnelBreakthroughPressure(
        gateIntegrityPercent: Int,
        tunnelShotsFired: Int
    ): Float {
        val gateIntegrityPressure = gateIntegrityPercent.coerceIn(0, 100) / 100f
        val shotPressure = ((tunnelShotsFired - 6).coerceAtLeast(0) / 20f).coerceIn(0f, 1f)
        return (gateIntegrityPressure * (0.78f + shotPressure * 0.22f)).coerceIn(0f, 1f)
    }
}
