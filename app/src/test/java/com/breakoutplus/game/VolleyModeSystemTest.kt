package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolleyModeSystemTest {
    @Test
    fun isBallInFlight_ignoresNearZeroVelocityJitter() {
        assertFalse(VolleyModeSystem.isBallInFlight(0.05f, 0.1f, motionEpsilon = 0.35f))
        assertTrue(VolleyModeSystem.isBallInFlight(0.4f, 0.1f, motionEpsilon = 0.35f))
    }

    @Test
    fun startingBallCount_matchesGameplayIdentity() {
        assertEquals(5, VolleyModeSystem.STARTING_BALL_COUNT)
        assertEquals(20, VolleyModeSystem.MAX_BALL_COUNT)
    }

    @Test
    fun evaluateTurnDecision_waitsWhileBallsAreQueued() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 2,
            inFlightBalls = 0,
            stuckBalls = 0,
            stalledBalls = 0
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldNudgeStalledBalls)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun evaluateTurnDecision_waitsWhileBallsAreInFlight() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 2,
            stuckBalls = 0,
            stalledBalls = 0
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldNudgeStalledBalls)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun evaluateTurnDecision_autoReleasesWhenOnlyStuckBallsRemain() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 0,
            stuckBalls = 3,
            stalledBalls = 0
        )

        assertTrue(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldNudgeStalledBalls)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun evaluateTurnDecision_nudgesWhenOnlyStalledBallsRemain() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 0,
            stuckBalls = 0,
            stalledBalls = 2
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertTrue(decision.shouldNudgeStalledBalls)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun shouldAwardBall_grantsEarlyTurnBonuses() {
        assertTrue(VolleyModeSystem.shouldAwardBall(turnCount = 2, currentBalls = 5, pressure = 0.1f))
        assertTrue(VolleyModeSystem.shouldAwardBall(turnCount = 4, currentBalls = 8, pressure = 0.6f))
    }

    @Test
    fun evaluateTurnDecision_resolvesWhenNoBallsRemainInPlay() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 0,
            stuckBalls = 0,
            stalledBalls = 0
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldNudgeStalledBalls)
        assertTrue(decision.shouldResolveTurn)
    }
}
