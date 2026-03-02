package com.breakoutplus.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelAdvancePolicyTest {
    @Test
    fun awaitingNextLevelAlwaysAllowsAdvance() {
        val decision = LevelAdvancePolicy.evaluate(
            awaitingNextLevel = true,
            state = GameState.PAUSED,
            lives = 1,
            clearedBoard = false,
            godModeEnabled = false
        )

        assertTrue(decision.canAdvance)
    }

    @Test
    fun godModeAllowsManualSkipFromReadyState() {
        val decision = LevelAdvancePolicy.evaluate(
            awaitingNextLevel = false,
            state = GameState.READY,
            lives = 1,
            clearedBoard = false,
            godModeEnabled = true
        )

        assertTrue(decision.canAdvance)
    }

    @Test
    fun godModeAllowsRecoveryWhenBoardAlreadyCleared() {
        val decision = LevelAdvancePolicy.evaluate(
            awaitingNextLevel = false,
            state = GameState.RUNNING,
            lives = 1,
            clearedBoard = true,
            godModeEnabled = true
        )

        assertTrue(decision.canAdvance)
    }

    @Test
    fun standardModeRejectsAdvanceWithoutAwaitingSignal() {
        val decision = LevelAdvancePolicy.evaluate(
            awaitingNextLevel = false,
            state = GameState.READY,
            lives = 3,
            clearedBoard = false,
            godModeEnabled = false
        )

        assertFalse(decision.canAdvance)
    }
}
