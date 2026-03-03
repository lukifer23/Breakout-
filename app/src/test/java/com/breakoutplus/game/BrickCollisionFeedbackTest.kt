package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrickCollisionFeedbackTest {

    @Test
    fun comboMultiplier_matchesLegacyThresholds() {
        assertEquals(1f, BrickCollisionFeedback.comboMultiplier(1))
        assertEquals(1.5f, BrickCollisionFeedback.comboMultiplier(2))
        assertEquals(2f, BrickCollisionFeedback.comboMultiplier(4))
        assertEquals(3f, BrickCollisionFeedback.comboMultiplier(7))
        assertEquals(5f, BrickCollisionFeedback.comboMultiplier(10))
    }

    @Test
    fun dynamicBrickSoundRate_clampsAndScalesLikeLegacyLogic() {
        val low = BrickCollisionFeedback.dynamicBrickSoundRate(
            baseRate = 0.6f,
            combo = 10,
            randomUnit = 0f
        )
        assertEquals(0.7f, low)

        val high = BrickCollisionFeedback.dynamicBrickSoundRate(
            baseRate = 1.25f,
            combo = 10,
            randomUnit = 1f
        )
        assertEquals(1.3f, high)

        val mid = BrickCollisionFeedback.dynamicBrickSoundRate(
            baseRate = 1f,
            combo = 5,
            randomUnit = 0.5f
        )
        assertEquals(1.1f, mid)
    }

    @Test
    fun shouldTriggerComboFlash_onlyForHighMultipliers() {
        assertFalse(BrickCollisionFeedback.shouldTriggerComboFlash(1.5f))
        assertTrue(BrickCollisionFeedback.shouldTriggerComboFlash(2f))
        assertTrue(BrickCollisionFeedback.shouldTriggerComboFlash(5f))
    }
}
