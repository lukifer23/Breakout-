package com.breakoutplus.game

import android.view.MotionEvent
import com.breakoutplus.DeviceLayoutPolicy
import com.breakoutplus.SettingsManager
import com.breakoutplus.UnlockManager
import com.breakoutplus.game.LevelFactory.buildLevel
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Core game engine handling physics, collisions, powerups, and level progression.
 * Manages game state, entities (balls, bricks, powerups), and renders via Renderer2D.
 * World coordinates: 100 units wide, height scales to aspect ratio.
 */
class GameEngine(
    internal val config: GameConfig,
    internal val listener: GameEventListener,
    internal val audio: GameAudioManager,
    internal val logger: GameLogger? = null,
    internal val dailyChallenges: MutableList<DailyChallenge>? = null,
    internal val renderer: GameRenderer? = null
) {
    internal val random = Random(System.nanoTime())
    internal var settings: SettingsManager.Settings = config.settings
    internal val balls = mutableListOf<Ball>()
    internal val bricks = mutableListOf<Brick>()
    internal val powerups = mutableListOf<PowerUp>()
    internal val beams = mutableListOf<Beam>()
    internal val enemyShots = mutableListOf<EnemyShot>()
    internal val particles = mutableListOf<Particle>()
    internal val waves = mutableListOf<ExplosionWave>()
    internal val activeEffects = mutableMapOf<PowerUpType, Float>()

    internal var worldWidth = 100f
    internal var worldHeight = 160f

    internal var paddle = Paddle(x = worldWidth / 2f, y = 8f, width = 22f, height = 2.6f)
    internal var basePaddleWidth = paddle.width
    internal var paddleVelocity = 0f

    internal var state = GameState.READY
    internal var stateBeforePause = state
    internal var score = 0
    internal var levelIndex = 0
    internal var lives = config.mode.baseLives
    internal var awaitingNextLevel = false
    internal var timeRemaining = config.mode.timeLimitSeconds.toFloat()
    internal var lastReportedSecond = -1
    internal var elapsedSeconds = 0f
    internal var levelStartTime = 0f
    internal var runBricksBroken = 0
    internal var runLivesLost = 0
    internal var combo = 0
    internal var comboTimer = 0f
    internal var guardrailActive = false
    internal var shieldCharges = 0
    internal var laserCooldown = 0f
    internal val laserCooldownDuration = 0.4f
    internal var speedMultiplier = 1f
    internal var timeWarpMultiplier = 1f
    internal var fireballActive = false
    internal var magnetActive = false
    internal var gravityWellActive = false
    internal var freezeActive = false
    internal var pierceActive = false
    internal var explosiveTipShown = false
    internal var lastPowerupStatus = ""
    internal var lastPowerupSnapshot: List<PowerupStatus> = emptyList()
    internal var lastComboReported = 0
    internal var powerupStatusTick = 0f
    internal var lostLifeThisLevel = false
    internal var laserTipShown = false
    internal var magnetTipShown = false
    internal var magnetCatchTipShown = false
    internal var godModeTipShown = false
    internal val powerupTipShown = mutableSetOf<PowerUpType>()
    internal var invaderDirection = 1f
    internal var invaderSpeed = 6f
    internal var invaderBaseSpeed = 6f
    internal var invaderShotTimer = 0f
    internal var invaderShotCooldown = 1.6f
    internal var invaderBaseShotCooldown = 1.6f
    internal val invaderTelegraphLead = 0.28f
    internal var invaderWaveStyle = 0
    internal var invaderVolleyTimer = 0f
    internal var invaderPauseTimer = 0f
    internal var invaderBurstCount = 0
    internal var invaderShield = 0f
    internal var invaderShieldMax = 0f
    internal var invaderShieldAlerted = false
    internal var invaderShieldCritical = false
    internal var invaderTelegraphKey: Long? = null
    internal var invaderTotal = 0
    internal val invaderBricks = mutableListOf<Brick>()
    internal var invaderFormationOffset = 0f
    internal var invaderRowPhase = 0f
    internal var invaderRowDrift = 0.75f
    internal var invaderRowPhaseOffset = 0.5f
    internal val invaderFormationCompression = 0.74f
    internal var invaderTurnSoundCooldown = 0f
    internal val invaderTurnSoundMinInterval = 0.32f
    internal var shieldHitPulse = 0f
    internal var shieldHitX = 0f
    internal var shieldHitColor = floatArrayOf(0.8f, 0.95f, 1f, 1f)
    internal val tunnelWallColor = floatArrayOf(0.76f, 0.83f, 0.93f, 1f)
    internal val tempColor = FloatArray(4)
    internal val scratchColor0 = FloatArray(4)
    internal val scratchColor1 = FloatArray(4)
    internal val scratchColor2 = FloatArray(4)
    internal val scratchColor3 = FloatArray(4)
    internal val scratchColor4 = FloatArray(4)
    internal val scratchColor5 = FloatArray(4)
    internal val scratchColor6 = FloatArray(4)
    internal val scratchColor7 = FloatArray(4)
    internal val scratchColor8 = FloatArray(4)
    internal val scratchColor9 = FloatArray(4)
    internal val scratchColor10 = FloatArray(4)
    internal val scratchColor11 = FloatArray(4)
    internal val aliveInvaderBuffer = ArrayList<Brick>(72)
    internal var shieldBreakPulse = 0f
    internal var powerupCollectionPulse = 0f
    internal var powerupDropsSinceLaser = 0
    internal val recentPowerups = ArrayDeque<PowerUpType>()
    internal val recentPowerupLimit = 4
    internal var powerupsSinceOffense = 0
    internal var powerupsSinceDefense = 0
    internal var powerupsSinceControl = 0
    internal var aimNormalized = 0f
    internal var aimNormalizedTarget = 0f
    internal var aimAngle = (Math.PI.toFloat() * 0.5f)
    internal var aimHasInput = false
    internal var isDragging = false
    internal var activePointerId = MotionEvent.INVALID_POINTER_ID
    internal var touchWorldX = 0f
    internal var touchWorldY = 0f
    internal var lastTouchLogTimeMs = 0L
    internal var lastTouchLogX = Float.NaN
    internal var lastTouchLogY = Float.NaN
    internal val touchMoveLogMinIntervalMs = 58L
    internal val touchMoveLogMinDistance = 0.9f
    internal val aimSmoothingRate = 18f
    internal val aimCenterDeadZone = 0.018f
    internal var debugAutoPlayEnabled = false
    internal var debugAutoPlayActionTimer = 0f
    internal var debugAutoPlayWave = 0f
    internal var debugProgressionProbeEnabled = false
    internal var debugProgressionProbeTimer = 0f
    internal var volleyBallCount = VolleyModeSystem.STARTING_BALL_COUNT
    internal var volleyQueuedBalls = 0
    internal var volleyLaunchTimer = 0f
    internal var volleyTurnActive = false
    internal var volleyTurnCount = 0
    internal var volleyAdvanceRows = 0
    internal var volleyLaunchX = worldWidth * 0.5f
    internal var volleyReturnAnchorX = Float.NaN
    internal var volleyReturnSumX = 0f
    internal var volleyReturnCount = 0
    internal var volleyPreferredLaneCol = -1
    internal var lastVolleySupplyTurn = -99
    internal var volleyCompactionCheckTimer = 0f

    internal var tunnelShotsFired = 0
    internal var tunnelGateFlash = 0f
    internal var lastTunnelSupplyShot = 0
    internal var tunnelSupplyReadinessPercent = 0
    internal var cachedTunnelGateIntegrityPercent = 100
    internal var tunnelGateIntegrityDirty = true

    // Spatial hash for brick collisions (packed key avoids per-frame Pair allocation).
    internal val spatialHashCellSize = 8f
    internal val spatialHash = mutableMapOf<Long, MutableList<Brick>>()
    internal val nearbyBrickBuffer = ArrayList<Brick>(96)
    internal val nearbyBrickSeen = HashSet<Brick>(96)
    internal var spatialHashDirty = true
    internal var dynamicBrickLayout = false
    internal var pendingInitialLayoutRetune = true
    internal var lastResizeWidthPx = 0
    internal var lastResizeHeightPx = 0

    internal var theme: LevelTheme = LevelThemes.DEFAULT
    internal var themePool: MutableList<LevelTheme> = LevelThemes.baseThemes().toMutableList()
    internal var currentLayout: LevelFactory.LevelLayout? = null
    internal var currentAspectRatio = worldHeight / worldWidth
    internal var brickAreaTopRatio = 0.92f
    internal var brickAreaBottomRatio = 0.52f
    internal var brickSpacing = 0.42f
    internal var layoutRowBoost = 0
    internal var layoutColBoost = 0
    internal var invaderScale = 1f
    internal var globalBrickScale = 0.9f
    internal var aliveBreakableBrickCount = 0
    internal var aliveExplosiveBrickCount = 0
    internal var levelClearFlash = 0f
    internal var renderTimeSeconds = 0f
    internal val hitFlashDecayRate = 2.0f
    internal val maxParticles = 240
    internal val maxWaves = 10
    internal var trailLife = 0.28f
    internal var maxTrailPoints = 8
    internal var cosmeticTier = config.unlocks.cosmeticTier
    internal var rewardScoreMultiplier = 0f
    internal var streakBonusRemaining = 0
    internal var streakBonusActive = false
    internal val streakBonusPerBrick = 20
    internal val aimMinAngle = 0.30f

    internal enum class VisualFeedbackEvent {
        VOLLEY_ROW_DROP,
        BOSS_BREAK,
        COMBO_STREAK,
        BEAM_BRICK_BREAK,
        INVADER_SHIELD_HIT,
        INVADER_SHIELD_BREAK,
        TUNNEL_PITY_SUPPLY,
        TUNNEL_GATE_BREACH,
        EXPLOSION_BREAK,
        INVADER_BURST,
        LEVEL_CLEAR
    }

    internal data class VisualFeedbackProfile(
        val shakeIntensity: Float = 0f,
        val shakeDuration: Float = 0f,
        val impactFlash: Float = 0f,
        val comboFlash: Boolean = false,
        val levelClearFlash: Boolean = false
    )

    internal fun visualFeedbackProfile(event: VisualFeedbackEvent): VisualFeedbackProfile {
        return when (event) {
            VisualFeedbackEvent.VOLLEY_ROW_DROP -> VisualFeedbackProfile(shakeIntensity = 0.7f, shakeDuration = 0.07f)
            VisualFeedbackEvent.BOSS_BREAK -> VisualFeedbackProfile(shakeIntensity = 3.2f, shakeDuration = 0.22f, impactFlash = 0.42f)
            VisualFeedbackEvent.COMBO_STREAK -> VisualFeedbackProfile(comboFlash = true)
            VisualFeedbackEvent.BEAM_BRICK_BREAK -> VisualFeedbackProfile(shakeIntensity = 1.7f, shakeDuration = 0.12f, impactFlash = 0.1f)
            VisualFeedbackEvent.INVADER_SHIELD_HIT -> VisualFeedbackProfile(shakeIntensity = 1.0f, shakeDuration = 0.08f)
            VisualFeedbackEvent.INVADER_SHIELD_BREAK -> VisualFeedbackProfile(shakeIntensity = 2.0f, shakeDuration = 0.18f, impactFlash = 0.26f)
            VisualFeedbackEvent.TUNNEL_PITY_SUPPLY -> VisualFeedbackProfile(shakeIntensity = 1.2f, shakeDuration = 0.08f, impactFlash = 0.18f)
            VisualFeedbackEvent.TUNNEL_GATE_BREACH -> VisualFeedbackProfile(shakeIntensity = 2.8f, shakeDuration = 0.20f, impactFlash = 0.35f)
            VisualFeedbackEvent.EXPLOSION_BREAK -> VisualFeedbackProfile(shakeIntensity = 2.4f, shakeDuration = 0.16f, impactFlash = 0.24f)
            VisualFeedbackEvent.INVADER_BURST -> VisualFeedbackProfile(shakeIntensity = 1.2f, shakeDuration = 0.12f, impactFlash = 0.08f)
            VisualFeedbackEvent.LEVEL_CLEAR -> VisualFeedbackProfile(shakeIntensity = 1.0f, shakeDuration = 0.11f, levelClearFlash = true)
        }
    }

    internal fun emitVisualFeedback(event: VisualFeedbackEvent, scale: Float = 1f) {
        val clampedScale = scale.coerceIn(0.5f, 1.6f)
        val profile = visualFeedbackProfile(event)
        if (profile.shakeIntensity > 0f && profile.shakeDuration > 0f) {
            renderer?.triggerScreenShake(profile.shakeIntensity * clampedScale, profile.shakeDuration * clampedScale)
        }
        if (profile.impactFlash > 0f) {
            renderer?.triggerImpactFlash(profile.impactFlash * clampedScale)
        }
        if (profile.comboFlash) {
            renderer?.triggerComboFlash()
        }
        if (profile.levelClearFlash) {
            renderer?.triggerLevelClearFlash()
        }
    }

    internal fun spatialKey(cellX: Int, cellY: Int): Long {
        return (cellX.toLong() shl 32) or (cellY.toLong() and 0xffffffffL)
    }

    internal data class VolleyBallStateCounts(
        val stuckBalls: Int,
        val inFlightBalls: Int,
        val stalledBalls: Int
    )

    internal fun isBreakable(type: BrickType): Boolean = type != BrickType.UNBREAKABLE

    internal fun recalcAliveBrickCounters() {
        var aliveBreakable = 0
        var aliveExplosive = 0
        for (brick in bricks) {
            if (!brick.alive) continue
            if (isBreakable(brick.type)) {
                aliveBreakable += 1
            }
            if (brick.type == BrickType.EXPLOSIVE) {
                aliveExplosive += 1
            }
        }
        aliveBreakableBrickCount = aliveBreakable
        aliveExplosiveBrickCount = aliveExplosive
    }

    internal fun countVolleyBallStates(): VolleyBallStateCounts {
        var stuck = 0
        var inFlight = 0
        var stalled = 0
        for (ball in balls) {
            if (ball.stuckToPaddle) {
                stuck += 1
            } else if (VolleyModeSystem.isBallInFlight(ball.vx, ball.vy)) {
                inFlight += 1
            } else {
                stalled += 1
            }
        }
        return VolleyBallStateCounts(
            stuckBalls = stuck,
            inFlightBalls = inFlight,
            stalledBalls = stalled
        )
    }

    internal fun nudgeStalledVolleyBalls() {
        if (config.mode != GameMode.VOLLEY) return
        var nudgedCount = 0
        val baseSpeed = (config.mode.launchSpeed * 0.9f).coerceAtLeast(18f)
        val targetX = paddle.targetX.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
        for (ball in balls) {
            if (ball.stuckToPaddle) continue
            if (VolleyModeSystem.isBallInFlight(ball.vx, ball.vy)) continue
            val horizontal = ((targetX - ball.x) / worldWidth).coerceIn(-0.65f, 0.65f)
            val vx = baseSpeed * horizontal
            val vyMagnitude = sqrt((baseSpeed * baseSpeed - vx * vx).coerceAtLeast(baseSpeed * baseSpeed * 0.36f))
            ball.vx = vx
            ball.vy = vyMagnitude
            nudgedCount += 1
        }
        if (nudgedCount > 0) {
            audio.play(GameSound.BOUNCE, 0.2f, 1.04f)
        }
    }

    internal fun hasStuckBall(): Boolean {
        for (ball in balls) {
            if (ball.stuckToPaddle) {
                return true
            }
        }
        return false
    }

    internal fun hasBreakthroughActiveEffect(): Boolean {
        return activeEffects.containsKey(PowerUpType.PIERCE) ||
            activeEffects.containsKey(PowerUpType.FIREBALL) ||
            activeEffects.containsKey(PowerUpType.LASER)
    }

    internal fun hasQueuedBreakthroughDrop(): Boolean {
        for (power in powerups) {
            if (power.type == PowerUpType.PIERCE ||
                power.type == PowerUpType.FIREBALL ||
                power.type == PowerUpType.LASER
            ) {
                return true
            }
        }
        return false
    }

    init {
        themePool = LevelThemes.baseThemes().toMutableList()
        themePool.addAll(LevelThemes.bonusThemes().filter { it.name in config.unlocks.unlockedThemes })
        cosmeticTier = config.unlocks.cosmeticTier
        applyCosmeticTier()
        logger?.logSessionStart(config.mode)
        listener.onModeUpdated(config.mode)
        resetLevel(first = true)
        listener.onLivesUpdated(lives)
        reportScore()
        listener.onLevelUpdated(levelIndex + 1)
        if (config.mode == GameMode.VOLLEY) {
            updatePowerupStatus()
        } else {
            listener.onPowerupStatus("Powerups: none")
            lastPowerupStatus = "Powerups: none"
        }
        if (config.mode.timeLimitSeconds > 0) {
            listener.onTimeUpdated(timeRemaining.toInt())
        } else {
            listener.onTimeUpdated(0)
        }
    }

    fun onResize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val significantLayoutShift = if (lastResizeWidthPx > 0 && lastResizeHeightPx > 0) {
            val widthDelta = kotlin.math.abs(width - lastResizeWidthPx) / lastResizeWidthPx.toFloat()
            val heightDelta = kotlin.math.abs(height - lastResizeHeightPx) / lastResizeHeightPx.toFloat()
            widthDelta >= 0.08f || heightDelta >= 0.08f
        } else {
            false
        }
        val pristineBoard =
            state == GameState.READY &&
                runBricksBroken == 0 &&
                balls.size <= 1 &&
                elapsedSeconds <= 2f
        val shouldRetuneStructure = pristineBoard && (pendingInitialLayoutRetune || significantLayoutShift)
        worldWidth = 100f
        worldHeight = worldWidth * (height.toFloat() / width.toFloat())
        paddle.y = 8f
        currentAspectRatio = worldHeight / worldWidth
        basePaddleWidth = resolveBasePaddleWidth(currentAspectRatio)
        paddle.width = basePaddleWidth
        paddle.height = 2.6f
        applyLayoutTuning(currentAspectRatio, preserveRowBoost = !shouldRetuneStructure)
        syncPaddleWidthFromEffects()
        paddle.targetX = paddle.x.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
        updateAimFromPaddle()
        updateAim(0f)
        // Retune structure when viewport meaningfully changes before a board is "in progress".
        if (shouldRetuneStructure) {
            currentLayout?.let {
                buildBricks(it)
                buildSpatialHash()
            }
            pendingInitialLayoutRetune = false
        } else {
            // Keep active gameplay geometry in sync across fold/unfold/rotation changes.
            relayoutBricks()
        }
        markTunnelGateIntegrityDirty()
        lastResizeWidthPx = width
        lastResizeHeightPx = height
    }

    internal fun resolveBasePaddleWidth(aspectRatio: Float): Float {
        val tallness = ((aspectRatio - 1.25f) / 0.85f).coerceIn(0f, 1f)
        val aspectBoost = lerp(1.1f, 1.02f, tallness)
        val modeBoost = when (config.mode) {
            GameMode.INVADERS -> 1.12f
            GameMode.GOD -> 1.08f
            GameMode.RUSH -> 1.06f
            GameMode.VOLLEY -> 1.1f
            GameMode.SURVIVAL -> 0.96f
            GameMode.TIMED -> 1.04f
            else -> 1.0f
        }
        // Slightly narrower baseline to support a more zoomed-out visual feel.
        val base = worldWidth * 0.21f
        return base * aspectBoost * modeBoost
    }

    fun update(delta: Float) {
        val dt = delta * speedMultiplier
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) {
            return
        }
        if (debugAutoPlayEnabled) {
            updateDebugAutoPlay(delta)
        }
        if (debugProgressionProbeEnabled) {
            updateDebugProgressionProbe(delta)
        }
        if (updateTimers(dt)) {
            return
        }
        updatePaddle(delta)
        updateAim(delta)
        updateBricks(dt)
        updateEffects(delta)

        if (state == GameState.READY) {
            attachBallToPaddle()
            // Show explosive brick tip if not shown yet and explosive bricks exist
            if (!explosiveTipShown && aliveExplosiveBrickCount > 0) {
                listener.onTip("Explosive bricks damage neighbors when destroyed.")
                explosiveTipShown = true
            }
        }

        if (state == GameState.RUNNING) {
            val ballDt = dt / timeWarpMultiplier  // Balls move at normal speed during TIME_WARP
            val worldDt = dt  // Other entities affected by TIME_WARP

            if (config.mode == GameMode.VOLLEY) {
                updateVolleyLaunchQueue(ballDt)
            }
            updateBalls(ballDt)
            updateBallTrails(ballDt)
            updateBeams(worldDt)
            updatePowerups(worldDt)
            updateInvaderShots(worldDt)
            updateParticles(worldDt)
            updateWaves(worldDt)
            if (state == GameState.RUNNING) {
                checkLevelCompletion()
            }
            if (config.mode == GameMode.VOLLEY && state == GameState.RUNNING) {
                resolveVolleyTurnIfReady()
            }
        }
        if (config.mode == GameMode.VOLLEY) {
            volleyCompactionCheckTimer -= dt
            if (volleyCompactionCheckTimer <= 0f) {
                volleyCompactionCheckTimer = 0.5f
                compactDeadVolleyBricksIfNeeded()
                updateVolleyDanger()
            }
        } else {
            volleyCompactionCheckTimer = 0f
        }
        val fastPulseDecay = dt * 2.5f
        val slowPulseDecay = dt * 1.2f
        
        if (shieldHitPulse > 0f) {
            shieldHitPulse = max(0f, shieldHitPulse - fastPulseDecay)
        }
        if (shieldBreakPulse > 0f) {
            shieldBreakPulse = max(0f, shieldBreakPulse - fastPulseDecay)
        }
        if (powerupCollectionPulse > 0f) {
            powerupCollectionPulse = max(0f, powerupCollectionPulse - fastPulseDecay)
        }
        if (tunnelGateFlash > 0f) {
            tunnelGateFlash = max(0f, tunnelGateFlash - slowPulseDecay) // prolonged flash
        }
    }

    fun setDebugAutoPlay(enabled: Boolean) {
        debugAutoPlayEnabled = enabled
        if (!enabled) {
            debugAutoPlayActionTimer = 0f
            return
        }
        debugAutoPlayActionTimer = 0f
        debugAutoPlayWave = 0f
    }

    fun setDebugProgressionProbe(enabled: Boolean) {
        debugProgressionProbeEnabled = enabled
        debugProgressionProbeTimer = 0f
    }

    internal fun updateDebugAutoPlay(dt: Float) {
        if (dt <= 0f) return
        debugAutoPlayActionTimer -= dt
        val minX = paddle.width / 2f
        val maxX = worldWidth - paddle.width / 2f

        when (state) {
            GameState.READY -> {
                if (config.mode == GameMode.VOLLEY) {
                    debugAutoPlayWave += dt
                    val swing = kotlin.math.sin(debugAutoPlayWave * 1.3f) * worldWidth * 0.22f
                    paddle.targetX = (worldWidth * 0.5f + swing).coerceIn(minX, maxX)
                } else {
                    val anchorX = balls.firstOrNull()?.x ?: worldWidth * 0.5f
                    paddle.targetX = anchorX.coerceIn(minX, maxX)
                }
                if (debugAutoPlayActionTimer <= 0f) {
                    launchBall()
                    debugAutoPlayActionTimer = if (config.mode == GameMode.VOLLEY) 0.34f else 0.22f
                }
            }
            GameState.RUNNING -> {
                if (config.mode != GameMode.VOLLEY) {
                    val incoming = balls.minByOrNull { it.y }
                    if (incoming != null) {
                        val lead = (incoming.vx * 0.17f).coerceIn(-8f, 8f)
                        paddle.targetX = (incoming.x + lead).coerceIn(minX, maxX)
                    }
                }
                if (magnetActive && hasStuckBall() && debugAutoPlayActionTimer <= 0f) {
                    releaseStuckBalls()
                    debugAutoPlayActionTimer = 0.24f
                }
                if (activeEffects.containsKey(PowerUpType.LASER) && laserCooldown <= 0f && debugAutoPlayActionTimer <= 0f) {
                    shootLaser()
                    debugAutoPlayActionTimer = 0.16f
                }
            }
            else -> Unit
        }
    }

    internal fun updateDebugProgressionProbe(dt: Float) {
        if (dt <= 0f) return
        if (state != GameState.RUNNING || awaitingNextLevel) return
        debugProgressionProbeTimer -= dt
        if (debugProgressionProbeTimer > 0f) return

        val target = bricks.firstOrNull { it.alive && it.type != BrickType.UNBREAKABLE } ?: return
        val probeBeam = Beam(
            x = target.centerX,
            y = target.centerY,
            width = target.width.coerceAtLeast(0.8f),
            height = target.height.coerceAtLeast(0.8f),
            speed = 0f,
            color = PowerUpType.LASER.color
        )
        handleBrickCollisionFromBeam(probeBeam, target)
        debugProgressionProbeTimer = 0.03f
    }

    

    

    

    internal data class AimHit(val t: Float, val nx: Float, val ny: Float, val hitsBrick: Boolean = false)

    

    

    internal fun invaderKey(brick: Brick): Long {
        return (brick.gridX.toLong() shl 32) or (brick.gridY.toLong() and 0xffffffffL)
    }

    

    

    

    internal fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t.coerceIn(0f, 1f)
    }

    fun getObjectCount(): Int = balls.size + bricks.size + powerups.size + beams.size + enemyShots.size + particles.size + waves.size

    fun isGameRunning(): Boolean = state == GameState.RUNNING

    internal fun normalizedAspectRatio(aspectRatio: Float = currentAspectRatio): Float {
        return DeviceLayoutPolicy.normalizedAspectRatio(aspectRatio)
    }

    internal fun isSlateAspect(aspectRatio: Float = currentAspectRatio): Boolean {
        return DeviceLayoutPolicy.isSlateAspect(aspectRatio)
    }

    internal fun currentVolleyMetrics(
        laneWindowRatio: Float = 0.2f
    ): ModeBoardMetrics.VolleyBoardMetrics {
        return ModeBoardMetrics.volleyMetrics(
            bricks = bricks,
            paddleY = paddle.y,
            paddleHeight = paddle.height,
            worldHeight = worldHeight,
            laneWindowRatio = laneWindowRatio
        )
    }

    internal fun applyLayoutTuning(aspectRatio: Float, preserveRowBoost: Boolean) {
        val normalizedAspect = normalizedAspectRatio(aspectRatio)
        val tallness = ((normalizedAspect - 1.25f) / 0.85f).coerceIn(0f, 1f)
        val isSlate = isSlateAspect(normalizedAspect)

        // Shared baseline, with specific adjustments for slate/tablet devices to prevent cramping.
        brickAreaTopRatio = if (isSlate) 0.96f else lerp(0.992f, 0.978f, tallness)
        brickAreaBottomRatio = if (isSlate) 0.48f else lerp(0.69f, 0.62f, tallness)
        brickSpacing = if (isSlate) 0.22f else lerp(0.31f, 0.37f, tallness)

        if (!preserveRowBoost) {
            // Adjust row boost to ensure density on taller screens, but relax it for slates.
            val densityBoost = (levelIndex / 6).coerceAtMost(2)
            val baseRowBoost = if (isSlate) 18 else if (normalizedAspect > 2.05f) 4 else if (normalizedAspect > 1.85f) 2 else 0
            val baseColBoost = if (isSlate) 4 else 0

            when (config.mode) {
                GameMode.RUSH -> {
                   layoutRowBoost = (baseRowBoost - 2).coerceAtLeast(0)
                   layoutColBoost = (baseColBoost - 2).coerceAtLeast(0)
                }
                GameMode.GOD -> {
                    layoutRowBoost = baseRowBoost + 2 + densityBoost
                    layoutColBoost = baseColBoost
                }
                GameMode.VOLLEY -> {
                    val volleyRows = ModeLayoutPolicy.volleyRowBoost(
                        aspectRatio = normalizedAspect,
                        isSlate = isSlate,
                        levelIndex = levelIndex
                    )
                    layoutRowBoost = volleyRows
                    // Volley width is fixed
                    layoutColBoost = 0
                }
                GameMode.TUNNEL -> {
                     layoutRowBoost = ModeLayoutPolicy.tunnelRowBoost(
                         aspectRatio = normalizedAspect,
                         isSlate = isSlate,
                         levelIndex = levelIndex
                     )
                     layoutColBoost = baseColBoost
                }
                GameMode.SURVIVAL -> {
                    layoutRowBoost = baseRowBoost + 1 + densityBoost
                    layoutColBoost = baseColBoost
                }
                else -> {
                    layoutRowBoost = baseRowBoost + densityBoost
                    layoutColBoost = baseColBoost
                }
            }
        }
        
        // Slightly reduce global scale for slate to fit more content comfortably.
        globalBrickScale = if (isSlate) 0.92f else lerp(1f, 0.9f, tallness)
        
        if (config.mode.invaders) {
             brickAreaBottomRatio = (brickAreaBottomRatio + 0.05f).coerceAtMost(0.79f)
             brickSpacing *= 0.95f
             invaderScale = lerp(0.5f, 0.47f, tallness)
             invaderRowDrift = lerp(0.8f, 0.65f, tallness)
             invaderRowPhaseOffset = lerp(0.45f, 0.6f, tallness)
             if (!preserveRowBoost) {
                 layoutRowBoost = 0
                 layoutColBoost = 0
             }
        } else {
             invaderScale = 1f
        }
    }

    internal fun clampPaddleX(worldX: Float): Float {
        return worldX.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
    }

    internal fun syncStuckBallsToPaddle() {
        balls.forEach { ball ->
            if (!ball.stuckToPaddle) return@forEach
            val minX = paddle.x - paddle.width / 2f + ball.radius
            val maxX = paddle.x + paddle.width / 2f - ball.radius
            ball.x = (paddle.x + ball.stickOffset).coerceIn(minX, maxX)
            ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
            ball.vx = 0f
            ball.vy = 0f
        }
    }

    internal fun updatePaddleFromTouch(worldX: Float, snapImmediately: Boolean) {
        val clamped = clampPaddleX(worldX)
        paddle.targetX = clamped
        if (!snapImmediately) return
        paddle.x = clamped
        if (state == GameState.READY) {
            attachBallToPaddle()
        }
        syncStuckBallsToPaddle()
    }

    internal fun shouldSnapTouchToPaddle(): Boolean {
        return state == GameState.READY || hasStuckBall()
    }

    internal fun shouldLogTouch(actionMasked: Int, x: Float, y: Float, eventTimeMs: Long): Boolean {
        if (actionMasked != MotionEvent.ACTION_MOVE) {
            lastTouchLogTimeMs = eventTimeMs
            lastTouchLogX = x
            lastTouchLogY = y
            return true
        }
        val elapsedMs = eventTimeMs - lastTouchLogTimeMs
        val movedDistance = if (lastTouchLogX.isFinite() && lastTouchLogY.isFinite()) {
            kotlin.math.sqrt(
                (x - lastTouchLogX) * (x - lastTouchLogX) +
                    (y - lastTouchLogY) * (y - lastTouchLogY)
            )
        } else {
            Float.POSITIVE_INFINITY
        }
        val shouldLog = elapsedMs >= touchMoveLogMinIntervalMs || movedDistance >= touchMoveLogMinDistance
        if (shouldLog) {
            lastTouchLogTimeMs = eventTimeMs
            lastTouchLogX = x
            lastTouchLogY = y
        }
        return shouldLog
    }

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return

        val clampWorldX = { screenX: Float ->
            clampPaddleX(screenX / viewWidth * worldWidth)
        }
        val clampWorldY = { screenY: Float ->
            worldHeight - (screenY / viewHeight * worldHeight)
        }
        val pointerIndexForId = { pointerId: Int ->
            if (pointerId == MotionEvent.INVALID_POINTER_ID) -1 else event.findPointerIndex(pointerId)
        }
        val pointerWorldX = { pointerId: Int ->
            val idx = pointerIndexForId(pointerId)
            if (idx in 0 until event.pointerCount) clampWorldX(event.getX(idx)) else null
        }
        val pointerWorldY = { pointerId: Int ->
            val idx = pointerIndexForId(pointerId)
            if (idx in 0 until event.pointerCount) clampWorldY(event.getY(idx)) else null
        }

        if (viewWidth <= 0f || viewHeight <= 0f || event.pointerCount <= 0) return

        val actionIndex = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val actionPointerId = event.getPointerId(actionIndex)
        val trackedPointerId = if (activePointerId != MotionEvent.INVALID_POINTER_ID) activePointerId else actionPointerId
        val trackedX = pointerWorldX(trackedPointerId) ?: clampWorldX(event.getX(actionIndex))
        val trackedY = pointerWorldY(trackedPointerId) ?: clampWorldY(event.getY(actionIndex))
        val trackedPointerIndex = pointerIndexForId(trackedPointerId).coerceIn(0, event.pointerCount - 1)
        val trackedPressure = event.getPressure(trackedPointerIndex)

        // Log touch input
        val actionString = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "down"
            MotionEvent.ACTION_MOVE -> "move"
            MotionEvent.ACTION_POINTER_DOWN -> "pointer_down"
            MotionEvent.ACTION_POINTER_UP -> "pointer_up"
            MotionEvent.ACTION_UP -> "up"
            MotionEvent.ACTION_CANCEL -> "cancel"
            else -> "other"
        }
        if (logger != null && shouldLogTouch(event.actionMasked, trackedX, trackedY, event.eventTime)) {
            logger.logTouchInput(actionString, trackedX, trackedY, trackedPressure)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = actionPointerId
                val downX = pointerWorldX(activePointerId) ?: trackedX
                val downY = pointerWorldY(activePointerId) ?: trackedY
                touchWorldX = downX
                touchWorldY = downY
                val snapToTouch = shouldSnapTouchToPaddle()
                updatePaddleFromTouch(
                    downX,
                    snapImmediately = snapToTouch
                )
                isDragging = true
                updateAimFromTouch()
                aimNormalized = aimNormalizedTarget
                applyAimFromNormalized(aimNormalized)
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    activePointerId = actionPointerId
                }
                val moveX = pointerWorldX(activePointerId) ?: trackedX
                val moveY = pointerWorldY(activePointerId) ?: trackedY
                touchWorldX = moveX
                touchWorldY = moveY
                val snapToTouch = shouldSnapTouchToPaddle()
                updatePaddleFromTouch(
                    moveX,
                    snapImmediately = snapToTouch
                )
                isDragging = true
                updateAimFromTouch()
                aimNormalized = aimNormalizedTarget
                applyAimFromNormalized(aimNormalized)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val liftedPointerId = actionPointerId
                if (liftedPointerId == activePointerId) {
                    var replacementIndex = -1
                    for (i in 0 until event.pointerCount) {
                        if (i == actionIndex) continue
                        replacementIndex = i
                        break
                    }
                    if (replacementIndex >= 0) {
                        activePointerId = event.getPointerId(replacementIndex)
                        touchWorldX = clampWorldX(event.getX(replacementIndex))
                        touchWorldY = clampWorldY(event.getY(replacementIndex))
                        val snapToTouch = shouldSnapTouchToPaddle()
                        updatePaddleFromTouch(
                            touchWorldX,
                            snapImmediately = snapToTouch
                        )
                        isDragging = true
                        updateAimFromTouch()
                        aimNormalized = aimNormalizedTarget
                        applyAimFromNormalized(aimNormalized)
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        isDragging = false
                        updateAimFromPaddle()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val upX = pointerWorldX(actionPointerId) ?: trackedX
                val upY = pointerWorldY(actionPointerId) ?: trackedY
                touchWorldX = upX
                touchWorldY = upY
                updatePaddleFromTouch(upX, snapImmediately = true)
                syncAimForLaunch()
                if (state == GameState.READY) {
                    // Launch on tap/release for intuitive starts.
                    launchBall()
                    if (config.mode == GameMode.VOLLEY) {
                        listener.onTip("Volley launched. Bricks will descend when all balls return.")
                    } else {
                        listener.onTip("Tap with two fingers to fire when laser is active")
                    }
                } else if (magnetActive && hasStuckBall()) {
                    releaseStuckBalls()
                }
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                updateAimFromPaddle()
            }
        }
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN &&
            activeEffects.containsKey(PowerUpType.LASER)
        ) {
            shootLaser()
        }
    }

    fun triggerLaserFromUi() {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return
        if (activeEffects.containsKey(PowerUpType.LASER)) {
            shootLaser()
        }
    }

    fun pause() {
        if (state != GameState.PAUSED) {
            stateBeforePause = state
        }
        state = GameState.PAUSED
    }

    fun resume() {
        if (state == GameState.PAUSED) {
            // Restore the exact gameplay state if known; otherwise remain paused.
            state = when (stateBeforePause) {
                GameState.READY, GameState.RUNNING, GameState.GAME_OVER -> stateBeforePause
                else -> GameState.PAUSED
            }
        }
    }

    fun nextLevel() {
        val clearedBoard = bricks.none { it.alive && it.type != BrickType.UNBREAKABLE }
        val decision = LevelAdvancePolicy.evaluate(
            awaitingNextLevel = awaitingNextLevel,
            state = state,
            lives = lives,
            clearedBoard = clearedBoard,
            godModeEnabled = config.mode.godMode
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

    internal fun resetLevel(first: Boolean) {
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
        guardrailActive = config.mode.godMode
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

    internal fun getNearbyBricks(ball: Ball): List<Brick> {
        nearbyBrickBuffer.clear()
        nearbyBrickSeen.clear()
        val ballMinX = ((ball.x - ball.radius) / spatialHashCellSize).toInt()
        val ballMaxX = ((ball.x + ball.radius) / spatialHashCellSize).toInt()
        val ballMinY = ((ball.y - ball.radius) / spatialHashCellSize).toInt()
        val ballMaxY = ((ball.y + ball.radius) / spatialHashCellSize).toInt()

        for (cellX in ballMinX..ballMaxX) {
            for (cellY in ballMinY..ballMaxY) {
                val cellKey = spatialKey(cellX, cellY)
                spatialHash[cellKey]?.forEach { brick ->
                    if (nearbyBrickSeen.add(brick)) {
                        nearbyBrickBuffer.add(brick)
                    }
                }
            }
        }
        if (nearbyBrickBuffer.isEmpty() && bricks.isNotEmpty() && (spatialHashDirty || spatialHash.isEmpty())) {
            // Safety fallback: avoid collision loss if hash is temporarily stale.
            // Return a snapshot so collision handlers can mutate `bricks` safely.
            return ArrayList(bricks)
        }
        return nearbyBrickBuffer
    }

    internal fun buildBricks(layout: LevelFactory.LevelLayout) {
        bricks.clear()
        val rows = layout.rows + layoutRowBoost
        val requestedCols = if (config.mode == GameMode.VOLLEY) layout.cols else layout.cols + layoutColBoost
        val cols = if (config.mode == GameMode.VOLLEY) effectiveVolleyColumns(requestedCols) else requestedCols
        val spacing = brickSpacing
        val areaTop = worldHeight * brickAreaTopRatio
        val areaBottom = worldHeight * brickAreaBottomRatio
        val areaHeight = areaTop - areaBottom
        val baseBrickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val baseBrickWidth = (worldWidth - spacing * (cols + 1)) / cols
        val sizeScale = (if (config.mode.invaders) invaderScale else 1f) * globalBrickScale
        val brickHeight = baseBrickHeight * sizeScale
        val brickWidth = baseBrickWidth * sizeScale
        val colOffset = if (config.mode == GameMode.VOLLEY) 0 else layoutColBoost / 2
        val occupied = HashSet<Long>(layout.bricks.size * 2)
        fun key(col: Int, row: Int): Long = (col.toLong() shl 32) or (row.toLong() and 0xffffffffL)
        layout.bricks.forEach { spec ->
            val sourceCol = spec.col + colOffset
            val gridX = if (config.mode == GameMode.VOLLEY) {
                remapVolleyColumn(spec.col, layout.cols, cols)
            } else {
                sourceCol
            }
            if (!occupied.add(key(gridX, spec.row))) return@forEach
            val cellX = spacing + gridX * (baseBrickWidth + spacing)
            val visualRow = if (config.mode == GameMode.VOLLEY) spec.row + volleyAdvanceRows else spec.row
            val cellY = areaBottom + (rows - 1 - visualRow) * (baseBrickHeight + spacing * 0.5f)
            val rawX = cellX + (baseBrickWidth - brickWidth) * 0.5f
            val x = if (config.mode.invaders) compressInvaderX(rawX, brickWidth) else rawX
            val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
            val (resolvedType, resolvedHitPoints) = resolveBrickSpecForMode(spec.type, spec.hitPoints)
            val brick = Brick(
                gridX = gridX,
                gridY = spec.row,
                x = x,
                y = y,
                width = brickWidth,
                height = brickHeight,
                hitPoints = resolvedHitPoints,
                maxHitPoints = resolvedHitPoints,
                type = resolvedType
            )
            brick.baseX = x
            brick.baseY = y

            // Set up special properties for dynamic bricks
            when (resolvedType) {
                BrickType.MOVING -> {
                    // Velocity will be initialized in updateBricks
                }
                BrickType.PHASE -> {
                    brick.maxPhase = 2 + (levelIndex / 3).coerceAtMost(2) // 2-4 phases
                    brick.phase = 0
                }
                BrickType.BOSS -> {
                    brick.maxPhase = 3 + (levelIndex / 5).coerceAtMost(2) // 3-5 phases
                    brick.phase = 0
                }
                BrickType.SPAWNING -> {
                    brick.spawnCount = 2 + kotlin.random.Random(spec.col * 7 + spec.row * 11).nextInt(2)
                }
                else -> {}
            }

            bricks.add(brick)
        }

        if (layoutRowBoost > 0 && !config.mode.invaders) {
            val difficulty = 1f + levelIndex * 0.08f
            val baseRow = layout.rows
            val boostCols = if (config.mode == GameMode.VOLLEY) cols else layout.cols
            repeat(layoutRowBoost) { offset ->
                val rowIndex = baseRow + offset
                for (col in 0 until boostCols) {
                    val sourceCol = if (config.mode == GameMode.VOLLEY) {
                        (((col.toFloat() + 0.5f) / boostCols.toFloat()) * layout.cols.toFloat())
                            .toInt()
                            .coerceIn(0, layout.cols - 1)
                    } else {
                        col
                    }
                    val gridX = if (config.mode == GameMode.VOLLEY) col else col + colOffset
                    if (occupied.contains(key(gridX, rowIndex))) continue
                    val seed = (levelIndex + 1) * 97 + rowIndex * 13 + sourceCol * 7
                    val roll = Random(seed).nextFloat()
                    val type = when {
                        roll > 0.9f -> BrickType.REINFORCED
                        roll < 0.03f -> BrickType.ARMORED
                        else -> BrickType.NORMAL
                    }
                    val baseHp = baseHitPoints(type)
                    val hp = if (type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * difficulty).roundToInt())
                    val cellX = spacing + gridX * (baseBrickWidth + spacing)
                    val visualRow = if (config.mode == GameMode.VOLLEY) rowIndex + volleyAdvanceRows else rowIndex
                    val cellY = areaBottom + (rows - 1 - visualRow) * (baseBrickHeight + spacing * 0.5f)
                    val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
                    val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
                    val gridY = rowIndex
                    bricks.add(
                        Brick(
                            gridX = gridX,
                            gridY = gridY,
                            x = x,
                            y = y,
                            width = brickWidth,
                            height = brickHeight,
                            hitPoints = hp,
                            maxHitPoints = hp,
                            type = type
                        )
                    )
                    bricks.last().apply {
                        baseX = x
                        baseY = y
                    }
                    occupied.add(key(gridX, gridY))
                }
            }
        }

        if (layoutColBoost > 0 && !config.mode.invaders && config.mode != GameMode.VOLLEY) {
            val difficulty = 1f + levelIndex * 0.08f
            val leftCols = colOffset
            val rightStart = colOffset + layout.cols
            for (col in 0 until cols) {
                val isExtraCol = col < leftCols || col >= rightStart
                if (!isExtraCol) continue
                for (row in 0 until rows) {
                    if (occupied.contains(key(col, row))) continue
                    val rowRatio = if (rows > 1) row.toFloat() / (rows - 1).toFloat() else 0f
                    val density = 0.62f - rowRatio * 0.22f
                    val seed = (levelIndex + 3) * 131 + row * 17 + col * 29
                    val roll = Random(seed).nextFloat()
                    if (roll > density) continue
                    val typeRoll = Random(seed + 7).nextFloat()
                    val type = when {
                        typeRoll > 0.9f -> BrickType.REINFORCED
                        typeRoll < 0.04f -> BrickType.ARMORED
                        else -> BrickType.NORMAL
                    }
                    val baseHp = baseHitPoints(type)
                    val hp = if (type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * difficulty).roundToInt())
                    val cellX = spacing + col * (baseBrickWidth + spacing)
                    val visualRow = if (config.mode == GameMode.VOLLEY) row + volleyAdvanceRows else row
                    val cellY = areaBottom + (rows - 1 - visualRow) * (baseBrickHeight + spacing * 0.5f)
                    val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
                    val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
                    bricks.add(
                        Brick(
                            gridX = col,
                            gridY = row,
                            x = x,
                            y = y,
                            width = brickWidth,
                            height = brickHeight,
                            hitPoints = hp,
                            maxHitPoints = hp,
                            type = type
                        )
                    )
                    bricks.last().apply {
                        baseX = x
                        baseY = y
                    }
                    occupied.add(key(col, row))
                }
            }
        }

        applyRowBoostTopPadding(baseBrickHeight)

        invaderBricks.clear()
        if (config.mode.invaders) {
            invaderBricks.addAll(bricks.filter { it.type == BrickType.INVADER })
        }
        recalcAliveBrickCounters()
        dynamicBrickLayout = config.mode.invaders || bricks.any { it.type == BrickType.MOVING }
        markTunnelGateIntegrityDirty()
        buildSpatialHash()
    }

    internal fun resolveBrickSpecForMode(type: BrickType, hitPoints: Int): Pair<BrickType, Int> {
        if (config.mode != GameMode.VOLLEY || type != BrickType.UNBREAKABLE) {
            return type to hitPoints
        }
        // Volley must remain fully breakable: convert unbreakables into durable breakable bricks.
        val fallbackType = if (levelIndex >= 4) BrickType.ARMORED else BrickType.REINFORCED
        val baseHp = baseHitPoints(fallbackType)
        val scaledHp = (baseHp * (1f + levelIndex * 0.06f)).roundToInt().coerceIn(baseHp, 9)
        return fallbackType to scaledHp
    }

    internal fun effectiveVolleyColumns(requestedCols: Int): Int {
        val clampedRequested = requestedCols.coerceAtLeast(6)
        val target = when {
            currentAspectRatio <= 1.34f -> 12
            currentAspectRatio <= 1.55f -> 11
            currentAspectRatio <= 1.85f -> 10
            else -> 9
        }
        return min(clampedRequested, target)
    }

    internal fun applyRowBoostTopPadding(baseBrickHeight: Float) {
        if (layoutRowBoost <= 0 || config.mode.invaders || config.mode == GameMode.VOLLEY) return
        val topPad = baseBrickHeight * 0.15f
        bricks.forEach { brick ->
            brick.y += topPad
            brick.baseY = brick.y
        }
    }

    internal fun remapVolleyColumn(col: Int, originalCols: Int, targetCols: Int): Int {
        if (targetCols >= originalCols) return col.coerceIn(0, targetCols - 1)
        val ratio = (col.toFloat() + 0.5f) / originalCols.toFloat().coerceAtLeast(1f)
        return (ratio * targetCols).toInt().coerceIn(0, targetCols - 1)
    }

    internal fun relayoutBricks() {
        val layout = currentLayout ?: return
        if (bricks.isEmpty()) return
        val rows = layout.rows + layoutRowBoost
        val requestedCols = if (config.mode == GameMode.VOLLEY) layout.cols else layout.cols + layoutColBoost
        val cols = if (config.mode == GameMode.VOLLEY) effectiveVolleyColumns(requestedCols) else requestedCols
        val spacing = brickSpacing
        val areaTop = worldHeight * brickAreaTopRatio
        val areaBottom = worldHeight * brickAreaBottomRatio
        val areaHeight = areaTop - areaBottom
        val baseBrickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val baseBrickWidth = (worldWidth - spacing * (cols + 1)) / cols
        val sizeScale = (if (config.mode.invaders) invaderScale else 1f) * globalBrickScale
        val brickHeight = baseBrickHeight * sizeScale
        val brickWidth = baseBrickWidth * sizeScale
        bricks.forEach { brick ->
            val visualRow = if (config.mode == GameMode.VOLLEY) brick.gridY + volleyAdvanceRows else brick.gridY
            if (brick.gridX < 0 || visualRow < 0) return@forEach
            val cellX = spacing + brick.gridX * (baseBrickWidth + spacing)
            val cellY = areaBottom + (rows - 1 - visualRow) * (baseBrickHeight + spacing * 0.5f)
            val rawX = cellX + (baseBrickWidth - brickWidth) * 0.5f
            val x = if (config.mode.invaders) compressInvaderX(rawX, brickWidth) else rawX
            val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
            brick.x = x
            brick.y = y
            brick.width = brickWidth
            brick.height = brickHeight
            brick.baseX = x
            brick.baseY = y
        }
        applyRowBoostTopPadding(baseBrickHeight)
        buildSpatialHash()
    }

    internal fun baseHitPoints(type: BrickType): Int {
        return when (type) {
            BrickType.NORMAL -> 1
            BrickType.REINFORCED -> 2
            BrickType.ARMORED -> 3
            BrickType.EXPLOSIVE -> 1
            BrickType.UNBREAKABLE -> 999
            BrickType.MOVING -> 2
            BrickType.SPAWNING -> 2
            BrickType.PHASE -> 3
            BrickType.BOSS -> 6
            BrickType.INVADER -> 1
        }
    }

    internal fun updateTimers(dt: Float): Boolean {
        if (state != GameState.RUNNING) return false
        elapsedSeconds += dt

        // Update combo timer
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                combo = 0  // Reset combo when timer expires
            }
        }

        if (config.mode.timeLimitSeconds > 0) {
            timeRemaining -= dt
            if (timeRemaining <= 0f) {
                triggerGameOver()
                return true
            } else {
                val currentSecond = timeRemaining.toInt()
                if (currentSecond != lastReportedSecond) {
                    lastReportedSecond = currentSecond
                    listener.onTimeUpdated(currentSecond)
                }
            }
        } else {
            val currentSecond = elapsedSeconds.toInt()
            if (currentSecond != lastReportedSecond) {
                lastReportedSecond = currentSecond
                listener.onTimeUpdated(currentSecond)
            }
        }
        return state == GameState.GAME_OVER
    }

    fun updateSettings(newSettings: SettingsManager.Settings) {
        settings = newSettings
    }

    fun updateUnlocks(unlocks: UnlockManager.UnlockState) {
        themePool = LevelThemes.baseThemes().toMutableList()
        themePool.addAll(LevelThemes.bonusThemes().filter { it.name in unlocks.unlockedThemes })
        cosmeticTier = unlocks.cosmeticTier
        applyCosmeticTier()
    }

    fun currentSummary(): GameSummary {
        return GameSummary(
            score = score,
            level = levelIndex + 1,
            durationSeconds = elapsedSeconds.toInt().coerceAtLeast(0),
            bricksBroken = runBricksBroken,
            livesLost = runLivesLost
        )
    }

    internal fun applyCosmeticTier() {
        maxTrailPoints = 8 + cosmeticTier * 2
        trailLife = 0.28f + cosmeticTier * 0.04f
    }

    internal fun updateDailyChallenges(type: ChallengeType, value: Int = 1) {
        val challenges = dailyChallenges ?: return
        val newlyCompleted = DailyChallengeManager.updateChallengeProgress(challenges, type, value)
        if (newlyCompleted.isNotEmpty()) {
            handleChallengeRewards(newlyCompleted)
        }
    }

    internal fun handleChallengeRewards(completed: List<DailyChallenge>) {
        completed.forEach { challenge ->
            when (challenge.rewardType) {
                RewardType.SCORE_MULTIPLIER -> {
                    val bonus = (challenge.rewardValue / 100f).coerceAtLeast(0.01f)
                    rewardScoreMultiplier += bonus
                    listener.onTip("Challenge reward: +${(bonus * 100).toInt()}% score boost")
                }
                RewardType.STREAK_BONUS -> {
                    streakBonusRemaining += challenge.rewardValue.coerceAtLeast(1)
                    streakBonusActive = true
                    listener.onTip("Challenge reward: streak bonus x${challenge.rewardValue}")
                }
                RewardType.COSMETIC_UNLOCK -> {
                    if (cosmeticTier < 3) {
                        cosmeticTier = (cosmeticTier + 1).coerceAtMost(3)
                        applyCosmeticTier()
                        listener.onCosmeticUnlocked(cosmeticTier)
                        listener.onTip("Challenge reward: cosmetic upgrade")
                    } else {
                        rewardScoreMultiplier += 0.05f
                        listener.onTip("All cosmetics unlocked: +5% score boost")
                    }
                }
                RewardType.THEME_UNLOCK -> {
                    val locked = LevelThemes.bonusThemes().filter { bonus ->
                        themePool.none { it.name == bonus.name }
                    }
                    if (locked.isNotEmpty()) {
                        val picked = locked[random.nextInt(locked.size)]
                        themePool.add(picked)
                        listener.onThemeUnlocked(picked.name)
                        listener.onTip("Challenge reward: theme unlocked")
                    } else {
                        rewardScoreMultiplier += 0.05f
                        listener.onTip("All themes unlocked: +5% score boost")
                    }
                }
            }
        }
    }

    internal fun updatePaddle(dt: Float) {
        val previousX = paddle.x
        val target = paddle.targetX
        val snapToFinger = isDragging && (state == GameState.READY || hasStuckBall())
        if (snapToFinger) {
            paddle.x = target
        } else {
            val speed = 90f + settings.sensitivity * 180f
            val delta = target - paddle.x
            val dragBoost = if (isDragging) {
                val distanceBoost = (abs(delta) / 24f).coerceAtMost(2.5f)
                1f + distanceBoost
            } else {
                1f
            }
            val maxMove = speed * dragBoost * dt
            if (abs(delta) > 0.05f) {
                paddle.x += delta.coerceIn(-maxMove, maxMove)
            }
        }
        paddle.x = paddle.x.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
        paddleVelocity = if (dt > 0f) (paddle.x - previousX) / dt else 0f
        updateAimFromPaddle()
    }

    internal fun updateVolleyDanger() {
        if (config.mode != GameMode.VOLLEY) {
            renderer?.setVolleyDanger(0f)
            return
        }
        if (bricks.none { it.alive }) {
            renderer?.setVolleyDanger(0f)
            return
        }

        // Reuse the same board-pressure model that drives Volley status text.
        renderer?.setVolleyDanger(currentVolleyMetrics().pressure)
    }

    internal fun updateBricks(dt: Float) {
        var movedBricks = false
        if (config.mode.invaders) {
            updateInvaderFormation(dt)
            movedBricks = true
        }
        bricks.forEach { brick ->
            when (brick.type) {
                BrickType.MOVING -> {
                    // Initialize velocity if not set
                    if (brick.vx == 0f) {
                        brick.vx = (kotlin.random.Random(brick.gridX * 31 + brick.gridY * 17).nextFloat() - 0.5f) * 20f
                    }
                    // Move horizontally and bounce off edges
                    val previousX = brick.x
                    brick.x += brick.vx * dt
                    if (brick.x <= 0.6f || brick.x + brick.width >= worldWidth - 0.6f) {
                        brick.vx = -brick.vx
                        brick.x = brick.x.coerceIn(0.6f, worldWidth - brick.width - 0.6f)
                    }
                    if (abs(brick.x - previousX) > 0.0001f) {
                        movedBricks = true
                    }
                }
                BrickType.PHASE -> {
                    // Phase bricks pulse or change appearance based on phase
                    // Visual effect handled in rendering
                }
                BrickType.BOSS -> {
                    // Boss bricks stay static but use enhanced visuals
                }
                BrickType.INVADER -> {
                    // Invader formation movement handled globally.
                }
                else -> {
                    // Static bricks, no movement
                }
            }
            if (brick.hitFlash > 0f) {
                brick.hitFlash = max(0f, brick.hitFlash - dt * hitFlashDecayRate)
            }
            if (brick.fireFlash > 0f) {
                brick.fireFlash = max(0f, brick.fireFlash - dt * 3.2f)
            }
        }
        if (dynamicBrickLayout && movedBricks) {
            buildSpatialHash()
        } else if (spatialHashDirty) {
            buildSpatialHash()
        }
    }

    internal fun collectAliveInvaders(): List<Brick> {
        aliveInvaderBuffer.clear()
        invaderBricks.forEach { invader ->
            if (invader.alive) {
                aliveInvaderBuffer.add(invader)
            }
        }
        return aliveInvaderBuffer
    }

    internal fun updateInvaderFormation(dt: Float) {
        val invaders = collectAliveInvaders()
        if (invaders.isEmpty()) return
        invaderRowPhase += dt * (0.55f + levelIndex * 0.015f)
        invaderTurnSoundCooldown = max(0f, invaderTurnSoundCooldown - dt)
        val leftBound = 0.6f
        val rightBound = worldWidth - 0.6f

        fun rowDrift(row: Int): Float {
            return InvadersModeSystem.rowDrift(
                row = row,
                phase = invaderRowPhase,
                rowPhaseOffset = invaderRowPhaseOffset,
                rowDriftAmount = invaderRowDrift
            )
        }

        var minBaseX = Float.POSITIVE_INFINITY
        var maxBaseX = Float.NEGATIVE_INFINITY
        invaders.forEach { invader ->
            val x = invader.baseX + rowDrift(invader.gridY)
            minBaseX = min(minBaseX, x)
            maxBaseX = max(maxBaseX, x + invader.width)
        }

        val (minOffsetAllowed, maxOffsetAllowed) = InvadersModeSystem.formationOffsetLimits(
            minBaseX = minBaseX,
            maxBaseX = maxBaseX,
            leftBound = leftBound,
            rightBound = rightBound
        )
        val offsetStep = InvadersModeSystem.nextFormationOffset(
            currentOffset = invaderFormationOffset,
            direction = invaderDirection,
            speed = invaderSpeed,
            dt = dt,
            minOffsetAllowed = minOffsetAllowed,
            maxOffsetAllowed = maxOffsetAllowed
        )
        if (offsetStep.playTurnSound) {
            playInvaderTurnSound()
        }
        invaderDirection = offsetStep.direction
        invaderFormationOffset = offsetStep.offset
        invaders.forEach { invader ->
            val drift = rowDrift(invader.gridY)
            invader.x = invader.baseX + invaderFormationOffset + drift
        }
    }

    internal fun playInvaderTurnSound() {
        if (invaderTurnSoundCooldown > 0f) return
        audio.play(GameSound.BRICK_MOVING, 0.12f, 0.92f)
        invaderTurnSoundCooldown = invaderTurnSoundMinInterval
    }

    internal fun compressInvaderX(rawX: Float, brickWidth: Float): Float {
        val center = worldWidth * 0.5f
        val brickCenter = rawX + brickWidth * 0.5f
        val compressedCenter = center + (brickCenter - center) * invaderFormationCompression
        return compressedCenter - brickWidth * 0.5f
    }

    internal fun attachBallToPaddle() {
        balls.firstOrNull()?.let { ball ->
            ball.x = paddle.x
            ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
            ball.vx = 0f
            ball.vy = 0f
            ball.trail.clear()
            ball.trailTimer = 0f
        }
    }

    internal fun launchBall() {
        syncAimForLaunch()
        attachBallToPaddle()
        if (config.mode == GameMode.VOLLEY) {
            launchVolleyTurn()
            return
        }
        balls.firstOrNull()?.let { ball ->
            if (ball.vx == 0f && ball.vy == 0f) {
                launchBallWithAim(ball)
                aimHasInput = false
                aimNormalized = 0f
                aimNormalizedTarget = 0f
                state = GameState.RUNNING
                // Start background music when gameplay begins
                audio.startMusic()
            }
        }
    }

    internal fun releaseStuckBalls() {
        val stuck = balls.filter { it.stuckToPaddle }
        if (stuck.isEmpty()) return
        syncAimForLaunch()
        val spread = 0.08f
        val center = (stuck.size - 1) / 2f
        stuck.forEachIndexed { index, ball ->
            val offset = (index - center) * spread
            launchBallWithAim(ball, offset)
        }
        aimHasInput = false
        aimNormalized = 0f
        aimNormalizedTarget = 0f
        audio.startMusic()
    }

    internal fun launchVolleyTurn() {
        if (volleyTurnActive) return
        val firstBall = balls.firstOrNull() ?: run {
            spawnBall()
            balls.firstOrNull()
        } ?: return
        if (firstBall.vx != 0f || firstBall.vy != 0f) return

        volleyTurnActive = true
        volleyQueuedBalls = (volleyBallCount - 1).coerceAtLeast(0)
        volleyLaunchTimer = 0f
        volleyLaunchX = paddle.x
        volleyReturnAnchorX = Float.NaN
        volleyReturnSumX = 0f
        volleyReturnCount = 0

        launchBallWithAim(firstBall)
        audio.play(GameSound.BOUNCE, 0.45f, 1.08f)
        aimHasInput = false
        aimNormalized = 0f
        aimNormalizedTarget = 0f
        state = GameState.RUNNING
        audio.startMusic()
    }

    internal fun updateVolleyLaunchQueue(dt: Float) {
        if (!volleyTurnActive || volleyQueuedBalls <= 0) return
        volleyLaunchTimer -= dt
        while (volleyQueuedBalls > 0 && volleyLaunchTimer <= 0f) {
            spawnBall(spawnX = volleyLaunchX.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f))
            balls.lastOrNull()?.let { launchBallWithAim(it) }
            audio.play(GameSound.BOUNCE, 0.28f, 1.12f)
            volleyQueuedBalls -= 1
            volleyLaunchTimer += 0.065f
        }
    }

    internal fun spawnBall(spawnX: Float = paddle.x) {
        val ball = Ball(spawnX, paddle.y + 5f, 0.92f, 0f, 0f)
        if (fireballActive) {
            ball.isFireball = true
            ball.color = PowerUpType.FIREBALL.color
        } else if (pierceActive) {
            ball.color = PowerUpType.PIERCE.color
        }
        balls.add(ball)
    }

    internal fun updateBalls(dt: Float) {
        var lifeLossPending = false
        val iterator = balls.iterator()
        while (iterator.hasNext()) {
            val ball = iterator.next()
            if (ball.stuckToPaddle) {
                val minX = paddle.x - paddle.width / 2f + ball.radius
                val maxX = paddle.x + paddle.width / 2f - ball.radius
                ball.x = (paddle.x + ball.stickOffset).coerceIn(minX, maxX)
                ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
                ball.vx = 0f
                ball.vy = 0f
                continue
            }
            val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            val maxStep = ball.radius * 0.75f
            val steps = max(1, ceil((speed * dt) / maxStep).toInt())
            val stepDt = dt / steps
            var removed = false

            repeat(steps) {
                if (removed) return@repeat
                if (gravityWellActive) {
                    applyGravityWell(ball, stepDt)
                }
                ball.x += ball.vx * stepDt
                ball.y += ball.vy * stepDt

                if (ball.x - ball.radius < 0f) {
                    ball.x = ball.radius
                    ball.vx = abs(ball.vx)
                    audio.play(GameSound.BOUNCE, 0.6f)
                    ball.ricochetBounces?.let { bounces ->
                        ball.ricochetBounces = bounces - 1
                        if (bounces <= 1) ball.ricochetBounces = null
                    }
                } else if (ball.x + ball.radius > worldWidth) {
                    ball.x = worldWidth - ball.radius
                    ball.vx = -abs(ball.vx)
                    audio.play(GameSound.BOUNCE, 0.6f)
                    ball.ricochetBounces?.let { bounces ->
                        ball.ricochetBounces = bounces - 1
                        if (bounces <= 1) ball.ricochetBounces = null
                    }
                }

                if (ball.y + ball.radius > worldHeight) {
                    ball.y = worldHeight - ball.radius
                    ball.vy = -abs(ball.vy)
                    audio.play(GameSound.BOUNCE, 0.6f)
                }

                if (ball.y - ball.radius < 0f) {
                    if (config.mode == GameMode.VOLLEY) {
                        val clampedReturnX = ball.x.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
                        volleyReturnSumX += clampedReturnX
                        volleyReturnCount += 1
                        volleyReturnAnchorX = if (!volleyReturnAnchorX.isFinite()) {
                            clampedReturnX
                        } else {
                            volleyReturnAnchorX + (clampedReturnX - volleyReturnAnchorX) * 0.35f
                        }
                        iterator.remove()
                        removed = true
                        return@repeat
                    } else if (config.mode.godMode) {
                        ball.y = ball.radius + 2f
                        ball.vy = abs(ball.vy)
                        audio.play(GameSound.BOUNCE, 0.6f)
                    } else if (guardrailActive) {
                        ball.y = ball.radius + 2f
                        ball.vy = abs(ball.vy)
                        logger?.logBallLost(balls.size, Pair(ball.x, ball.y), lives)
                        audio.play(GameSound.BOUNCE, 0.6f)
                    } else if (shieldCharges > 0) {
                        shieldCharges -= 1
                        ball.y = ball.radius + 2f
                        ball.vy = abs(ball.vy)
                        audio.play(GameSound.POWERUP, 0.6f)
                        if (shieldCharges == 0) {
                            activeEffects.remove(PowerUpType.SHIELD)
                        }
                    } else {
                        iterator.remove()
                        if (balls.isEmpty()) {
                            lifeLossPending = true
                        }
                        removed = true
                        return@repeat
                    }
                }

                if (config.mode != GameMode.VOLLEY) {
                    handlePaddleCollision(ball)
                }
                handleBrickCollision(ball)
            }

            if (!removed) {
                clampBallSpeed(ball)
            }
        }
        if (lifeLossPending && state != GameState.GAME_OVER) {
            // Defer life-loss handling until after iteration to avoid mutating
            // the balls list while its iterator is active.
            loseLife()
        }
    }

    internal fun resolveVolleyTurnIfReady() {
        if (!volleyTurnActive) return

        val states = countVolleyBallStates()
        val decision = VolleyModeSystem.evaluateTurnDecision(
            turnActive = volleyTurnActive,
            queuedBalls = volleyQueuedBalls,
            inFlightBalls = states.inFlightBalls,
            stuckBalls = states.stuckBalls,
            stalledBalls = states.stalledBalls
        )
        if (decision.shouldAutoReleaseStuck) {
            releaseStuckBalls()
            return
        }
        if (decision.shouldNudgeStalledBalls) {
            nudgeStalledVolleyBalls()
            return
        }
        if (!decision.shouldResolveTurn) return

        volleyTurnActive = false
        volleyTurnCount += 1
        volleyAdvanceRows += 1
        val pressureBeforeSpawn = volleyLanePressure()
        audio.play(GameSound.BRICK_MOVING, 0.36f, 0.88f)
        emitVisualFeedback(VisualFeedbackEvent.VOLLEY_ROW_DROP)
        if (VolleyModeSystem.shouldAwardBall(volleyTurnCount, volleyBallCount, pressureBeforeSpawn) && volleyBallCount < VolleyModeSystem.MAX_BALL_COUNT) {
            volleyBallCount += 1
            listener.onVolleyBallsUpdated(volleyBallCount)
            listener.onTip("Volley +1 ball (${volleyBallCount} total).")
            audio.play(GameSound.POWERUP, 0.32f, 1.1f)
        }

        relayoutBricks()
        if (triggerVolleyBreachIfNeeded()) {
            return
        }

        spawnVolleyTopRow()
        relayoutBricks()
        if (triggerVolleyBreachIfNeeded()) {
            return
        }

        val pressureAfterSpawn = volleyLanePressure()
        maybeSpawnVolleySupplyDrop(pressureAfterSpawn)
        val averagedReturnX = if (volleyReturnCount > 0) {
            volleyReturnSumX / volleyReturnCount.toFloat()
        } else {
            Float.NaN
        }
        val anchorX = when {
            averagedReturnX.isFinite() -> averagedReturnX
            volleyReturnAnchorX.isFinite() -> volleyReturnAnchorX
            else -> paddle.targetX
        }
        val launchX = anchorX * 0.78f + paddle.targetX * 0.22f
        paddle.x = launchX.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
        paddle.targetX = paddle.x
        val laneCols = effectiveVolleyColumns((currentLayout?.cols ?: 10).coerceAtLeast(6))
        volleyPreferredLaneCol = resolveVolleyLaneColumn(laneCols)
        volleyReturnAnchorX = Float.NaN
        volleyReturnSumX = 0f
        volleyReturnCount = 0
        spawnBall(paddle.x)
        syncAimForLaunch()
        state = GameState.READY
        updatePowerupStatus()
    }

    internal fun spawnVolleyTopRow() {
        val layout = currentLayout ?: return
        val cols = effectiveVolleyColumns(layout.cols)
        val spawnRow = -volleyAdvanceRows
        val occupied = HashSet<Int>(cols)
        bricks.forEach { brick ->
            if (brick.alive && brick.gridY == spawnRow) {
                occupied.add(brick.gridX)
            }
        }

        val aliveBricks = bricks.count { it.alive }
        val softCap = (cols * 5).coerceAtLeast(24)
        val congestionPenalty = ((aliveBricks - softCap).coerceAtLeast(0) / softCap.toFloat()).coerceIn(0f, 0.38f)
        val earlyEase = when {
            volleyTurnCount < 3 -> 0.2f
            volleyTurnCount < 6 -> 0.11f
            else -> 0f
        }
        val pressure = currentVolleyMetrics().pressure
        val nearBreach = pressure >= 0.45f
        val slateDensityBoost = if (isSlateAspect()) 0.04f else 0f
        val lowBallRelief = when {
            volleyBallCount <= 5 -> 0.1f
            volleyBallCount <= 7 -> 0.06f
            else -> 0f
        }
        val dangerRelief = pressure * 0.2f
        val density = (
            0.66f +
                levelIndex * 0.008f +
                volleyTurnCount * 0.003f +
                slateDensityBoost -
                congestionPenalty -
                earlyEase -
                lowBallRelief -
                dangerRelief
            ).coerceIn(0.42f, 0.86f)

        val ballRelief = ((volleyBallCount - 5).coerceAtLeast(0) * 0.045f).coerceAtMost(0.26f)
        val earlyHpRelief = if (volleyTurnCount < 4) 0.08f else 0f
        val hpScale = (
            0.94f +
                levelIndex * 0.052f +
                volleyTurnCount * 0.016f -
                ballRelief -
                pressure * 0.22f -
                earlyHpRelief
            ).coerceAtLeast(0.74f)

        val danger = (volleyTurnCount / 10f).coerceIn(0f, 1f)
        val pressureScale = 1f - pressure * 0.45f
        val explosiveChance = (0.028f + danger * 0.045f - pressure * 0.02f).coerceIn(0.01f, 0.09f)
        val reinforcedChance = ((0.1f + danger * 0.085f) * pressureScale).coerceIn(0.08f, 0.22f)
        val armoredChance = if (volleyTurnCount < 3) 0f else ((0.055f + danger * 0.075f) * pressureScale).coerceAtMost(0.16f)
        val movingChance = if (volleyTurnCount < 5) 0f else (0.045f * pressureScale).coerceAtMost(0.05f)
        val phaseChance = if (volleyTurnCount < 7) 0f else (0.035f * pressureScale).coerceAtMost(0.04f)
        val spawningChance = if (volleyTurnCount < 9) 0f else (0.028f * pressureScale).coerceAtMost(0.03f)

        val preferredLane = resolveVolleyLaneColumn(cols)
        val forcedGapPrimary = preferredLane
        val forcedGapSecondary = if (volleyTurnCount < 6 && cols >= 8) {
            (forcedGapPrimary + (cols / 3).coerceAtLeast(2)) % cols
        } else {
            -1
        }
        val forcedGapTertiary = if (volleyTurnCount < 9 && cols >= 11) {
            (forcedGapPrimary + (cols / 2)) % cols
        } else {
            -1
        }
        val forcedGaps = hashSetOf(forcedGapPrimary)
        if (forcedGapSecondary >= 0) forcedGaps.add(forcedGapSecondary)
        if (forcedGapTertiary >= 0) forcedGaps.add(forcedGapTertiary)
        if (nearBreach && cols >= 7) {
            forcedGaps.add((forcedGapPrimary + (cols / 4).coerceAtLeast(1)) % cols)
            forcedGaps.add(wrapVolleyColumn(forcedGapPrimary - (cols / 4).coerceAtLeast(1), cols))
        }
        if (volleyBallCount <= 6 && cols >= 9) {
            forcedGaps.add(wrapVolleyColumn(forcedGapPrimary + 1, cols))
            forcedGaps.add(wrapVolleyColumn(forcedGapPrimary - 1, cols))
        }
        var spawned = 0

        for (col in 0 until cols) {
            if (col in forcedGaps) continue
            if (occupied.contains(col)) continue
            val laneDistance = volleyColumnDistance(col, preferredLane, cols)
            val laneRelief = when {
                laneDistance == 0 -> 0.26f + pressure * 0.12f
                laneDistance == 1 -> (if (nearBreach) 0.18f else 0.11f) + pressure * 0.06f
                laneDistance == 2 && pressure > 0.6f -> 0.06f
                else -> 0f
            }
            val effectiveDensity = (density - laneRelief).coerceIn(0.36f, 0.87f)
            if (random.nextFloat() > effectiveDensity) continue
            val typeRoll = random.nextFloat()
            val type = when {
                typeRoll < explosiveChance -> BrickType.EXPLOSIVE
                typeRoll < explosiveChance + reinforcedChance -> BrickType.REINFORCED
                typeRoll < explosiveChance + reinforcedChance + armoredChance -> BrickType.ARMORED
                typeRoll < explosiveChance + reinforcedChance + armoredChance + movingChance -> BrickType.MOVING
                typeRoll < explosiveChance + reinforcedChance + armoredChance + movingChance + phaseChance -> BrickType.PHASE
                typeRoll < explosiveChance + reinforcedChance + armoredChance + movingChance + phaseChance + spawningChance -> BrickType.SPAWNING
                else -> BrickType.NORMAL
            }
            val baseHp = baseHitPoints(type)
            val hp = if (type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * hpScale).roundToInt())
            val brick = Brick(
                gridX = col,
                gridY = spawnRow,
                x = 0f,
                y = 0f,
                width = 1f,
                height = 1f,
                hitPoints = hp,
                maxHitPoints = hp,
                type = type
            )
            // Initialize phase/spawn props
            if (type == BrickType.PHASE) {
                brick.maxPhase = 2
                brick.phase = 0
            } else if (type == BrickType.SPAWNING) {
                brick.spawnCount = 2
            }
            bricks.add(brick)
            if (isBreakable(type)) {
                aliveBreakableBrickCount += 1
            }
            if (type == BrickType.EXPLOSIVE) {
                aliveExplosiveBrickCount += 1
            }
            spawned += 1
        }
        if (spawned == 0 && cols > forcedGaps.size) {
            val fallbackCol = ((forcedGapPrimary + cols / 2) % cols)
                .coerceIn(0, cols - 1)
                .let { candidate ->
                    if (candidate in forcedGaps || occupied.contains(candidate)) {
                        (0 until cols).firstOrNull { it !in forcedGaps && !occupied.contains(it) } ?: -1
                    } else {
                        candidate
                    }
                }
            if (fallbackCol >= 0) {
                val hp = max(1, (baseHitPoints(BrickType.NORMAL) * hpScale).roundToInt())
                bricks.add(
                    Brick(
                        gridX = fallbackCol,
                        gridY = spawnRow,
                        x = 0f,
                        y = 0f,
                        width = 1f,
                        height = 1f,
                        hitPoints = hp,
                        maxHitPoints = hp,
                        type = BrickType.NORMAL
                    )
                )
                aliveBreakableBrickCount += 1
                spawned = 1
            }
        }
        if (spawned > 0) {
            spatialHashDirty = true
            val spawnPitch = (0.94f + spawned.coerceAtMost(8) * 0.02f).coerceAtMost(1.1f)
            audio.play(GameSound.BRICK_SPAWNING, 0.34f, spawnPitch)
        }
    }

    internal fun volleyLanePressure(): Float {
        if (config.mode != GameMode.VOLLEY) return 0f
        return currentVolleyMetrics().pressure
    }

    internal fun maybeSpawnVolleySupplyDrop(pressure: Float) {
        if (config.mode != GameMode.VOLLEY) return
        val turnsSinceDrop = volleyTurnCount - lastVolleySupplyTurn
        if (turnsSinceDrop <= 0) return
        val nearBreach = pressure >= 0.52f
        val lowBallBoost = when {
            volleyBallCount <= 5 -> 0.3f
            volleyBallCount <= 7 -> 0.18f
            else -> 0f
        }
        val cadenceBoost = if (turnsSinceDrop >= 3) 0.12f else 0f
        val overflowPenalty = if (volleyBallCount >= 12) 0.1f else 0f
        val chance = (
            0.16f +
                levelIndex * 0.006f +
                pressure * 0.34f +
                lowBallBoost +
                cadenceBoost -
                overflowPenalty
            ).coerceIn(0.12f, 0.72f)
        val minTurnsBetweenDrops = if (nearBreach || volleyBallCount <= 6) 1 else 2
        if (turnsSinceDrop < minTurnsBetweenDrops) return
        if (random.nextFloat() > chance) return

        val cols = effectiveVolleyColumns((currentLayout?.cols ?: 10).coerceAtLeast(6))
        val laneCol = resolveVolleyLaneColumn(cols)
        val laneX = ((laneCol.toFloat() + 0.5f) / cols.toFloat()) * worldWidth
        val spread = worldWidth / (cols.toFloat() * 3.2f)
        val spawnX = (laneX + (random.nextFloat() - 0.5f) * spread).coerceIn(8f, worldWidth - 8f)
        val spawnY = (worldHeight * if (nearBreach) 0.46f else 0.58f)
            .coerceIn(paddle.y + 12f, worldHeight * 0.8f)
        val hint = if (nearBreach || pressure > 0.68f) {
            PowerupSelectionHint.VOLLEY_SUPPLY_CRITICAL
        } else {
            PowerupSelectionHint.VOLLEY_SUPPLY
        }
        spawnPowerup(spawnX, spawnY, randomPowerupType(hint))
        lastVolleySupplyTurn = volleyTurnCount
    }

    internal fun hasVolleyBreach(): Boolean {
        if (config.mode != GameMode.VOLLEY) return false
        val breachY = ModeBoardMetrics.volleyBreachY(
            paddleY = paddle.y,
            paddleHeight = paddle.height
        )
        return ModeBoardMetrics.hasVolleyBreach(bricks = bricks, breachY = breachY)
    }

    internal fun triggerVolleyBreachIfNeeded(): Boolean {
        if (!hasVolleyBreach()) return false
        logger?.logGameOver(score, levelIndex + 1, "volley_breach")
        listener.onTip("Breach! Bricks reached the launch line.")
        triggerGameOver()
        return true
    }

    internal fun updateBallTrails(dt: Float) {
        balls.forEach { ball ->
            val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            if (speed < 5f) {
                ball.trail.clear()
                ball.trailTimer = 0f
                return@forEach
            }
            val interval = (0.024f - speed * 0.0002f).coerceIn(0.012f, 0.03f)
            ball.trailTimer -= dt
            if (ball.trailTimer <= 0f) {
                ball.trailTimer = interval
                val radius = ball.radius * 0.9f
                ball.trail.addFirst(
                    TrailPoint(
                        x = ball.x,
                        y = ball.y,
                        radius = radius,
                        life = trailLife,
                        maxLife = trailLife
                    )
                )
                while (ball.trail.size > maxTrailPoints) {
                    ball.trail.removeLast()
                }
            }

            val iterator = ball.trail.iterator()
            while (iterator.hasNext()) {
                val point = iterator.next()
                point.life -= dt
                if (point.life <= 0f) {
                    iterator.remove()
                }
            }
        }
    }

    internal fun applyGravityWell(ball: Ball, dt: Float) {
        val centerX = worldWidth * 0.5f
        val centerY = worldHeight * 0.62f
        val dx = centerX - ball.x
        val dy = centerY - ball.y
        val distSq = dx * dx + dy * dy
        if (distSq < 0.1f) return
        val pull = 120f / (distSq + 200f)
        ball.vx += dx * pull * dt
        ball.vy += dy * pull * dt
    }

    internal fun updateBeams(dt: Float) {
        laserCooldown = max(0f, laserCooldown - dt)
        beams.forEach { beam ->
            beam.y += beam.speed * dt
        }
        beams.removeAll { it.y > worldHeight + 4f }
        handleBeamCollision()
    }

    internal fun updateInvaderShots(dt: Float) {
        if (!config.mode.invaders) return
        invaderShotTimer -= dt
        val invaders = collectAliveInvaders()
        if (invaders.isEmpty()) return
        val pace = InvadersModeSystem.paceAdjustments(
            aliveCount = invaders.size,
            totalCount = invaderTotal,
            baseSpeed = invaderBaseSpeed,
            baseShotCooldown = invaderBaseShotCooldown
        )
        invaderSpeed = pace.speed
        invaderShotCooldown = pace.shotCooldown
        var allowFire = true
        var volleyTriggered = false

        if (invaderWaveStyle == 2) {
            if (invaderPauseTimer > 0f) {
                invaderPauseTimer -= dt
                allowFire = false
            } else if (invaderBurstCount == 0) {
                invaderBurstCount = 2 + random.nextInt(2)
                invaderShotTimer = min(invaderShotTimer, 0.08f)
            }
        }

        if (invaderWaveStyle == 1) {
            invaderVolleyTimer -= dt
            if (invaderVolleyTimer <= 0f) {
                val volleyShots = InvadersModeSystem.volleyShotCount(levelIndex)
                val minRow = invaders.minOf { it.gridY }
                val candidates = invaders.filter { it.gridY <= minRow + 1 }.shuffled(random)
                val selected = if (candidates.size >= volleyShots) {
                    candidates.take(volleyShots)
                } else {
                    (candidates + invaders.shuffled(random)).distinct().take(volleyShots)
                }
                selected.forEach { target ->
                    target.fireFlash = max(target.fireFlash, invaderTelegraphLead)
                    spawnInvaderShot(target)
                }
                invaderVolleyTimer = invaderShotCooldown * (2.1f + random.nextFloat() * 0.8f)
                invaderShotTimer = invaderShotCooldown * (0.6f + random.nextFloat() * 0.5f)
                volleyTriggered = true
            }
        }

        if (allowFire && !volleyTriggered && invaderShotTimer <= invaderTelegraphLead && invaderTelegraphKey == null) {
            val target = invaders[random.nextInt(invaders.size)]
            invaderTelegraphKey = invaderKey(target)
            target.fireFlash = max(target.fireFlash, invaderTelegraphLead)
        }
        if (allowFire && !volleyTriggered && invaderShotTimer <= 0f) {
            if (InvadersModeSystem.canSpawnShot(levelIndex, enemyShots.size)) {
                val target = invaderTelegraphKey?.let { key -> invaders.firstOrNull { invaderKey(it) == key } }
                spawnInvaderShot(target ?: invaders[random.nextInt(invaders.size)])
                if (invaderWaveStyle == 2 && invaderBurstCount > 0) {
                    invaderBurstCount -= 1
                    if (invaderBurstCount <= 0) {
                        invaderPauseTimer = invaderShotCooldown * (1.4f + random.nextFloat() * 0.8f)
                    }
                }
            }
            invaderTelegraphKey = null
            invaderShotTimer = invaderShotCooldown * (0.7f + random.nextFloat() * 0.7f)
        }

        var lifeLossPending = false
        val iterator = enemyShots.iterator()
        while (iterator.hasNext()) {
            val shot = iterator.next()
            shot.x += shot.vx * dt
            shot.y += shot.vy * dt
            if (shot.wiggle > 0f && shot.wobbleFreq > 0f) {
                shot.age += dt
                shot.x += kotlin.math.sin(shot.age * shot.wobbleFreq) * shot.wiggle * dt
            }
            if (shot.y < -5f || shot.x < -5f || shot.x > worldWidth + 5f) {
                iterator.remove()
                continue
            }
            if (shotIntersectsPaddle(shot)) {
                if (handleInvaderShotHit(shot)) {
                    lifeLossPending = true
                }
                iterator.remove()
            }
        }
        if (lifeLossPending && state != GameState.GAME_OVER) {
            // Defer life-loss side effects (which may clear enemyShots) until after iterator traversal.
            loseLife()
        }
    }

    internal fun spawnInvaderShot(origin: Brick) {
        origin.fireFlash = 0.55f
        val baseSpeed = (28f + levelIndex * 1.2f).coerceAtMost(42f)
        val spread = 6f
        val vxBase = (random.nextFloat() - 0.5f) * spread
        val hpTier = origin.maxHitPoints.coerceAtMost(3)
        val shotColor = adjustColor(origin.currentColor(theme), 1.1f + hpTier * 0.05f, 1f)

        val roll = random.nextFloat()
        val speed: Float
        val radius: Float
        val wiggle: Float
        val wobbleFreq: Float
        val style: Int
        when {
            roll < 0.6f -> {
                speed = baseSpeed
                radius = 0.75f
                wiggle = 0f
                wobbleFreq = 0f
                style = 0
            }
            roll < 0.85f -> {
                speed = baseSpeed * 1.25f
                radius = 0.6f
                wiggle = 0f
                wobbleFreq = 0f
                style = 1
            }
            else -> {
                speed = baseSpeed * 0.9f
                radius = 0.7f
                wiggle = 5.5f
                wobbleFreq = 9f
                style = 2
            }
        }

        val finalColor = when (style) {
            1 -> adjustColor(shotColor, 1.2f, 1f)
            2 -> floatArrayOf(shotColor[0].coerceIn(0f, 1f), (shotColor[1] * 0.7f).coerceIn(0f, 1f), 1f, 1f)
            else -> shotColor
        }

        val shot = EnemyShot(
            x = origin.centerX,
            y = origin.y - origin.height * 0.2f,
            radius = radius + (hpTier - 1) * 0.1f,
            vx = vxBase * (0.8f + hpTier * 0.1f),
            vy = -speed,
            color = finalColor,
            wiggle = wiggle,
            wobbleFreq = wobbleFreq,
            style = style
        )
        enemyShots.add(shot)
        audio.play(GameSound.LASER, 0.35f)
    }

    internal fun shotIntersectsPaddle(shot: EnemyShot): Boolean {
        val paddleLeft = paddle.x - paddle.width / 2f
        val paddleRight = paddle.x + paddle.width / 2f
        val paddleBottom = paddle.y - paddle.height / 2f
        val paddleTop = paddle.y + paddle.height / 2f
        val closestX = shot.x.coerceIn(paddleLeft, paddleRight)
        val closestY = shot.y.coerceIn(paddleBottom, paddleTop)
        val dx = shot.x - closestX
        val dy = shot.y - closestY
        return dx * dx + dy * dy <= shot.radius * shot.radius
    }

    internal fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.life -= dt
            if (particle.life <= 0f) {
                iterator.remove()
            } else {
                particle.x += particle.vx * dt
                particle.y += particle.vy * dt
            }
        }
    }

    internal fun updateWaves(dt: Float) {
        val iterator = waves.iterator()
        while (iterator.hasNext()) {
            val wave = iterator.next()
            wave.life -= dt
            if (wave.life <= 0f) {
                iterator.remove()
            } else {
                wave.radius += wave.speed * dt
                val alpha = (wave.life / wave.maxLife).coerceIn(0f, 1f)
                wave.color[3] = alpha * 0.6f
            }
        }
    }

    internal fun updateEffects(dt: Float) {
        val iterator = activeEffects.entries.iterator()
        var ballStyleDirty = false
        var paddleWidthDirty = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - dt
            if (remaining <= 0f) {
                when (entry.key) {
                    PowerUpType.WIDE_PADDLE,
                    PowerUpType.SHRINK -> paddleWidthDirty = true
                    PowerUpType.SLOW -> Unit
                    PowerUpType.GUARDRAIL -> guardrailActive = config.mode.godMode
                    PowerUpType.LASER -> Unit
                    PowerUpType.SHIELD -> shieldCharges = 0
                    PowerUpType.FIREBALL -> {
                        fireballActive = false
                        ballStyleDirty = true
                    }
                    PowerUpType.MAGNET -> {
                        magnetActive = false
                        releaseStuckBalls()
                    }
                    PowerUpType.GRAVITY_WELL -> gravityWellActive = false
                    PowerUpType.FREEZE -> {
                        freezeActive = false
                    }
                    PowerUpType.PIERCE -> {
                        pierceActive = false
                        ballStyleDirty = true
                    }
                    PowerUpType.OVERDRIVE -> Unit
                    else -> Unit
                }
                iterator.remove()
            } else {
                entry.setValue(remaining)
            }
        }
        val baseSpeedMultiplier = when {
            activeEffects.containsKey(PowerUpType.FREEZE) -> 0.1f
            activeEffects.containsKey(PowerUpType.SLOW) -> 0.8f
            activeEffects.containsKey(PowerUpType.OVERDRIVE) -> 1.2f
            else -> 1f
        }
        timeWarpMultiplier = if (activeEffects.containsKey(PowerUpType.TIME_WARP)) 0.7f else 1f
        speedMultiplier = baseSpeedMultiplier * timeWarpMultiplier
        if (ballStyleDirty) {
            syncBallStyles()
        }
        if (paddleWidthDirty) {
            syncPaddleWidthFromEffects()
        }
        // Update screen flash
        levelClearFlash = max(0f, levelClearFlash - dt * 1.5f)
        powerupStatusTick -= dt
        if (powerupStatusTick <= 0f) {
            powerupStatusTick = 0.12f
            updatePowerupStatus()
        }
    }

    internal fun syncPaddleWidthFromEffects() {
        val wideActive = activeEffects.containsKey(PowerUpType.WIDE_PADDLE)
        val shrinkActive = activeEffects.containsKey(PowerUpType.SHRINK)
        paddle.width = when {
            wideActive && shrinkActive -> basePaddleWidth
            wideActive -> basePaddleWidth * (25f / 18f)
            shrinkActive -> basePaddleWidth * 0.7f
            else -> basePaddleWidth
        }
    }

    internal fun clampBallSpeed(ball: Ball) {
        val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
        if (speed <= 0f) return
        val minSpeed = config.mode.launchSpeed * minSpeedFactor()
        val maxSpeed = config.mode.launchSpeed * maxSpeedFactor()
        val target = when {
            speed < minSpeed -> minSpeed
            speed > maxSpeed -> maxSpeed
            else -> speed
        }
        if (target != speed) {
            val scale = target / speed
            ball.vx *= scale
            ball.vy *= scale
        }
        val minVerticalRatio = 0.22f
        val minVy = target * minVerticalRatio
        if (kotlin.math.abs(ball.vy) < minVy) {
            val signVy = if (ball.vy == 0f) 1f else kotlin.math.sign(ball.vy)
            val newVy = signVy * minVy
            val signVx = if (ball.vx == 0f) 1f else kotlin.math.sign(ball.vx)
            val newVx = kotlin.math.sqrt((target * target - newVy * newVy).coerceAtLeast(0f)) * signVx
            ball.vy = newVy
            ball.vx = newVx
        }
    }

    internal fun speedBoostSlope(): Float = ModeBalance.pacingFor(config.mode).speedBoostSlope

    internal fun speedBoostCap(): Float = ModeBalance.pacingFor(config.mode).speedBoostCap

    internal fun minSpeedFactor(): Float = ModeBalance.pacingFor(config.mode).minSpeedFactor

    internal fun maxSpeedFactor(): Float = ModeBalance.pacingFor(config.mode).maxSpeedFactor

    internal fun difficultyForMode(): Float {
        val pacing = ModeBalance.pacingFor(config.mode)
        return (pacing.difficultyBase + levelIndex * pacing.difficultySlope).coerceAtMost(3.0f)
    }

    internal fun syncBallStyles() {
        val useFire = fireballActive
        val usePierce = pierceActive
        balls.forEach { ball ->
            ball.isFireball = useFire
            ball.color = when {
                useFire -> PowerUpType.FIREBALL.color
                usePierce -> PowerUpType.PIERCE.color
                else -> ball.defaultColor
            }
        }
    }

    internal fun displaySeconds(time: Float): Int = ceil(time).toInt().coerceAtLeast(1)

    internal fun resolveVolleyLaneColumn(cols: Int): Int {
        val safeCols = cols.coerceAtLeast(1)
        val rawAnchorX = when {
            volleyReturnAnchorX.isFinite() -> volleyReturnAnchorX
            else -> paddle.targetX
        }.coerceIn(0f, worldWidth)
        val normalized = (rawAnchorX / worldWidth).coerceIn(0f, 0.9999f)
        val anchorCol = (normalized * safeCols).toInt().coerceIn(0, safeCols - 1)
        val candidate = if (volleyPreferredLaneCol >= 0) {
            val blend = (volleyPreferredLaneCol * 0.4f + anchorCol * 0.6f).roundToInt()
            blend.coerceIn(0, safeCols - 1)
        } else {
            anchorCol
        }
        volleyPreferredLaneCol = candidate
        return candidate
    }

    internal fun volleyColumnDistance(a: Int, b: Int, cols: Int): Int {
        val direct = kotlin.math.abs(a - b)
        return min(direct, cols - direct)
    }

    internal fun wrapVolleyColumn(index: Int, cols: Int): Int {
        if (cols <= 0) return 0
        return ((index % cols) + cols) % cols
    }

    internal fun tunnelGateZone(): ModeBoardMetrics.TunnelGateZone? {
        if (config.mode != GameMode.TUNNEL) return null
        val layout = currentLayout ?: return null
        return ModeBoardMetrics.tunnelGateZone(
            layoutCols = layout.cols,
            layoutRows = layout.rows,
            layoutColBoost = layoutColBoost,
            levelIndex = levelIndex
        )
    }

    internal fun tunnelGateIntegrityPercent(): Int {
        if (!tunnelGateIntegrityDirty) {
            return cachedTunnelGateIntegrityPercent
        }
        val gateZone = tunnelGateZone() ?: return 100
        cachedTunnelGateIntegrityPercent = ModeBoardMetrics.tunnelGateMetrics(
            bricks = bricks,
            gateZone = gateZone
        ).integrityPercent
        tunnelGateIntegrityDirty = false
        return cachedTunnelGateIntegrityPercent
    }

    internal fun isTunnelGateBrick(brick: Brick): Boolean {
        val gateZone = tunnelGateZone() ?: return false
        return brick.gridX in gateZone.minCol..gateZone.maxCol &&
            brick.gridY in gateZone.rows
    }

    internal fun markTunnelGateIntegrityDirty() {
        if (config.mode == GameMode.TUNNEL) {
            tunnelGateIntegrityDirty = true
        }
    }

    internal fun markTunnelGateIntegrityDirtyIfGateBrick(brick: Brick) {
        if (config.mode == GameMode.TUNNEL && isTunnelGateBrick(brick)) {
            tunnelGateIntegrityDirty = true
        }
    }

    internal fun onBrickDestroyed(brick: Brick) {
        if (isBreakable(brick.type) && aliveBreakableBrickCount > 0) {
            aliveBreakableBrickCount -= 1
        }
        if (brick.type == BrickType.EXPLOSIVE && aliveExplosiveBrickCount > 0) {
            aliveExplosiveBrickCount -= 1
        }
        if (config.mode == GameMode.TUNNEL && isTunnelGateBrick(brick)) {
            val oldIntegrity = tunnelGateIntegrityPercent()
            tunnelGateIntegrityDirty = true
            val newIntegrity = tunnelGateIntegrityPercent()
            if (oldIntegrity > 0 && newIntegrity == 0) {
                emitVisualFeedback(VisualFeedbackEvent.TUNNEL_GATE_BREACH)
            }
        }
    }

    internal fun tunnelBreakthroughPressure(): Float {
        if (config.mode != GameMode.TUNNEL) return 0f
        return ModeBoardMetrics.tunnelBreakthroughPressure(
            gateIntegrityPercent = tunnelGateIntegrityPercent(),
            tunnelShotsFired = tunnelShotsFired
        )
    }

    internal fun maybeSpawnTunnelSupplyDrop() {
        if (config.mode != GameMode.TUNNEL || state == GameState.PAUSED || state == GameState.GAME_OVER || awaitingNextLevel) return
        val shotsSinceDrop = tunnelShotsFired - lastTunnelSupplyShot
        if (shotsSinceDrop <= 0) return

        val gatePressure = tunnelBreakthroughPressure()
        val gateIntegrity = tunnelGateIntegrityPercent()

        val hasBreakthroughActive = hasBreakthroughActiveEffect()
        val hasBreakthroughDropQueued = hasQueuedBreakthroughDrop()

        val gate = TunnelModeSystem.supplyDropGate(
            gatePressure = gatePressure,
            gateIntegrityPercent = gateIntegrity,
            hasBreakthroughActive = hasBreakthroughActive,
            hasBreakthroughDropQueued = hasBreakthroughDropQueued
        )
        val dropDecision = TunnelModeSystem.supplyDropDecision(
            shotsSinceDrop = shotsSinceDrop,
            gate = gate,
            roll = random.nextFloat()
        )
        tunnelSupplyReadinessPercent = TunnelModeSystem.supplyReadinessPercent(
            shotsSinceDrop = shotsSinceDrop,
            requiredShots = gate.requiredShots
        )
        if (!dropDecision.shouldDrop) return

        val cols = ((currentLayout?.cols ?: 12) + layoutColBoost).coerceAtLeast(1)
        val lane = TunnelModeSystem.supplyLane(
            worldWidth = worldWidth,
            boardCols = cols,
            gateZone = tunnelGateZone()
        )
        val spawn = TunnelModeSystem.supplySpawnPoint(
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            paddleY = paddle.y,
            laneX = lane.laneX,
            spread = lane.spread,
            gatePressure = gatePressure,
            xJitterUnit = random.nextFloat()
        )
        spawnPowerup(spawn.x, spawn.y, randomPowerupType(PowerupSelectionHint.TUNNEL_BREAKTHROUGH))
        if (gatePressure >= 0.72f) {
            spawnPowerup(spawn.x - lane.spread * 0.4f, spawn.y, randomPowerupType(PowerupSelectionHint.TUNNEL_BREAKTHROUGH))
            spawnPowerup(spawn.x + lane.spread * 0.4f, spawn.y, randomPowerupType(PowerupSelectionHint.TUNNEL_BREAKTHROUGH))
        }
        lastTunnelSupplyShot = tunnelShotsFired
        tunnelSupplyReadinessPercent = 0
        if (gatePressure >= 0.72f) {
            listener.onTip("Tunnel supply drop inbound.")
        } else if (dropDecision.forcedByPity) {
            listener.onTip("Tunnel supply guaranteed after sustained pressure.")
            emitVisualFeedback(VisualFeedbackEvent.TUNNEL_PITY_SUPPLY)
        }
    }

    internal fun addScore(points: Int) {
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

    internal fun compactDeadVolleyBricksIfNeeded() {
        if (config.mode != GameMode.VOLLEY) return
        val totalBricks = bricks.size
        // Lower threshold to keep the board cleaner and more dynamic.
        if (totalBricks < 120) return
        val deadBricks = bricks.count { !it.alive }
        // Aggressively compact if a significant portion of the board is dead debris.
        if (deadBricks < 40 || deadBricks * 3 < totalBricks) return
        bricks.removeAll { !it.alive }
        spatialHashDirty = true
        buildSpatialHash()
    }

    internal fun reportScore() {
        updateScoreChallenges()
        listener.onScoreUpdated(score)
    }

    internal fun updateScoreChallenges() {
        val challenges = dailyChallenges ?: return
        val completed = mutableListOf<DailyChallenge>()
        challenges.forEach { challenge ->
            if (challenge.type != ChallengeType.SCORE_ACHIEVED || challenge.completed) return@forEach
            if (score > challenge.progress) {
                challenge.progress = score
            }
            if (challenge.progress >= challenge.targetValue) {
                challenge.completed = true
                challenge.rewardGranted = true
                completed.add(challenge)
            }
        }
        if (completed.isNotEmpty()) {
            handleChallengeRewards(completed)
        }
    }

    internal fun checkLevelCompletion() {
        if (state != GameState.RUNNING || awaitingNextLevel) return
        val hasRemainingBreakables = aliveBreakableBrickCount > 0
        if (!hasRemainingBreakables) {
            val levelDuration = elapsedSeconds - levelStartTime
            dailyChallenges?.let { challenges ->
                if (!lostLifeThisLevel) {
                    updateDailyChallenges(ChallengeType.PERFECT_LEVEL)
                }
                challenges.forEach { challenge ->
                    if (challenge.type == ChallengeType.TIME_UNDER_LIMIT && !challenge.completed) {
                        if (levelDuration <= challenge.targetValue) {
                            DailyChallengeManager.completeChallenge(challenge)
                            handleChallengeRewards(listOf(challenge))
                        }
                    }
                }
            }
            logger?.logLevelComplete(levelIndex + 1, score, elapsedSeconds, 0)
            levelClearFlash = 1.0f
            emitVisualFeedback(VisualFeedbackEvent.LEVEL_CLEAR)
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

    internal fun loseLife() {
        // Reset combo on life loss
        combo = 0
        comboTimer = 0f
        lostLifeThisLevel = true

        if (config.mode.godMode) {
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

    internal fun triggerGameOver() {
        if (state == GameState.GAME_OVER) return
        val summary = GameSummary(
            score = score,
            level = levelIndex + 1,
            durationSeconds = elapsedSeconds.toInt(),
            bricksBroken = runBricksBroken,
            livesLost = runLivesLost
        )
        awaitingNextLevel = false
        audio.play(GameSound.GAME_OVER, 1f)
        audio.haptic(GameHaptic.HEAVY)
        audio.stopMusic() // Stop background music on game over
        state = GameState.GAME_OVER
        listener.onGameOver(summary)
    }

    internal fun shootLaser() {
        if (laserCooldown > 0f) return
        laserCooldown = laserCooldownDuration
        val beamOffset = paddle.width / 3f
        beams.add(Beam(paddle.x - beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        beams.add(Beam(paddle.x + beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        updateDailyChallenges(ChallengeType.LASER_FIRED)
        audio.play(GameSound.LASER, 0.6f)
        listener.onLaserFired(laserCooldownDuration)
    }

    fun debugSpawnPowerup(type: PowerUpType) {
        powerups.clear()
        val spawnX = worldWidth * 0.5f
        val spawnY = (worldHeight * 0.55f).coerceIn(paddle.y + 10f, worldHeight * 0.8f)
        spawnPowerup(spawnX, spawnY, type)
    }

    internal fun triggerExplosion(brick: Brick) {
        audio.play(GameSound.EXPLOSION, 0.8f)
        audio.haptic(GameHaptic.HEAVY)
        emitVisualFeedback(VisualFeedbackEvent.EXPLOSION_BREAK)
        val radius = 1
        for (neighbor in bricks) {
            if (!neighbor.alive || neighbor.gridX < 0 || neighbor.gridY < 0 || !neighbor.isNeighbor(brick, radius)) continue
            if (neighbor == brick) continue
            val destroyed = neighbor.applyHit(true)
            if (destroyed) {
                addScore(neighbor.scoreValue)
                updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
                runBricksBroken += 1
                onBrickDestroyed(neighbor)
                spawnBrickDestructionFx(neighbor, brick.centerX, brick.centerY, intensity = 0.92f)
                maybeSpawnPowerup(neighbor)
            }
        }
        reportScore()
    }

    internal fun spawnImpactSparks(x: Float, y: Float, baseColor: FloatArray, count: Int, speed: Float) {
        val available = maxParticles - particles.size
        val actualCount = min(count, max(0, available))
        if (actualCount <= 0) return
        val sparkColor = adjustColor(baseColor, 1.2f, 1f)
        repeat(actualCount) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speedScale = speed * (0.5f + random.nextFloat() * 0.7f)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speedScale,
                    vy = kotlin.math.sin(angle) * speedScale,
                    radius = 0.35f + random.nextFloat() * 0.25f,
                    life = 0.25f + random.nextFloat() * 0.15f,
                    color = sparkColor
                )
            )
        }
    }

    internal fun spawnBrickDestructionFx(brick: Brick, impactX: Float, impactY: Float, intensity: Float) {
        if (brick.type == BrickType.INVADER) {
            spawnInvaderBurst(brick, intensity)
            return
        }

        val fxScale = intensity.coerceIn(0.65f, 1.35f)
        val base = brick.currentColor(theme)
        val sparkBase = when (brick.type) {
            BrickType.NORMAL -> 6
            BrickType.REINFORCED -> 8
            BrickType.ARMORED -> 10
            BrickType.EXPLOSIVE -> 14
            BrickType.UNBREAKABLE -> 5
            BrickType.MOVING -> 8
            BrickType.SPAWNING -> 9
            BrickType.PHASE -> 11
            BrickType.BOSS -> 18
            BrickType.INVADER -> 10
        }
        val debrisBase = when (brick.type) {
            BrickType.NORMAL -> 8
            BrickType.REINFORCED -> 10
            BrickType.ARMORED -> 12
            BrickType.EXPLOSIVE -> 18
            BrickType.UNBREAKABLE -> 6
            BrickType.MOVING -> 11
            BrickType.SPAWNING -> 13
            BrickType.PHASE -> 15
            BrickType.BOSS -> 26
            BrickType.INVADER -> 12
        }
        val waveLife = when (brick.type) {
            BrickType.BOSS -> 0.95f
            BrickType.EXPLOSIVE -> 0.82f
            BrickType.PHASE -> 0.68f
            BrickType.ARMORED -> 0.58f
            else -> 0.46f
        }
        val waveSpeed = when (brick.type) {
            BrickType.BOSS -> 23f
            BrickType.EXPLOSIVE -> 20f
            BrickType.PHASE -> 18f
            BrickType.ARMORED -> 17f
            else -> 14f
        }
        val shakeStrength = when (brick.type) {
            BrickType.BOSS -> 1.6f
            BrickType.EXPLOSIVE -> 1.25f
            BrickType.PHASE -> 0.92f
            BrickType.ARMORED -> 0.78f
            BrickType.REINFORCED -> 0.56f
            else -> 0f
        }
        val shakeDuration = when (brick.type) {
            BrickType.BOSS -> 0.13f
            BrickType.EXPLOSIVE -> 0.11f
            BrickType.PHASE -> 0.1f
            BrickType.ARMORED -> 0.09f
            BrickType.REINFORCED -> 0.08f
            else -> 0f
        }
        val directionalBias = when (brick.type) {
            BrickType.BOSS -> 7f
            BrickType.EXPLOSIVE -> 6.4f
            BrickType.PHASE -> 6f
            BrickType.ARMORED -> 5.2f
            else -> 3.8f
        } * fxScale

        val sparkCount = (sparkBase * fxScale).roundToInt().coerceAtLeast(3)
        val debrisTarget = (debrisBase * fxScale).roundToInt().coerceAtLeast(4)
        val impactColor = adjustColor(base, 1.15f, 1f)
        spawnImpactSparks(impactX, impactY, impactColor, sparkCount, 12f + waveSpeed * 0.45f)
        val centerSparkCount = (sparkCount * 0.45f).roundToInt().coerceAtLeast(2)
        spawnImpactSparks(brick.centerX, brick.centerY, adjustColor(base, 1.05f, 1f), centerSparkCount, 9f + waveSpeed * 0.35f)

        if (waves.size < maxWaves) {
            waves.add(
                ExplosionWave(
                    x = brick.centerX,
                    y = brick.centerY,
                    radius = 0.72f + fxScale * 0.34f,
                    color = adjustColor(base, 1.14f, 0.72f).copyOf(),
                    life = waveLife * fxScale,
                    maxLife = waveLife * fxScale,
                    speed = waveSpeed * (0.92f + fxScale * 0.18f)
                )
            )
        }
        if ((brick.type == BrickType.EXPLOSIVE || brick.type == BrickType.PHASE || brick.type == BrickType.BOSS) && waves.size < maxWaves) {
            waves.add(
                ExplosionWave(
                    x = brick.centerX,
                    y = brick.centerY,
                    radius = 1.08f + fxScale * 0.44f,
                    color = adjustColor(base, 0.85f, 0.55f).copyOf(),
                    life = waveLife * 0.78f,
                    maxLife = waveLife * 0.78f,
                    speed = waveSpeed * 1.2f
                )
            )
        }

        val dx = brick.centerX - impactX
        val dy = brick.centerY - impactY
        val distance = sqrt(dx * dx + dy * dy)
        val biasX = if (distance > 0.001f) dx / distance else 0f
        val biasY = if (distance > 0.001f) dy / distance else 0f
        val available = maxParticles - particles.size
        val debrisCount = min(debrisTarget, max(0, available))
        repeat(debrisCount) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = random.nextFloat() * (8f + fxScale * 5f) + 7f
            val biasScale = directionalBias * (0.45f + random.nextFloat() * 0.9f)
            val vx = kotlin.math.cos(angle) * speed + biasX * biasScale
            val vy = kotlin.math.sin(angle) * speed + biasY * biasScale
            val shade = 0.85f + random.nextFloat() * 0.42f
            particles.add(
                Particle(
                    x = brick.centerX,
                    y = brick.centerY,
                    vx = vx,
                    vy = vy,
                    radius = 0.36f + random.nextFloat() * 0.34f,
                    life = 0.36f + random.nextFloat() * 0.36f,
                    color = adjustColor(base, shade, 0.92f)
                )
            )
        }

        if (shakeStrength > 0f || brick.type == BrickType.EXPLOSIVE || brick.type == BrickType.BOSS) {
            val eventScale = ((shakeStrength / 2.4f).coerceAtLeast(0.55f) * fxScale).coerceIn(0.55f, 1.6f)
            emitVisualFeedback(VisualFeedbackEvent.EXPLOSION_BREAK, eventScale)
        }
    }

    internal fun spawnInvaderBurst(brick: Brick, intensity: Float = 1f) {
        val fxScale = intensity.coerceIn(0.65f, 1.35f)
        val base = brick.currentColor(theme)
        spawnImpactSparks(brick.centerX, brick.centerY, base, (10 * fxScale).roundToInt().coerceAtLeast(4), 20f * fxScale)
        if (waves.size < maxWaves) {
            waves.add(
                ExplosionWave(
                    x = brick.centerX,
                    y = brick.centerY,
                    radius = 0.9f,
                    color = adjustColor(base, 1.25f, 0.6f),
                    life = 0.9f * fxScale,
                    maxLife = 0.9f * fxScale,
                    speed = 18f * fxScale
                )
            )
        }
        val debrisCount = min((10 * fxScale).roundToInt().coerceAtLeast(4), max(0, maxParticles - particles.size))
        repeat(debrisCount) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = random.nextFloat() * (10f + fxScale * 5f) + 6f
            particles.add(
                Particle(
                    x = brick.centerX,
                    y = brick.centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    radius = 0.45f + random.nextFloat() * 0.35f,
                    life = 0.5f + random.nextFloat() * 0.25f,
                    color = adjustColor(base, 1.1f, 0.9f)
                )
            )
        }
        emitVisualFeedback(VisualFeedbackEvent.INVADER_BURST, fxScale)
    }

    internal fun brickSoundFor(type: BrickType): GameSound {
        return when (type) {
            BrickType.NORMAL -> GameSound.BRICK_NORMAL
            BrickType.REINFORCED -> GameSound.BRICK_REINFORCED
            BrickType.ARMORED -> GameSound.BRICK_ARMORED
            BrickType.EXPLOSIVE -> GameSound.BRICK_EXPLOSIVE
            BrickType.UNBREAKABLE -> GameSound.BRICK_UNBREAKABLE
            BrickType.MOVING -> GameSound.BRICK_MOVING
            BrickType.SPAWNING -> GameSound.BRICK_SPAWNING
            BrickType.PHASE -> GameSound.BRICK_PHASE
            BrickType.BOSS -> GameSound.BRICK_BOSS
            BrickType.INVADER -> GameSound.BRICK_NORMAL
        }
    }

    internal fun brickSoundRate(type: BrickType): Float {
        return when (type) {
            BrickType.NORMAL -> 1.0f
            BrickType.REINFORCED -> 0.96f
            BrickType.ARMORED -> 0.9f
            BrickType.EXPLOSIVE -> 0.85f
            BrickType.UNBREAKABLE -> 0.8f
            BrickType.MOVING -> 1.06f
            BrickType.SPAWNING -> 1.12f
            BrickType.PHASE -> 0.98f
            BrickType.BOSS -> 0.78f
            BrickType.INVADER -> 1.08f
        }
    }

    internal enum class PowerupSelectionHint {
        DEFAULT,
        VOLLEY_SUPPLY,
        VOLLEY_SUPPLY_CRITICAL,
        TUNNEL_BREAKTHROUGH
    }

    internal enum class PowerupBucket { OFFENSE, DEFENSE, CONTROL, RISK }

    internal fun spawnComboStreakParticles(x: Float, y: Float, combo: Int) {
        val available = maxParticles - particles.size
        val count = min(12, max(0, available))
        if (count <= 0) return

        val streakColor = when {
            combo >= 10 -> floatArrayOf(1f, 0.8f, 0f, 1f) // Gold
            combo >= 7 -> floatArrayOf(0.8f, 0.4f, 0.9f, 1f) // Purple
            else -> floatArrayOf(0.4f, 0.8f, 1f, 1f) // Blue
        }

        repeat(count) { index ->
            val angle = (index / count.toFloat()) * (Math.PI.toFloat() * 2f)
            val speed = 8f + random.nextFloat() * 6f
            val radius = 0.3f + random.nextFloat() * 0.3f
            particles.add(
                Particle(
                    x = x + random.nextFloat() * 4f - 2f,
                    y = y + random.nextFloat() * 4f - 2f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed + random.nextFloat() * 4f - 2f, // Some upward bias
                    radius = radius,
                    life = 0.6f + random.nextFloat() * 0.4f,
                    color = streakColor
                )
            )
        }
    }

    internal fun spawnLevelCompleteConfetti() {
        val available = maxParticles - particles.size
        val count = min(25, max(0, available)) // More particles for celebration
        if (count <= 0) return

        val confettiColors = arrayOf(
            floatArrayOf(1f, 0f, 0f, 1f), // Red
            floatArrayOf(0f, 1f, 0f, 1f), // Green
            floatArrayOf(0f, 0f, 1f, 1f), // Blue
            floatArrayOf(1f, 1f, 0f, 1f), // Yellow
            floatArrayOf(1f, 0f, 1f, 1f), // Magenta
            floatArrayOf(0f, 1f, 1f, 1f), // Cyan
            floatArrayOf(1f, 0.5f, 0f, 1f), // Orange
            floatArrayOf(0.5f, 0f, 1f, 1f)  // Purple
        )

        repeat(count) { index ->
            val colorIndex = index % confettiColors.size
            val startX = worldWidth * 0.2f + random.nextFloat() * (worldWidth * 0.6f)
            val startY = worldHeight * 0.7f + random.nextFloat() * (worldHeight * 0.2f)
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 12f + random.nextFloat() * 8f
            val radius = 0.4f + random.nextFloat() * 0.3f

            particles.add(
                Particle(
                    x = startX,
                    y = startY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - random.nextFloat() * 6f, // Upward bias with some variation
                    radius = radius,
                    life = 2.0f + random.nextFloat() * 1.5f, // Longer life for celebration
                    color = confettiColors[colorIndex]
                )
            )
        }
    }

    internal fun spawnChildBricks(parentBrick: Brick) {
        // Spawn 2-3 smaller bricks around the destroyed spawning brick
        val spawnCount = 2 + kotlin.random.Random(parentBrick.gridX * 13 + parentBrick.gridY * 19).nextInt(2)
        val childSize = parentBrick.width * 0.6f
        val childHeight = parentBrick.height * 0.6f

        if (config.mode == GameMode.VOLLEY && parentBrick.gridX >= 0) {
            val layout = currentLayout
            if (layout != null) {
                val cols = effectiveVolleyColumns(layout.cols).coerceAtLeast(1)
                val occupied = HashSet<Long>(bricks.size * 2)
                fun key(col: Int, row: Int): Long = (col.toLong() shl 32) or (row.toLong() and 0xffffffffL)
                bricks.forEach { brick ->
                    if (brick.alive && brick.gridX >= 0) {
                        occupied.add(key(brick.gridX, brick.gridY))
                    }
                }
                val candidateOffsets = intArrayOf(0, -1, 1, -2, 2, -3, 3)
                var created = 0
                for (offset in candidateOffsets) {
                    if (created >= spawnCount) break
                    val childCol = wrapVolleyColumn(parentBrick.gridX + offset, cols)
                    val childRow = parentBrick.gridY
                    val cellKey = key(childCol, childRow)
                    if (!occupied.add(cellKey)) continue
                    val childBrick = Brick(
                        gridX = childCol,
                        gridY = childRow,
                        x = parentBrick.x,
                        y = parentBrick.y,
                        width = childSize,
                        height = childHeight,
                        hitPoints = 1,
                        maxHitPoints = 1,
                        type = BrickType.NORMAL
                    )
                    childBrick.baseX = childBrick.x
                    childBrick.baseY = childBrick.y
                    bricks.add(childBrick)
                    aliveBreakableBrickCount += 1
                    created += 1
                }
                if (created > 0) {
                    relayoutBricks()
                    markTunnelGateIntegrityDirty()
                    return
                }
            }
        }

        for (i in 0 until spawnCount) {
            val offsetX = (kotlin.random.Random(i * 7).nextFloat() - 0.5f) * parentBrick.width
            val offsetY = (kotlin.random.Random(i * 11).nextFloat() - 0.5f) * parentBrick.height

            val childX = (parentBrick.x + parentBrick.width / 2f + offsetX - childSize / 2f)
                .coerceIn(0.6f, worldWidth - childSize - 0.6f)
            val childY = (parentBrick.y + parentBrick.height / 2f + offsetY - childHeight / 2f)
                .coerceIn(worldHeight * 0.55f, worldHeight * 0.88f - childHeight)

            val childBrick = Brick(
                gridX = -1, // Not on grid
                gridY = -1,
                x = childX,
                y = childY,
                width = childSize,
                height = childHeight,
                hitPoints = 1,
                maxHitPoints = 1,
                type = BrickType.NORMAL
            )
            childBrick.baseX = childX
            childBrick.baseY = childY
            bricks.add(childBrick)
            aliveBreakableBrickCount += 1
        }
        spatialHashDirty = true
        markTunnelGateIntegrityDirty()
    }

}

enum class GameState {
    READY, RUNNING, PAUSED, GAME_OVER
}

data class Ball(
    var x: Float,
    var y: Float,
    var radius: Float,
    var vx: Float,
    var vy: Float,
    var isFireball: Boolean = false,
    var color: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f),
    var stuckToPaddle: Boolean = false,
    var stickOffset: Float = 0f,
    var ricochetBounces: Int? = null
) {
    val defaultColor: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f)
    val trail: ArrayDeque<TrailPoint> = ArrayDeque()
    var trailTimer: Float = 0f
}

data class Paddle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var targetX: Float = x
)

data class Brick(
    val gridX: Int,
    val gridY: Int,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var baseX: Float = x,
    var baseY: Float = y,
    var hitPoints: Int,
    val maxHitPoints: Int,
    val type: BrickType,
    var alive: Boolean = true
) {
    companion object {
        internal val ROW_BANDS = arrayOf(
            floatArrayOf(0.08f, -0.02f, -0.05f),
            floatArrayOf(-0.03f, 0.06f, 0.02f),
            floatArrayOf(0.02f, 0.04f, -0.06f),
            floatArrayOf(-0.05f, -0.01f, 0.07f),
            floatArrayOf(0.06f, -0.04f, 0.03f)
        )
        internal val COL_BANDS = arrayOf(
            floatArrayOf(0.04f, 0.01f, -0.03f),
            floatArrayOf(-0.02f, 0.05f, 0.02f),
            floatArrayOf(0.03f, -0.04f, 0.04f),
            floatArrayOf(-0.04f, -0.02f, 0.05f)
        )
        internal val COOL_VARIANTS = arrayOf(
            floatArrayOf(0.32f, 0.84f, 0.98f),
            floatArrayOf(0.45f, 0.75f, 0.99f),
            floatArrayOf(0.46f, 0.88f, 0.76f),
            floatArrayOf(0.62f, 0.64f, 0.98f),
            floatArrayOf(0.86f, 0.62f, 0.95f),
            floatArrayOf(0.95f, 0.7f, 0.45f),
            floatArrayOf(0.56f, 0.94f, 0.5f),
            floatArrayOf(0.94f, 0.58f, 0.78f)
        )
        internal val WARM_VARIANTS = arrayOf(
            floatArrayOf(0.98f, 0.56f, 0.34f),
            floatArrayOf(0.95f, 0.72f, 0.4f),
            floatArrayOf(0.98f, 0.45f, 0.5f),
            floatArrayOf(0.88f, 0.68f, 0.3f),
            floatArrayOf(0.94f, 0.55f, 0.74f),
            floatArrayOf(0.74f, 0.76f, 0.4f),
            floatArrayOf(0.7f, 0.58f, 0.9f),
            floatArrayOf(0.89f, 0.46f, 0.36f)
        )
        internal val BALANCED_VARIANTS = arrayOf(
            floatArrayOf(0.54f, 0.84f, 0.97f),
            floatArrayOf(0.91f, 0.64f, 0.42f),
            floatArrayOf(0.45f, 0.9f, 0.65f),
            floatArrayOf(0.89f, 0.56f, 0.84f),
            floatArrayOf(0.96f, 0.79f, 0.38f),
            floatArrayOf(0.59f, 0.63f, 0.99f),
            floatArrayOf(0.94f, 0.5f, 0.56f),
            floatArrayOf(0.7f, 0.88f, 0.5f)
        )
        internal val BIAS_NORMAL = floatArrayOf(0f, 0f, 0f)
        internal val BIAS_REINFORCED = floatArrayOf(0.04f, -0.02f, 0.03f)
        internal val BIAS_ARMORED = floatArrayOf(-0.02f, 0.04f, -0.01f)
        internal val BIAS_EXPLOSIVE = floatArrayOf(0.12f, -0.08f, -0.07f)
        internal val BIAS_UNBREAKABLE = floatArrayOf(-0.04f, -0.03f, 0.06f)
        internal val BIAS_MOVING = floatArrayOf(-0.01f, 0.08f, 0.03f)
        internal val BIAS_SPAWNING = floatArrayOf(0.03f, 0.01f, 0.08f)
        internal val BIAS_PHASE = floatArrayOf(0.08f, 0.06f, -0.04f)
        internal val BIAS_BOSS = floatArrayOf(0.12f, -0.07f, -0.05f)
        internal val BIAS_INVADER = floatArrayOf(0.03f, 0.08f, 0.1f)
    }

    var hitFlash = 0f
    // Dynamic brick properties
    var vx: Float = 0f  // Horizontal velocity for moving bricks
    var vy: Float = 0f  // Vertical velocity
    var phase: Int = 0  // Current phase for phase bricks
    var maxPhase: Int = 1  // Total phases for phase bricks
    var spawnCount: Int = 0  // Number of spawns left for spawning bricks
    var lastHitTime: Float = 0f  // Timestamp of last hit for special effects
    var fireFlash: Float = 0f  // Invader firing glow
    internal var cachedThemeName: String? = null
    internal var cachedHitPoints: Int = -1
    internal var cachedColor: FloatArray? = null
    val scoreValue: Int = when (type) {
        BrickType.NORMAL -> 50
        BrickType.REINFORCED -> 80
        BrickType.ARMORED -> 120
        BrickType.EXPLOSIVE -> 150
        BrickType.UNBREAKABLE -> 200
        BrickType.MOVING -> 75
        BrickType.SPAWNING -> 100
        BrickType.PHASE -> 180
        BrickType.BOSS -> 300
        BrickType.INVADER -> 120
    }

    val centerX: Float
        get() = x + width / 2f
    val centerY: Float
        get() = y + height / 2f

    fun applyHit(forceBreak: Boolean): Boolean {
        if (type == BrickType.UNBREAKABLE && !forceBreak) {
            hitFlash = 0.2f
            return false
        }

        val damage = if (forceBreak && type == BrickType.UNBREAKABLE) 2 else 1
        hitPoints -= damage
        hitFlash = 0.2f
        lastHitTime = System.nanoTime() / 1_000_000_000f  // Current time in seconds

        // Special brick behaviors
        when (type) {
            BrickType.PHASE -> {
                if (hitPoints <= 0) {
                    phase++
                    if (phase >= maxPhase) {
                        alive = false
                        return true
                    } else {
                        // Reset hitpoints for next phase
                        hitPoints = max(1, maxHitPoints / (phase + 1))
                        hitFlash = 0.5f  // Longer flash for phase change
                        return false
                    }
                }
            }
            BrickType.BOSS -> {
                if (hitPoints <= 0) {
                    phase++
                    if (phase >= maxPhase) {
                        alive = false
                        return true
                    } else {
                        // Boss maintains strength across phases
                        hitPoints = maxHitPoints
                        hitFlash = 0.8f  // Dramatic flash for boss phase
                        // Could add screen shake or special effects here
                        return false
                    }
                }
            }
            BrickType.SPAWNING -> {
                if (hitPoints <= 0) {
                    alive = false
                    // Spawning logic will be handled externally when brick is destroyed
                    return true
                }
            }
            else -> {
                if (hitPoints <= 0) {
                    alive = false
                    return true
                }
            }
        }
        return false
    }

    fun currentColor(theme: LevelTheme): FloatArray {
        if (hitFlash <= 0f && cachedThemeName == theme.name && cachedHitPoints == hitPoints) {
            cachedColor?.let { return it }
        }
        val base = theme.brickPalette[type] ?: theme.accent
        val durability = if (type == BrickType.UNBREAKABLE) {
            1f
        } else {
            (hitPoints.toFloat() / maxHitPoints.toFloat()).coerceIn(0.35f, 1f)
        }
        val variants = variantsForTheme(theme.name)
        val seed = (gridX * 73856093) xor (gridY * 19349663) xor (type.ordinal * 83492791) xor theme.name.hashCode()
        val tint = variants[positiveMod(seed, variants.size)]
        val typeBias = biasForType()
        val tintMix = tintMixForType()
        val diversity = diversityForType()
        val rowBand = ROW_BANDS[positiveMod(gridY, ROW_BANDS.size)]
        val colBand = COL_BANDS[positiveMod(gridX, COL_BANDS.size)]
        val bandScale = when (type) {
            BrickType.NORMAL -> 0.22f
            BrickType.INVADER -> 0.2f
            BrickType.BOSS -> 0.09f
            else -> 0.14f
        } * diversity
        val brightness = 0.84f + durability * 0.24f
        val damageWarmth = (1f - durability) * when (type) {
            BrickType.EXPLOSIVE, BrickType.BOSS -> 0.12f
            BrickType.PHASE -> 0.09f
            else -> 0.06f
        }
        val mixR = mix(base[0], tint[0], tintMix)
        val mixG = mix(base[1], tint[1], tintMix)
        val mixB = mix(base[2], tint[2], tintMix)
        val finalColor = floatArrayOf(
            (mixR * brightness + typeBias[0] + (rowBand[0] + colBand[0]) * bandScale + damageWarmth).coerceIn(0.05f, 0.98f),
            (mixG * brightness + typeBias[1] + (rowBand[1] + colBand[1]) * bandScale).coerceIn(0.05f, 0.98f),
            (mixB * brightness + typeBias[2] + (rowBand[2] + colBand[2]) * bandScale - damageWarmth * 0.45f).coerceIn(0.05f, 0.98f),
            1f
        )

        if (hitFlash <= 0f) {
            cachedThemeName = theme.name
            cachedHitPoints = hitPoints
            cachedColor = finalColor
            return finalColor
        }
        val flashBoost = (0.2f + hitFlash * 0.85f).coerceIn(0.2f, 0.52f)
        return floatArrayOf(
            min(1f, finalColor[0] + flashBoost),
            min(1f, finalColor[1] + flashBoost),
            min(1f, finalColor[2] + flashBoost),
            1f
        )
    }

    internal fun variantsForTheme(themeName: String): Array<FloatArray> {
        return when (themeName) {
            "Sunset", "Lava", "Ember" -> WARM_VARIANTS
            "Neon", "Cobalt", "Circuit", "Invaders", "Vapor" -> COOL_VARIANTS
            else -> BALANCED_VARIANTS
        }
    }



    internal fun biasForType(): FloatArray {
        return when (type) {
            BrickType.NORMAL -> BIAS_NORMAL
            BrickType.REINFORCED -> BIAS_REINFORCED
            BrickType.ARMORED -> BIAS_ARMORED
            BrickType.EXPLOSIVE -> BIAS_EXPLOSIVE
            BrickType.UNBREAKABLE -> BIAS_UNBREAKABLE
            BrickType.MOVING -> BIAS_MOVING
            BrickType.SPAWNING -> BIAS_SPAWNING
            BrickType.PHASE -> BIAS_PHASE
            BrickType.BOSS -> BIAS_BOSS
            BrickType.INVADER -> BIAS_INVADER
        }
    }

    internal fun tintMixForType(): Float {
        return when (type) {
            BrickType.NORMAL -> 0.34f
            BrickType.REINFORCED -> 0.24f
            BrickType.ARMORED -> 0.21f
            BrickType.EXPLOSIVE -> 0.28f
            BrickType.UNBREAKABLE -> 0.12f
            BrickType.MOVING -> 0.29f
            BrickType.SPAWNING -> 0.28f
            BrickType.PHASE -> 0.33f
            BrickType.BOSS -> 0.2f
            BrickType.INVADER -> 0.32f
        }
    }

    internal fun diversityForType(): Float {
        return when (type) {
            BrickType.NORMAL -> 1f
            BrickType.MOVING, BrickType.PHASE, BrickType.SPAWNING, BrickType.INVADER -> 0.9f
            BrickType.BOSS -> 0.65f
            BrickType.UNBREAKABLE -> 0.55f
            else -> 0.75f
        }
    }

    internal fun mix(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }

    internal fun positiveMod(value: Int, size: Int): Int {
        val mod = value % size
        return if (mod < 0) mod + size else mod
    }

    fun isNeighbor(other: Brick, radius: Int): Boolean {
        return abs(gridX - other.gridX) <= radius && abs(gridY - other.gridY) <= radius
    }
}

enum class BrickType { NORMAL, REINFORCED, ARMORED, EXPLOSIVE, UNBREAKABLE, MOVING, SPAWNING, PHASE, BOSS, INVADER }

data class PowerUp(
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    val speed: Float,
    val size: Float = 3.2f
)

enum class PowerUpType(val displayName: String, val color: FloatArray) {
    MULTI_BALL("Multi-ball", floatArrayOf(0.19f, 0.88f, 0.97f, 1f)),
    LASER("Laser", floatArrayOf(1f, 0.31f, 0.84f, 1f)),
    GUARDRAIL("Guardrail", floatArrayOf(1f, 0.78f, 0.34f, 1f)),
    LIFE("Extra Life", floatArrayOf(0.14f, 0.92f, 0.64f, 1f)),
    SHIELD("Shield", floatArrayOf(0.52f, 0.61f, 1f, 1f)),
    WIDE_PADDLE("Wide Paddle", floatArrayOf(0.98f, 0.62f, 0.2f, 1f)),
    SHRINK("Shrink", floatArrayOf(1f, 0.45f, 0.35f, 1f)),
    SLOW("Slow", floatArrayOf(0.63f, 0.76f, 1f, 1f)),
    OVERDRIVE("Overdrive", floatArrayOf(1f, 0.62f, 0.22f, 1f)),
    FIREBALL("Fireball", floatArrayOf(1f, 0.36f, 0.27f, 1f)),
    MAGNET("Magnet", floatArrayOf(0.8f, 0.4f, 1f, 1f)),
    GRAVITY_WELL("Gravity Well", floatArrayOf(0.4f, 0.6f, 1f, 1f)),
    BALL_SPLITTER("Ball Splitter", floatArrayOf(1f, 0.8f, 0.2f, 1f)),
    FREEZE("Freeze", floatArrayOf(0.3f, 0.8f, 1f, 1f)),
    PIERCE("Pierce", floatArrayOf(0.9f, 0.5f, 0.1f, 1f)),
    RICOCHET("Ricochet", floatArrayOf(0.6f, 0.8f, 1f, 1f)),
    TIME_WARP("Time Warp", floatArrayOf(0.4f, 0.9f, 0.7f, 1f)),
    DOUBLE_SCORE("2x Score", floatArrayOf(1f, 0.8f, 0.3f, 1f))
}

data class Beam(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val color: FloatArray
)

data class EnemyShot(
    var x: Float,
    var y: Float,
    val radius: Float,
    val vx: Float,
    val vy: Float,
    val color: FloatArray,
    val style: Int = 0,
    val wiggle: Float = 0f,
    val wobbleFreq: Float = 0f,
    var age: Float = 0f
)

data class TrailPoint(
    var x: Float,
    var y: Float,
    var radius: Float,
    var life: Float,
    val maxLife: Float
)

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    var life: Float,
    val color: FloatArray
)

data class ExplosionWave(
    var x: Float,
    var y: Float,
    var radius: Float,
    val color: FloatArray,
    var life: Float,
    val maxLife: Float,
    val speed: Float,
    val chainCount: Int = 1
)
