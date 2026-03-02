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
        val clearedBoardWhilePaused = state == GameState.PAUSED && godModeEnabled && clearedBoard
        val godModeForce =
            godModeEnabled &&
                (state == GameState.PAUSED || state == GameState.READY) &&
                !awaitingNextLevel &&
                lives > 0
        val godModeRecoveryForce =
            godModeEnabled &&
                !awaitingNextLevel &&
                lives > 0 &&
                clearedBoard

        val canAdvance = awaitingNextLevel || clearedBoardWhilePaused || godModeForce || godModeRecoveryForce
        val reason = if (canAdvance) {
            "accepted"
        } else {
            "awaiting=$awaitingNextLevel,state=$state,lives=$lives,clearedBoard=$clearedBoard,godMode=$godModeEnabled"
        }
        return LevelAdvanceDecision(canAdvance = canAdvance, reason = reason)
    }
}
