package com.breakoutplus.game

/** Local diagnostics boundary; the simulation never owns files or a Context. */
interface GameDiagnostics {
    fun logSessionStart(mode: GameMode, seed: Long)
    fun logLevelStart(levelIndex: Int, theme: String)
    fun logLevelAdvance(newLevelIndex: Int)
    fun logLevelComplete(levelIndex: Int, score: Int, timeTaken: Float, bricksRemaining: Int)
    fun logGameOver(finalScore: Int, levelReached: Int, reason: String)
    fun logError(message: String, extraData: Map<String, Any> = emptyMap())
    fun logBrickDestroyed(brickType: BrickType, position: Pair<Float, Float>, comboCount: Int)
    fun logPowerupCollected(powerupType: PowerUpType, position: Pair<Float, Float>)
    fun logBallLost(ballCount: Int, position: Pair<Float, Float>, livesRemaining: Int)
    fun logComboAchieved(comboCount: Int, multiplier: Float, scoreGained: Int)
    fun logTouchInput(action: String, x: Float, y: Float, pressure: Float = 1f)
}
