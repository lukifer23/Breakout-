package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeLayoutPolicyTest {
    @Test
    fun volleyRowBoost_prefersHigherDensityOnSlate() {
        val slateRows = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 1.55f,
            isSlate = true,
            levelIndex = 0
        )
        val phoneRows = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 1.55f,
            isSlate = false,
            levelIndex = 0
        )

        assertTrue(slateRows > phoneRows)
        assertEquals(9, slateRows)
        assertEquals(6, phoneRows)
    }

    @Test
    fun volleyRowBoost_appliesProgressionOnSlate() {
        val early = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 1.7f,
            isSlate = true,
            levelIndex = 0
        )
        val late = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 1.7f,
            isSlate = true,
            levelIndex = 18
        )

        assertEquals(9, early)
        assertEquals(11, late)
    }

    @Test
    fun volleyRowBoost_compactsTallPhoneLayouts() {
        val veryTall = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 2.25f,
            isSlate = false,
            levelIndex = 3
        )
        val tall = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 2.0f,
            isSlate = false,
            levelIndex = 3
        )
        val midTall = ModeLayoutPolicy.volleyRowBoost(
            aspectRatio = 1.8f,
            isSlate = false,
            levelIndex = 3
        )

        assertEquals(2, veryTall)
        assertEquals(3, tall)
        assertEquals(5, midTall)
    }
}
