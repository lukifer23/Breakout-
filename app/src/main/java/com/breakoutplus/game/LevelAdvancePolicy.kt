package com.breakoutplus.game

internal data class LevelAdvanceDecision(
    val canAdvance: Boolean,
    val reason: String
)

internal object LevelAdvancePolicy {
    fun evaluate(
        awaitingNextLevel: Boolean,
        state: GameState,
        lives: Int,
        clearedBoard: Boolean,
        godModeEnabled: Boolean
    ): LevelAdvanceDecision {
        val relaxedMode = godModeEnabled
        val clearedBoardWhilePaused = state == GameState.PAUSED && relaxedMode && clearedBoard
        val relaxedForce =
            relaxedMode &&
                (state == GameState.PAUSED || state == GameState.READY || state == GameState.RUNNING) &&
                !awaitingNextLevel &&
                lives > 0
        val relaxedRecoveryForce =
            relaxedMode &&
                !awaitingNextLevel &&
                lives > 0 &&
                clearedBoard

        val canAdvance = awaitingNextLevel || clearedBoardWhilePaused || relaxedForce || relaxedRecoveryForce || (relaxedMode && clearedBoard)
        val reason = if (canAdvance) {
            "accepted"
        } else {
            "awaiting=$awaitingNextLevel,state=$state,lives=$lives,clearedBoard=$clearedBoard,relaxedMode=$relaxedMode"
        }
        return LevelAdvanceDecision(canAdvance = canAdvance, reason = reason)
    }
}
