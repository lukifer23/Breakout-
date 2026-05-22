package com.breakoutplus.game

import android.view.MotionEvent
import com.breakoutplus.game.LevelFactory.buildLevel
import kotlin.math.max

fun GameEngine.nextLevel() {
    val clearedBoard = bricks.none { it.alive && it.type != BrickType.UNBREAKABLE }
    val decision = LevelAdvancePolicy.evaluate(
        awaitingNextLevel = awaitingNextLevel,
        state = state,
        lives = lives,
        clearedBoard = clearedBoard,
        godModeEnabled = config.mode.relaxedMode
    )

    if (!decision.canAdvance) {
        logger?.logError(
            "nextLevel rejected (${decision.reason}, mode=${config.mode.name})"
        )
        return
    }
    if (state == GameState.GAME_OVER) {
        logger?.logError("nextLevel called in GAME_OVER state")
        return
    }
    if (lives <= 0) {
        logger?.logError("nextLevel called with 0 lives")
        return
    }

    logger?.logLevelAdvance(levelIndex + 1)
    levelIndex += 1
    awaitingNextLevel = false
    resetLevel(first = false)
}

internal fun GameEngine.resetLevel(first: Boolean) {
    state = GameState.READY
    stateBeforePause = GameState.READY
    awaitingNextLevel = false
    combo = 0
    lostLifeThisLevel = false
    if (first) {
        powerupDropsSinceLaser = 0
        powerupTipShown.clear()
        recentPowerups.clear()
        runBricksBroken = 0
        runLivesLost = 0
        pendingInitialLayoutRetune = true
    }
    powerupsSinceOffense = 0
    powerupsSinceDefense = 0
    powerupsSinceControl = 0
    guardrailActive = config.mode.relaxedMode
    shieldHitPulse = 0f
    shieldBreakPulse = 0f
    shieldCharges = 0
    fireballActive = false
    magnetActive = false
    gravityWellActive = false
    freezeActive = false
    pierceActive = false
    explosiveTipShown = false
    aimHasInput = false
    aimNormalized = 0f
    aimNormalizedTarget = 0f
    aimAngle = Math.PI.toFloat() * 0.5f
    isDragging = false
    activePointerId = MotionEvent.INVALID_POINTER_ID
    lastTouchLogTimeMs = 0L
    lastTouchLogX = Float.NaN
    lastTouchLogY = Float.NaN
    volleyTurnActive = false
    volleyQueuedBalls = 0
    volleyLaunchTimer = 0f
    volleyTurnCount = 0
    volleyAdvanceRows = 0
    volleyLaunchX = paddle.x
    volleyReturnAnchorX = Float.NaN
    volleyReturnSumX = 0f
    volleyReturnCount = 0
    volleyPreferredLaneCol = -1
    lastVolleySupplyTurn = -99
    volleyCompactionCheckTimer = 0f
    tunnelShotsFired = 0
    tunnelGateFlash = 0f
    lastTunnelSupplyShot = 0
    tunnelSupplyReadinessPercent = 0
    cachedTunnelGateIntegrityPercent = 100
    tunnelGateIntegrityDirty = true
    speedMultiplier = 1f
    levelClearFlash = 0f
    activeEffects.clear()
    sortedEffectsDirty = true
    aliveBreakableBrickCount = 0
    aliveExplosiveBrickCount = 0
    balls.clear()
    beams.clear()
    powerups.clear()
    enemyShots.clear()
    particles.clear()
    waves.clear()
    lastPowerupSnapshot = emptyList()
    lastComboReported = 0
    powerupStatusTick = 0f

    if (config.mode.godMode && !godModeTipShown) {
        listener.onTip("God mode: bottom shield is always active.")
        godModeTipShown = true
    }
    if (config.mode.zenMode && !zenModeTipShown) {
        listener.onTip("Zen mode: relax and flow — no scores, no pressure.")
        zenModeTipShown = true
    }
    if (config.mode == GameMode.VOLLEY) {
        volleyBallCount = if (first) {
            VolleyModeSystem.STARTING_BALL_COUNT
        } else {
            volleyBallCount.coerceIn(VolleyModeSystem.MIN_ACTIVE_BALL_COUNT, VolleyModeSystem.MAX_BALL_COUNT)
        }
        listener.onVolleyBallsUpdated(volleyBallCount)
    }

    applyLayoutTuning(currentAspectRatio, preserveRowBoost = false)

    val difficulty = difficultyForMode()
    val level = if (config.mode.invaders) {
        val invaderPacing = ModeBalance.invaderPacing()
        invaderDirection = if (levelIndex % 2 == 0) 1f else -1f
        invaderBaseSpeed = (invaderPacing.baseSpeed + levelIndex * invaderPacing.speedPerLevel)
            .coerceAtMost(invaderPacing.speedCap)
        invaderSpeed = invaderBaseSpeed
        invaderBaseShotCooldown = (invaderPacing.baseShotCooldown - levelIndex * invaderPacing.shotCooldownPerLevel)
            .coerceIn(invaderPacing.shotCooldownMin, invaderPacing.baseShotCooldown)
        invaderShotCooldown = invaderBaseShotCooldown
        invaderFormationOffset = 0f
        invaderRowPhase = random.nextFloat() * 6.28f
        invaderWaveStyle = levelIndex % 3
        invaderVolleyTimer = invaderBaseShotCooldown * (0.8f + random.nextFloat() * 0.6f)
        invaderPauseTimer = if (invaderWaveStyle == 2) invaderBaseShotCooldown * 1.2f else 0f
        invaderBurstCount = 0
        invaderShotTimer = invaderShotCooldown * (0.6f + random.nextFloat() * 0.8f)
        invaderTurnSoundCooldown = 0f
        invaderShieldMax = (invaderPacing.shieldBase + levelIndex * invaderPacing.shieldPerLevel)
            .coerceAtMost(invaderPacing.shieldCap)
        invaderShield = invaderShieldMax
        invaderShieldAlerted = false
        invaderShieldCritical = false
        invaderTelegraphKey = null
        listener.onShieldUpdated(invaderShield.toInt(), invaderShieldMax.toInt())
        LevelFactory.buildInvaderLevel(levelIndex, difficulty)
    } else {
        invaderShield = 0f
        invaderShieldMax = 0f
        invaderShieldAlerted = false
        invaderTurnSoundCooldown = 0f
        listener.onShieldUpdated(0, 0)
        val forcedTheme = ModeTheme.themeFor(
            mode = config.mode,
            levelIndex = levelIndex,
            availableThemeNames = themePool.asSequence().map { it.name }.toSet()
        )
        if (config.mode == GameMode.TUNNEL) {
            LevelFactory.buildTunnelLevel(
                index = levelIndex,
                difficulty = difficulty,
                theme = forcedTheme
            )
        } else {
            buildLevel(
                index = levelIndex,
                difficulty = difficulty,
                endless = config.mode.endless,
                themePool = themePool,
                forcedTheme = forcedTheme
            )
        }
    }
    currentLayout = level
    theme = level.theme
    buildBricks(level)
    buildSpatialHash()
    invaderTotal = if (config.mode.invaders) {
        max(1, invaderBricks.size)
    } else {
        0
    }

    if (config.mode.rush) {
        timeRemaining = config.mode.timeLimitSeconds.toFloat()
        lastReportedSecond = -1
        listener.onTimeUpdated(timeRemaining.toInt())
    }
    if (config.mode.timeLimitSeconds > 0 && first) {
        timeRemaining = config.mode.timeLimitSeconds.toFloat()
        lastReportedSecond = -1
        listener.onTimeUpdated(timeRemaining.toInt())
    }

    levelStartTime = elapsedSeconds
    spawnBall()
    paddle.targetX = paddle.x
    syncAimForLaunch()
    listener.onLevelUpdated(levelIndex + 1)
    when (config.mode) {
        GameMode.VOLLEY -> {
            updatePowerupStatus()
            listener.onTip("Volley mode: launch a chain, then survive the descending row.")
        }
        GameMode.TUNNEL,
        GameMode.SURVIVAL,
        GameMode.ZEN -> {
            updatePowerupStatus()
            listener.onTip(level.tip)
        }
        else -> {
            listener.onPowerupStatus("Powerups: none")
            lastPowerupStatus = "Powerups: none"
            listener.onTip(level.tip)
        }
    }
    logger?.logLevelStart(levelIndex + 1, theme.name)
}
