package com.breakoutplus.game

/**
 * Callback interface for game engine events.
 * Used by GameEngine to communicate state changes to UI layer.
 */
interface GameEventListener {
    fun onScoreUpdated(score: Int)
    fun onLivesUpdated(lives: Int)
    fun onTimeUpdated(secondsRemaining: Int)
    fun onLevelUpdated(level: Int)
    fun onModeUpdated(mode: GameMode)
    fun onPowerupStatus(status: String)
    fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int)
    fun onTip(message: String)
    fun onFpsUpdate(fps: Int)
    fun onShieldUpdated(current: Int, max: Int)
    fun onGameOver(summary: GameSummary)
    fun onLevelComplete(summary: GameSummary)
}
