package com.breakoutplus.game

/** Observes real engine outputs; it supplies no simulated responses or game behavior. */
class RecordingGameEvents : GameEventListener {
    val events = mutableListOf<Pair<String, Any>>()
    private fun record(kind: String, value: Any) { events.add(kind to value) }
    override fun onDailyChallengesUpdated(challenges: List<DailyChallenge>) = record("challenges", challenges)
    override fun onScoreUpdated(score: Int) = record("score", score)
    override fun onLivesUpdated(lives: Int) = record("lives", lives)
    override fun onTimeUpdated(secondsRemaining: Int) = record("time", secondsRemaining)
    override fun onLevelUpdated(level: Int) = record("level", level)
    override fun onModeUpdated(mode: GameMode) = record("mode", mode)
    override fun onPowerupStatus(status: String) = record("powerupText", status)
    override fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int) = record("powerups", status to combo)
    override fun onTip(message: String) = record("tip", message)
    override fun onFpsUpdate(fps: Int) = record("fps", fps)
    override fun onShieldUpdated(current: Int, max: Int) = record("shield", current to max)
    override fun onLaserFired(cooldownSeconds: Float) = record("laser", cooldownSeconds)
    override fun onVolleyBallsUpdated(volleyBalls: Int) = record("volleyBalls", volleyBalls)
    override fun onThemeUnlocked(themeName: String) = record("theme", themeName)
    override fun onCosmeticUnlocked(newTier: Int) = record("cosmetic", newTier)
    override fun onGameOver(summary: GameSummary) = record("gameOver", summary)
    override fun onLevelComplete(summary: GameSummary) = record("levelComplete", summary)
}
