package com.breakoutplus.game

import kotlin.math.sin

/**
 * Invaders-specific pacing and formation math extracted from GameEngine.
 */
object InvadersModeSystem {
    data class PaceAdjustments(
        val speed: Float,
        val shotCooldown: Float
    )

    data class FormationOffsetStep(
        val offset: Float,
        val direction: Float,
        val playTurnSound: Boolean
    )

    fun rowDrift(
        row: Int,
        phase: Float,
        rowPhaseOffset: Float,
        rowDriftAmount: Float
    ): Float {
        return sin(phase + row * rowPhaseOffset) * rowDriftAmount
    }

    fun formationOffsetLimits(
        minBaseX: Float,
        maxBaseX: Float,
        leftBound: Float,
        rightBound: Float
    ): Pair<Float, Float> {
        val minOffsetAllowed = leftBound - minBaseX
        val maxOffsetAllowed = rightBound - maxBaseX
        return minOffsetAllowed to maxOffsetAllowed
    }

    fun nextFormationOffset(
        currentOffset: Float,
        direction: Float,
        speed: Float,
        dt: Float,
        minOffsetAllowed: Float,
        maxOffsetAllowed: Float
    ): FormationOffsetStep {
        if (minOffsetAllowed > maxOffsetAllowed) {
            val centered = (minOffsetAllowed + maxOffsetAllowed) * 0.5f
            return FormationOffsetStep(
                offset = centered,
                direction = direction,
                playTurnSound = false
            )
        }

        var proposed = currentOffset + speed * direction * dt
        var nextDirection = direction
        var playTurnSound = false

        if (proposed < minOffsetAllowed) {
            val overshoot = minOffsetAllowed - proposed
            proposed = minOffsetAllowed + overshoot
            if (direction < 0f) {
                nextDirection = 1f
                playTurnSound = true
            }
        }
        if (proposed > maxOffsetAllowed) {
            val overshoot = proposed - maxOffsetAllowed
            proposed = maxOffsetAllowed - overshoot
            if (direction > 0f) {
                nextDirection = -1f
                playTurnSound = true
            }
        }

        return FormationOffsetStep(
            offset = proposed.coerceIn(minOffsetAllowed, maxOffsetAllowed),
            direction = nextDirection,
            playTurnSound = playTurnSound
        )
    }

    fun paceAdjustments(
        aliveCount: Int,
        totalCount: Int,
        baseSpeed: Float,
        baseShotCooldown: Float
    ): PaceAdjustments {
        val ratio = aliveCount.toFloat() / totalCount.toFloat().coerceAtLeast(1f)
        val paceBoost = (1f - ratio).coerceIn(0f, 1f)
        return PaceAdjustments(
            speed = baseSpeed * (1f + paceBoost * 0.5f),
            shotCooldown = (baseShotCooldown * (1f - paceBoost * 0.4f)).coerceIn(0.4f, baseShotCooldown)
        )
    }

    fun volleyShotCount(levelIndex: Int): Int {
        return (2 + levelIndex / 3).coerceAtMost(4)
    }

    fun canSpawnShot(levelIndex: Int, currentShots: Int): Boolean {
        val cap = 6 + (levelIndex / 2).coerceAtMost(6)
        return currentShots < cap
    }
}
