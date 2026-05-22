package com.breakoutplus.game

import kotlin.math.abs

/**
 * Volley-specific turn flow helpers extracted from GameEngine.
 * Pure logic here keeps turn progression reliable while reducing engine branching.
 */
object VolleyModeSystem {
    const val STARTING_BALL_COUNT = 5
    const val MAX_BALL_COUNT = 20
    const val MIN_ACTIVE_BALL_COUNT = STARTING_BALL_COUNT

    data class TurnDecision(
        val shouldAutoReleaseStuck: Boolean,
        val shouldNudgeStalledBalls: Boolean,
        val shouldResolveTurn: Boolean
    )

    fun isBallInFlight(vx: Float, vy: Float, motionEpsilon: Float = 0.35f): Boolean {
        val epsilon = motionEpsilon.coerceAtLeast(0.01f)
        return abs(vx) > epsilon || abs(vy) > epsilon
    }

    fun evaluateTurnDecision(
        turnActive: Boolean,
        queuedBalls: Int,
        inFlightBalls: Int,
        stuckBalls: Int,
        stalledBalls: Int
    ): TurnDecision {
        if (!turnActive) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldNudgeStalledBalls = false,
                shouldResolveTurn = false
            )
        }
        if (queuedBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldNudgeStalledBalls = false,
                shouldResolveTurn = false
            )
        }
        if (inFlightBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldNudgeStalledBalls = false,
                shouldResolveTurn = false
            )
        }
        if (stalledBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldNudgeStalledBalls = true,
                shouldResolveTurn = false
            )
        }
        if (stuckBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = true,
                shouldNudgeStalledBalls = false,
                shouldResolveTurn = false
            )
        }
        return TurnDecision(
            shouldAutoReleaseStuck = false,
            shouldNudgeStalledBalls = false,
            shouldResolveTurn = true
        )
    }

    fun shouldAwardBall(turnCount: Int, currentBalls: Int, pressure: Float): Boolean {
        val nearBreach = pressure >= 0.52f
        if (nearBreach && currentBalls <= 9 && turnCount % 2 == 0) return true
        if (currentBalls <= 6 && turnCount % 3 == 0) return true
        return when {
            turnCount <= 4 -> true
            turnCount <= 12 -> turnCount % 2 == 0
            turnCount <= 22 -> turnCount % 3 == 0 || turnCount % 5 == 0
            else -> turnCount % 4 == 0 || (currentBalls <= 7 && turnCount % 3 == 0)
        }
    }
}