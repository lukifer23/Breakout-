package com.breakoutplus.game

import kotlin.math.roundToInt

internal fun GameEngine.addScore(points: Int) {
    if (config.mode == GameMode.ZEN) return
    val boost = (1f + rewardScoreMultiplier).coerceAtLeast(1f)
    val doubleScoreMultiplier = if (activeEffects.containsKey(PowerUpType.DOUBLE_SCORE)) 2f else 1f
    val boosted = (points * boost * doubleScoreMultiplier).roundToInt()
    score += boosted
    if (streakBonusRemaining > 0) {
        score += streakBonusPerBrick
        streakBonusRemaining -= 1
        if (streakBonusRemaining <= 0 && streakBonusActive) {
            streakBonusActive = false
            listener.onTip("Streak bonus complete")
        }
    }
}

internal fun GameEngine.reportScore() {
    updateScoreChallenges()
    listener.onScoreUpdated(score)
}

internal fun GameEngine.updateScoreChallenges() {
    val challenges = dailyChallenges ?: return
    val completed = DailyChallengeManager.applyEvent(challenges,
        ChallengeEvent.Metric(ChallengeType.SCORE_ACHIEVED, score))
    publishChallengeProgress(completed)
}

internal fun GameEngine.checkLevelCompletion() {
    if (state != GameState.RUNNING || awaitingNextLevel) return
    val hasRemainingBreakables = aliveBreakableBrickCount > 0
    if (!hasRemainingBreakables) {
        val levelDuration = elapsedSeconds - levelStartTime
        dailyChallenges?.let { challenges ->
            val completed = DailyChallengeManager.applyEvent(challenges,
                ChallengeEvent.LevelCompleted(levelDuration, lostLifeThisLevel))
            publishChallengeProgress(completed)
        }
        logger?.logLevelComplete(levelIndex + 1, score, elapsedSeconds, 0)
        levelClearFlash = 1.0f
        emitVisualFeedback(GameEngine.VisualFeedbackEvent.LEVEL_CLEAR)
        spawnLevelCompleteConfetti()
        val summary = GameSummary(
            score = score,
            level = levelIndex + 1,
            durationSeconds = elapsedSeconds.toInt(),
            bricksBroken = runBricksBroken,
            livesLost = runLivesLost
        )

        // Keep one completion flow for all modes; GameActivity handles auto-advance behavior.
        awaitingNextLevel = true
        state = GameState.PAUSED
        stateBeforePause = GameState.PAUSED
        listener.onLevelComplete(summary)
    }
}

internal fun GameEngine.loseLife() {
    // Reset combo on life loss
    combo = 0
    comboTimer = 0f
    lostLifeThisLevel = true

    if (config.mode.relaxedMode) {
        spawnBall()
        state = GameState.READY
        syncAimForLaunch()
        return
    }
    lives -= 1
    runLivesLost += 1
    listener.onLivesUpdated(lives)
    audio.play(GameSound.LIFE, 0.9f)
    audio.haptic(GameHaptic.HEAVY)
    if (lives <= 0) {
        logger?.logGameOver(score, levelIndex + 1, "lives_depleted")
        triggerGameOver()
    } else {
        if (config.mode.invaders && invaderShieldMax > 0f) {
            invaderShield = invaderShieldMax
            invaderShieldAlerted = false
            listener.onShieldUpdated(invaderShield.toInt(), invaderShieldMax.toInt())
        }
        if (config.mode.invaders) {
            enemyShots.clear()
        }
        spawnBall()
        state = GameState.READY
        syncAimForLaunch()
    }
}
