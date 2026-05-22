package com.breakoutplus.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenModeBehaviorTest {
    @Test
    fun zenMode_isDistinctFromGodMode() {
        assertTrue(GameMode.ZEN.zenMode)
        assertFalse(GameMode.ZEN.godMode)
        assertTrue(GameMode.ZEN.relaxedMode)
        assertTrue(GameMode.ZEN.endless)
    }

    @Test
    fun godMode_remainsPracticeMode() {
        assertTrue(GameMode.GOD.godMode)
        assertFalse(GameMode.GOD.zenMode)
        assertTrue(GameMode.GOD.relaxedMode)
    }

    @Test
    fun zenSuggestedModeMapping_avoidsScoreModes() {
        assertTrue(DailyChallengeManager.suggestedModeForChallenge(ChallengeType.BRICKS_DESTROYED) == GameMode.CLASSIC)
        assertTrue(DailyChallengeManager.suggestedModeForChallenge(ChallengeType.TIME_UNDER_LIMIT) == GameMode.RUSH)
    }
}
