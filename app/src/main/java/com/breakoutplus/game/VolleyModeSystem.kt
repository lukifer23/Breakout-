package com.breakoutplus.game

import kotlin.math.abs

/**
 * Volley-specific turn flow helpers extracted from GameEngine.
 * Pure logic here keeps turn progression reliable while reducing engine branching.
 */
object VolleyModeSystem {
    data class TurnDecision(
        val shouldAutoReleaseStuck: Boolean,
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
        stuckBalls: Int
    ): TurnDecision {
        if (!turnActive) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldResolveTurn = false
            )
        }
        if (queuedBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldResolveTurn = false
            )
        }
        if (inFlightBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = false,
                shouldResolveTurn = false
            )
        }
        if (stuckBalls > 0) {
            return TurnDecision(
                shouldAutoReleaseStuck = true,
                shouldResolveTurn = false
            )
        }
        return TurnDecision(
            shouldAutoReleaseStuck = false,
            shouldResolveTurn = true
        )
    }
}