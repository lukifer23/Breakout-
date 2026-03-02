package com.breakoutplus.game

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
    fun evaluateTurnDecision_waitsWhileBallsAreQueued() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 2,
            inFlightBalls = 0,
            stuckBalls = 0
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun evaluateTurnDecision_autoReleasesWhenOnlyStuckBallsRemain() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 0,
            stuckBalls = 3
        )

        assertTrue(decision.shouldAutoReleaseStuck)
        assertFalse(decision.shouldResolveTurn)
    }

    @Test
    fun evaluateTurnDecision_resolvesWhenNoBallsRemainInPlay() {
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = true,
            queuedBalls = 0,
            inFlightBalls = 0,
            stuckBalls = 0
        )

        assertFalse(decision.shouldAutoReleaseStuck)
        assertTrue(decision.shouldResolveTurn)
    }
}
