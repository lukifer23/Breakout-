package com.breakoutplus.game

import android.view.MotionEvent
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
    private val config: GameConfig,
    private val listener: GameEventListener,
    private val audio: GameAudioManager,
    private val logger: GameLogger? = null,
    private val dailyChallenges: MutableList<DailyChallenge>? = null,
    private val renderer: GameRenderer? = null
) {
    private val random = Random(System.nanoTime())
    private var settings: SettingsManager.Settings = config.settings
    private val balls = mutableListOf<Ball>()
    private val bricks = mutableListOf<Brick>()
    private val powerups = mutableListOf<PowerUp>()
    private val beams = mutableListOf<Beam>()
    private val enemyShots = mutableListOf<EnemyShot>()
    private val particles = mutableListOf<Particle>()
    private val waves = mutableListOf<ExplosionWave>()
    private val activeEffects = mutableMapOf<PowerUpType, Float>()

    private var worldWidth = 100f
    private var worldHeight = 160f

    private var paddle = Paddle(x = worldWidth / 2f, y = 8f, width = 22f, height = 2.6f)
    private var basePaddleWidth = paddle.width
    private var paddleVelocity = 0f

    private var state = GameState.READY
    private var stateBeforePause = state
    private var score = 0
    private var levelIndex = 0
    private var lives = config.mode.baseLives
    private var awaitingNextLevel = false
    private var timeRemaining = config.mode.timeLimitSeconds.toFloat()
    private var lastReportedSecond = -1
    private var elapsedSeconds = 0f
    private var levelStartTime = 0f
    private var runBricksBroken = 0
    private var runLivesLost = 0
    private var combo = 0
    private var comboTimer = 0f
    private var guardrailActive = false
    private var shieldCharges = 0
    private var laserCooldown = 0f
    private val laserCooldownDuration = 0.4f
    private var speedMultiplier = 1f
    private var timeWarpMultiplier = 1f
    private var fireballActive = false
    private var magnetActive = false
    private var gravityWellActive = false
    private var freezeActive = false
    private var pierceActive = false
    private var explosiveTipShown = false
    private var lastPowerupStatus = ""
    private var lastPowerupSnapshot: List<PowerupStatus> = emptyList()
    private var lastComboReported = 0
    private var lostLifeThisLevel = false
    private var laserTipShown = false
    private var magnetTipShown = false
    private var magnetCatchTipShown = false
    private var godModeTipShown = false
    private val powerupTipShown = mutableSetOf<PowerUpType>()
    private var invaderDirection = 1f
    private var invaderSpeed = 6f
    private var invaderBaseSpeed = 6f
    private var invaderShotTimer = 0f
    private var invaderShotCooldown = 1.6f
    private var invaderBaseShotCooldown = 1.6f
    private val invaderTelegraphLead = 0.28f
    private var invaderWaveStyle = 0
    private var invaderVolleyTimer = 0f
    private var invaderPauseTimer = 0f
    private var invaderBurstCount = 0
    private var invaderShield = 0f
    private var invaderShieldMax = 0f
    private var invaderShieldAlerted = false
    private var invaderShieldCritical = false
    private var invaderTelegraphKey: Long? = null
    private var invaderTotal = 0
    private val invaderBricks = mutableListOf<Brick>()
    private var invaderFormationOffset = 0f
    private var invaderRowPhase = 0f
    private var invaderRowDrift = 0.75f
    private var invaderRowPhaseOffset = 0.5f
    private val invaderFormationCompression = 0.74f
    private var invaderTurnSoundCooldown = 0f
    private val invaderTurnSoundMinInterval = 0.32f
    private var shieldHitPulse = 0f
    private var shieldHitX = 0f
    private var shieldHitColor = floatArrayOf(0.8f, 0.95f, 1f, 1f)
    private val tempColor = FloatArray(4)
    private val aliveInvaderBuffer = ArrayList<Brick>(72)
    private var shieldBreakPulse = 0f
    private var powerupCollectionPulse = 0f
    private var powerupDropsSinceLaser = 0
    private val recentPowerups = ArrayDeque<PowerUpType>()
    private val recentPowerupLimit = 4
    private var powerupsSinceOffense = 0
    private var powerupsSinceDefense = 0
    private var powerupsSinceControl = 0
    private var aimNormalized = 0f
    private var aimNormalizedTarget = 0f
    private var aimAngle = (Math.PI.toFloat() * 0.5f)
    private var aimHasInput = false
    private var isDragging = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var touchWorldX = 0f
    private var touchWorldY = 0f
    private val aimSmoothingRate = 18f
    private val aimCenterDeadZone = 0.018f
    private var debugAutoPlayEnabled = false
    private var debugAutoPlayActionTimer = 0f
    private var debugAutoPlayWave = 0f
    private var volleyBallCount = 3
    private var volleyQueuedBalls = 0
    private var volleyLaunchTimer = 0f
    private var volleyTurnActive = false
    private var volleyTurnCount = 0
    private var volleyAdvanceRows = 0
    private var volleyLaunchX = worldWidth * 0.5f
    private var volleyReturnAnchorX = Float.NaN
    private var volleyReturnSumX = 0f
    private var volleyReturnCount = 0

    private var tunnelShotsFired = 0
    private var tunnelGateFlash = 0f

    // Spatial hash for brick collisions
    private val spatialHashCellSize = 8f
    private val spatialHash = mutableMapOf<Pair<Int, Int>, MutableList<Brick>>()
    private val nearbyBrickBuffer = ArrayList<Brick>(96)
    private val nearbyBrickSeen = HashSet<Brick>(96)
    private var spatialHashDirty = true
    private var dynamicBrickLayout = false
    private var pendingInitialLayoutRetune = true
    private var lastResizeWidthPx = 0
    private var lastResizeHeightPx = 0

    private var theme: LevelTheme = LevelThemes.DEFAULT
    private var themePool: MutableList<LevelTheme> = LevelThemes.baseThemes().toMutableList()
    private var currentLayout: LevelFactory.LevelLayout? = null
    private var currentAspectRatio = worldHeight / worldWidth
    private var brickAreaTopRatio = 0.92f
    private var brickAreaBottomRatio = 0.52f
    private var brickSpacing = 0.42f
    private var layoutRowBoost = 0
    private var layoutColBoost = 0
    private var invaderScale = 1f
    private var globalBrickScale = 0.9f
    private var levelClearFlash = 0f
    private var renderTimeSeconds = 0f
    private val hitFlashDecayRate = 2.0f
    private val maxParticles = 240
    private val maxWaves = 10
    private var trailLife = 0.28f
    private var maxTrailPoints = 8
    private var cosmeticTier = config.unlocks.cosmeticTier
    private var rewardScoreMultiplier = 0f
    private var streakBonusRemaining = 0
    private var streakBonusActive = false
    private val streakBonusPerBrick = 20
    private val aimMinAngle = 0.30f

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
        } else if (state == GameState.READY || state == GameState.PAUSED) {
            relayoutBricks()
        }
        lastResizeWidthPx = width
        lastResizeHeightPx = height
    }

    private fun resolveBasePaddleWidth(aspectRatio: Float): Float {
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
        updateTimers(dt)
        updatePaddle(delta)
        updateAim(delta)
        updateBricks(dt)
        updateEffects(delta)

        if (state == GameState.READY) {
            attachBallToPaddle()
            // Show explosive brick tip if not shown yet and explosive bricks exist
            if (!explosiveTipShown && bricks.any { it.type == BrickType.EXPLOSIVE && it.alive }) {
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
            checkLevelCompletion()
            if (config.mode == GameMode.VOLLEY && state == GameState.RUNNING) {
                resolveVolleyTurnIfReady()
            }
        }
        if (shieldHitPulse > 0f) {
            shieldHitPulse = max(0f, shieldHitPulse - dt * 2.6f)
        }
        if (shieldBreakPulse > 0f) {
            shieldBreakPulse = max(0f, shieldBreakPulse - dt * 2.4f)
        }
        if (powerupCollectionPulse > 0f) {
            powerupCollectionPulse = max(0f, powerupCollectionPulse - dt * 2.2f)
        }
        if (tunnelGateFlash > 0f) {
            tunnelGateFlash = max(0f, tunnelGateFlash - dt * 2.1f)
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

    private fun updateDebugAutoPlay(dt: Float) {
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
                if (magnetActive && balls.any { it.stuckToPaddle } && debugAutoPlayActionTimer <= 0f) {
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

    fun render(renderer: Renderer2D) {
        renderer.setWorldSize(worldWidth, worldHeight)
        renderTimeSeconds = System.nanoTime() / 1_000_000_000f
        // Enhanced background with subtle gradient and flash effect
        val flashIntensity = levelClearFlash * 0.8f
        val bgTop = if (flashIntensity > 0f) {
            adjustColor(theme.background, 1.1f + flashIntensity, 1f)
        } else {
            adjustColor(theme.background, 1.1f, 1f)
        }
        val bgBottom = if (flashIntensity > 0f) {
            adjustColor(theme.background, 0.9f + flashIntensity, 1f)
        } else {
            adjustColor(theme.background, 0.9f, 1f)
        }

        // Draw gradient background (top to bottom)
        val gradientSteps = 20
        val stepHeight = worldHeight / gradientSteps
        for (i in 0 until gradientSteps) {
            val y = i * stepHeight
            val ratio = i.toFloat() / gradientSteps.toFloat()
            renderer.drawRect(
                0f,
                y,
                worldWidth,
                stepHeight,
                fillColor(
                    tempColor,
                    bgTop[0] * (1f - ratio) + bgBottom[0] * ratio,
                    bgTop[1] * (1f - ratio) + bgBottom[1] * ratio,
                    bgTop[2] * (1f - ratio) + bgBottom[2] * ratio,
                    1f
                )
            )
        }

        // Add theme-specific background effects
        val time = renderTimeSeconds
        when (theme.name) {
            "Neon" -> {
                // Animated grid pattern
                val gridSize = 8f
                for (x in 0 until (worldWidth / gridSize).toInt()) {
                    for (y in 0 until (worldHeight / gridSize).toInt()) {
                        if ((x + y) % 2 == 0) {
                            val alpha = (kotlin.math.sin(time * 2f + x * 0.5f + y * 0.3f) * 0.5f + 0.5f) * 0.1f
                            renderer.drawRect(
                                x * gridSize,
                                y * gridSize,
                                gridSize,
                                gridSize,
                                fillColor(tempColor, 0.3f, 0.9f, 1f, alpha)
                            )
                        }
                    }
                }
            }
            "Sunset" -> {
                // Floating particles
                for (i in 0 until 15) {
                    val x = (kotlin.math.sin(time * 0.5f + i) * 0.5f + 0.5f) * worldWidth
                    val y = (kotlin.math.cos(time * 0.3f + i * 0.7f) * 0.5f + 0.5f) * worldHeight
                    val size = 1f + kotlin.math.sin(time * 2f + i) * 0.5f
                    renderer.drawCircle(x, y, size, fillColor(tempColor, 1f, 0.6f, 0.3f, 0.3f))
                }
            }
            "Aurora" -> {
                // Wave patterns
                for (i in 0 until 8) {
                    val waveY = worldHeight * 0.3f + kotlin.math.sin(time + i * 0.8f) * worldHeight * 0.2f
                    val alpha = (kotlin.math.sin(time * 1.5f + i) * 0.5f + 0.5f) * 0.15f
                    renderer.drawRect(
                        0f,
                        waveY,
                        worldWidth,
                        2f,
                        fillColor(tempColor, 0.3f, 0.8f, 0.5f, alpha)
                    )
                }
            }
            "Invaders" -> {
                // Starfield background
                for (i in 0 until 36) {
                    val seed = i * 37 + 13
                    val rx = kotlin.math.sin(time * 0.08f + seed) * 0.5f + 0.5f
                    val ry = kotlin.math.cos(time * 0.07f + seed * 1.7f) * 0.5f + 0.5f
                    val x = rx * worldWidth
                    val y = ry * worldHeight
                    val twinkle = (kotlin.math.sin(time * 2.2f + seed) * 0.5f + 0.5f)
                    val alpha = 0.12f + twinkle * 0.25f
                    val size = 0.3f + twinkle * 0.4f
                    renderer.drawCircle(x, y, size, fillColor(tempColor, 0.6f, 0.8f, 1f, alpha))
                }
            }
        }

        if (config.mode == GameMode.VOLLEY) {
            val turnPulse = (kotlin.math.sin(time * 2.4f) * 0.5f + 0.5f)
            val accentAlpha = 0.08f + turnPulse * 0.08f
            val laneY = paddle.y + paddle.height * 0.5f + 1.8f
            val warningBandHeight = (worldHeight * 0.09f).coerceIn(3f, 7f)
            renderer.drawRect(
                0f,
                0f,
                worldWidth,
                warningBandHeight,
                fillColor(tempColor, 1f, 0.43f, 0.2f, 0.06f + turnPulse * 0.05f)
            )
            renderer.drawRect(
                0f,
                laneY,
                worldWidth,
                0.3f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.2f + accentAlpha)
            )
            val scanYOffset = laneY + worldHeight * 0.02f
            for (i in 0 until 7) {
                val progress = ((time * (0.12f + i * 0.01f) + i * 0.19f) % 1f + 1f) % 1f
                val width = worldWidth * (0.06f + (i % 3) * 0.01f)
                val x = progress * (worldWidth + width) - width
                val alpha = (0.04f + turnPulse * 0.03f - i * 0.004f).coerceAtLeast(0.015f)
                renderer.drawRect(
                    x,
                    scanYOffset + i * 0.42f,
                    width,
                    0.12f,
                    fillColor(tempColor, 0.95f, 0.58f, 0.22f, alpha)
                )
            }
            for (i in 0 until 6) {
                val y = worldHeight * 0.58f + i * worldHeight * 0.055f
                val alpha = (0.03f + (i % 2) * 0.015f + turnPulse * 0.01f).coerceIn(0.02f, 0.08f)
                renderer.drawRect(
                    0f,
                    y,
                    worldWidth,
                    0.22f,
                    fillColor(tempColor, theme.paddle[0], theme.paddle[1], theme.paddle[2], alpha)
                )
            }
        }
        if (config.mode == GameMode.TUNNEL && tunnelGateFlash > 0f) {
            val gatePulse = tunnelGateFlash.coerceIn(0f, 1f)
            val gateWidth = (worldWidth * 0.16f).coerceIn(10f, 18f)
            val gateX = worldWidth * 0.5f - gateWidth * 0.5f
            val gateY = worldHeight * 0.52f
            val gateHeight = worldHeight * 0.36f
            renderer.drawRect(
                gateX,
                gateY,
                gateWidth,
                gateHeight,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.08f + gatePulse * 0.17f)
            )
            renderer.drawRect(
                gateX + gateWidth * 0.24f,
                gateY,
                gateWidth * 0.52f,
                gateHeight,
                fillColor(tempColor, 1f, 0.95f, 0.85f, 0.05f + gatePulse * 0.12f)
            )
        }

        if (gravityWellActive) {
            val centerX = worldWidth * 0.5f
            val centerY = worldHeight * 0.62f
            for (i in 0 until 6) {
                val radius = 2f + i * 2.4f
                val angle = time * (0.8f + i * 0.12f)
                val x = centerX + kotlin.math.cos(angle) * radius
                val y = centerY + kotlin.math.sin(angle) * radius
                val alpha = (0.18f - i * 0.02f).coerceAtLeast(0.05f)
                renderer.drawCircle(x, y, 0.6f + i * 0.12f, fillColor(tempColor, 0.45f, 0.65f, 1f, alpha))
            }
        }

        if (activeEffects.containsKey(PowerUpType.FREEZE) || activeEffects.containsKey(PowerUpType.SLOW)) {
            val chillAlpha = if (activeEffects.containsKey(PowerUpType.FREEZE)) 0.12f else 0.08f
            renderer.drawRect(0f, 0f, worldWidth, worldHeight, fillColor(tempColor, 0.35f, 0.6f, 1f, chillAlpha))
        }

        if (guardrailActive) {
            val pulse = (kotlin.math.sin(time * 3f) * 0.5f + 0.5f)
            renderer.drawRect(
                0f,
                2f,
                worldWidth,
                0.6f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.5f + pulse * 0.4f)
            )
        }

        if (config.mode.invaders && invaderTelegraphKey != null) {
            val target = invaderBricks.firstOrNull { it.alive && invaderKey(it) == invaderTelegraphKey }
            if (target != null) {
                val alpha = ((invaderTelegraphLead - invaderShotTimer).coerceIn(0f, invaderTelegraphLead) / invaderTelegraphLead)
                val pulse = (kotlin.math.sin(time * 16f) * 0.5f + 0.5f)
                val beamAlpha = (0.15f + alpha * 0.5f + pulse * 0.2f).coerceIn(0f, 0.8f)
                val beamWidth = 0.5f + alpha * 0.8f
                val beamX = target.centerX - beamWidth / 2f
                val beamY = target.y - worldHeight * 0.02f
                val beamHeight = target.y - paddle.y + paddle.height * 0.6f
                renderer.drawRect(
                    beamX,
                    paddle.y + paddle.height * 0.2f,
                    beamWidth,
                    beamHeight,
                    fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], beamAlpha)
                )
            }
        }

        for (brick in bricks) {
            if (!brick.alive) continue
            if (!renderer.isRectVisible(brick.x, brick.y, brick.width, brick.height)) continue
            val color = if (config.mode == GameMode.TUNNEL && brick.type == BrickType.UNBREAKABLE) {
                // Keep tunnel walls visually distinct from regular bricks.
                floatArrayOf(0.76f, 0.83f, 0.93f, 1f)
            } else {
                brick.currentColor(theme)
            }

            if (brick.type == BrickType.INVADER) {
                drawInvaderShip(renderer, brick, color)
                if (brick.maxHitPoints >= 2) {
                    val armor = adjustColor(color, 0.82f, 0.9f)
                    val count = if (brick.maxHitPoints >= 3) 2 else 1
                    drawStripe(renderer, brick, armor, count)
                if (brick.maxHitPoints >= 3) {
                    val core = adjustColor(color, 1.4f, 0.9f)
                    renderer.drawCircle(brick.centerX, brick.centerY, brick.height * 0.08f, core)
                }
            }
            continue
        }

            // 3D depth effect: base shadow
            val shadowOffset = brick.width * 0.02f
            val shadowColor = adjustColor(color, 0.4f, 0.3f)
            renderer.drawRect(brick.x + shadowOffset, brick.y + shadowOffset, brick.width, brick.height, shadowColor)

            // Main brick body
            renderer.drawRect(brick.x, brick.y, brick.width, brick.height, color)

            // 3D highlights and bevels
            val highlight = adjustColor(color, 1.3f, 1f)
            val midtone = adjustColor(color, 0.9f, 1f)
            val lowlight = adjustColor(color, 0.6f, 1f)

            // Top bevel (highlight)
            val topBevelHeight = brick.height * 0.08f
            renderer.drawRect(brick.x, brick.y, brick.width, topBevelHeight, highlight)

            // Left bevel (highlight)
            val leftBevelWidth = brick.width * 0.06f
            renderer.drawRect(brick.x, brick.y, leftBevelWidth, brick.height, midtone)

            // Bottom bevel (lowlight/shadow)
            val bottomBevelHeight = brick.height * 0.1f
            renderer.drawRect(brick.x, brick.y + brick.height - bottomBevelHeight, brick.width, bottomBevelHeight, lowlight)

            // Right bevel (lowlight/shadow)
            val rightBevelWidth = brick.width * 0.08f
            renderer.drawRect(brick.x + brick.width - rightBevelWidth, brick.y, rightBevelWidth, brick.height, lowlight)

            when (brick.type) {
                BrickType.REINFORCED -> drawStripe(renderer, brick, adjustColor(color, 0.85f, 1f), 1)
                BrickType.ARMORED -> drawStripe(renderer, brick, adjustColor(color, 0.78f, 1f), 2)
                BrickType.UNBREAKABLE -> {
                    drawStripe(renderer, brick, adjustColor(color, 0.66f, 1f), 3)
                    if (config.mode == GameMode.TUNNEL) {
                        val lock = adjustColor(color, 0.48f, 1f)
                        val cap = adjustColor(color, 1.12f, 0.95f)
                        val barWidth = brick.width * 0.14f
                        val barHeight = brick.height * 0.5f
                        renderer.drawRect(
                            brick.centerX - barWidth * 0.5f,
                            brick.centerY - barHeight * 0.5f,
                            barWidth,
                            barHeight,
                            lock
                        )
                        renderer.drawRect(
                            brick.centerX - brick.width * 0.24f,
                            brick.centerY - brick.height * 0.06f,
                            brick.width * 0.48f,
                            brick.height * 0.12f,
                            cap
                        )
                    }
                }
                BrickType.MOVING -> {
                    // Add movement indicator
                    val indicatorColor = adjustColor(color, 1.3f, 0.8f)
                    renderer.drawRect(brick.x + brick.width * 0.1f, brick.y + brick.height * 0.1f,
                                    brick.width * 0.8f, brick.height * 0.05f, indicatorColor)
                }
                BrickType.SPAWNING -> {
                    // Add spawn indicator (dots)
                    val dotColor = adjustColor(color, 1.2f, 0.9f)
                    val dotSize = brick.width * 0.08f
                    for (i in 0 until brick.spawnCount) {
                        val dotX = brick.x + brick.width * 0.2f + i * brick.width * 0.15f
                        val dotY = brick.y + brick.height * 0.85f
                        renderer.drawCircle(dotX, dotY, dotSize, dotColor)
                    }
                }
                BrickType.PHASE -> {
                    // Phase indicator (colored bars)
                    val phaseColor = when (brick.phase) {
                        0 -> fillColor(tempColor, 0f, 1f, 0f, 0.8f) // Green
                        1 -> fillColor(tempColor, 1f, 1f, 0f, 0.8f) // Yellow
                        else -> fillColor(tempColor, 1f, 0f, 0f, 0.8f) // Red
                    }
                    val barHeight = brick.height * 0.1f
                    val barY = brick.y + brick.height - barHeight
                    renderer.drawRect(brick.x, barY, brick.width, barHeight, phaseColor)
                }
                BrickType.BOSS -> {
                    // Boss indicator (pulsing border)
                    val pulse = (kotlin.math.sin(time * 4f) * 0.5f + 0.5f) * 0.3f + 0.7f
                    val bossColor = adjustColor(color, pulse, 1f)
                    val borderWidth = brick.width * 0.05f
                    // Draw border by drawing slightly larger rect underneath
                    renderer.drawRect(brick.x - borderWidth, brick.y - borderWidth,
                                    brick.width + borderWidth * 2, brick.height + borderWidth * 2, bossColor)
                }
                else -> Unit
            }
        }

        powerups.forEach { power ->
            renderPowerup(renderer, power)

            // Magnet indicator: show attraction line when magnet is active
            if (magnetActive) {
                val dx = paddle.x - power.x
                val dy = paddle.y + paddle.height / 2f - power.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance > 0.1f && distance < 25f) { // Only show for nearby powerups
                    val steps = 8
                    val lineAlpha = (0.15f * (1f - distance / 25f)).coerceIn(0.02f, 0.15f)
                    val lineColor = fillColor(tempColor, 0.8f, 0.4f, 1f, lineAlpha)
                    repeat(steps) { step ->
                        val t = step.toFloat() / (steps - 1)
                        val x = power.x + dx * t
                        val y = power.y + dy * t
                        val dotSize = 0.15f + (1f - t) * 0.1f
                        renderer.drawCircle(x, y, dotSize, lineColor)
                    }
                }
            }
        }

        beams.forEach { beam ->
            renderer.drawBeam(beam.x, beam.y, beam.width, beam.height, beam.color)
        }

        enemyShots.forEach { shot ->
            val speed = kotlin.math.abs(shot.vy)
            val glow = adjustColor(shot.color, 1.2f + speed * 0.002f, 0.5f)
            renderer.drawCircle(shot.x, shot.y, shot.radius * 1.9f, glow)
            val trailLen = when (shot.style) {
                1 -> 5.0f
                2 -> 4.2f
                else -> if (shot.wiggle > 0f) 3.8f else 3.2f
            }
            val trailColor = when (shot.style) {
                1 -> fillColor(tempColor, 1f, 0.85f, 0.55f, 0.45f)
                2 -> fillColor(tempColor, 0.85f, 0.55f, 1f, 0.4f)
                else -> fillColor(tempColor, shot.color[0], shot.color[1], shot.color[2], 0.35f)
            }
            renderer.drawRect(
                shot.x - shot.radius * 0.28f,
                shot.y + shot.radius * 0.6f,
                shot.radius * 0.56f,
                shot.radius * trailLen,
                trailColor
            )
            when (shot.style) {
                1 -> {
                    renderer.drawRect(
                        shot.x - shot.radius * 0.4f,
                        shot.y - shot.radius * 1.2f,
                        shot.radius * 0.8f,
                        shot.radius * 2.4f,
                        shot.color
                    )
                    renderer.drawCircle(
                        shot.x,
                        shot.y + shot.radius * 1.1f,
                        shot.radius * 0.6f,
                        adjustColor(shot.color, 1.4f, 0.8f)
                    )
                }
                2 -> {
                    renderer.drawCircle(shot.x, shot.y, shot.radius * 1.35f, adjustColor(shot.color, 1.4f, 0.45f))
                    renderer.drawCircle(shot.x, shot.y, shot.radius * 0.65f, shot.color)
                }
                else -> {
                    renderer.drawCircle(shot.x, shot.y, shot.radius, shot.color)
                }
            }
        }

        if (config.mode.invaders && invaderShieldMax > 0f) {
            val ratio = (invaderShield / invaderShieldMax).coerceIn(0f, 1f)
            val pulse = if (invaderShieldCritical) (kotlin.math.sin(time * 6f) * 0.5f + 0.5f) else 0f
            val shieldY = paddle.y + paddle.height * 1.15f
            val thickness = 0.6f + ratio * 0.5f
            val alpha = (0.15f + ratio * 0.35f + shieldHitPulse * 0.25f + pulse * 0.2f).coerceIn(0.1f, 0.75f)
            val baseColor = if (invaderShieldCritical) {
                floatArrayOf(1f, 0.45f, 0.45f, alpha)
            } else {
                floatArrayOf(0.45f, 0.9f, 1f, alpha)
            }
            val shieldX = worldWidth * 0.06f
            val shieldWidth = worldWidth * 0.88f
            renderer.drawRect(shieldX, shieldY - thickness / 2f, shieldWidth, thickness, baseColor)

            val segments = 10
            val segmentWidth = shieldWidth / segments
            for (i in 0 until segments) {
                val shimmer = (kotlin.math.sin(time * 3.2f + i) * 0.4f + 0.6f).coerceIn(0.3f, 1f)
                val segAlpha = (alpha * shimmer).coerceIn(0f, 0.8f)
                val segX = shieldX + i * segmentWidth + 0.35f
                renderer.drawRect(
                    segX,
                    shieldY - thickness * 0.28f,
                    segmentWidth - 0.7f,
                    thickness * 0.56f,
                    fillColor(tempColor, baseColor[0], baseColor[1], baseColor[2], segAlpha)
                )
            }

            if (shieldHitPulse > 0f) {
                val ringAlpha = (0.4f * shieldHitPulse).coerceIn(0f, 0.6f)
                val ringX = shieldHitX.coerceIn(shieldX, shieldX + shieldWidth)
                renderer.drawCircle(
                    ringX,
                    shieldY + thickness * 0.15f,
                    1.2f + 2.2f * shieldHitPulse,
                    fillColor(tempColor, baseColor[0], baseColor[1], baseColor[2], ringAlpha)
                )
            }

            if (shieldBreakPulse > 0f) {
                val breakAlpha = (shieldBreakPulse * 0.55f).coerceIn(0f, 0.7f)
                renderer.drawRect(
                    shieldX,
                    shieldY - thickness,
                    shieldWidth,
                    thickness * 2.2f,
                    fillColor(tempColor, 1f, 0.4f, 0.4f, breakAlpha)
                )
            }
        }

        waves.forEach { wave ->
            // Render explosion wave with gradient effect
            val lifeRatio = wave.life / wave.maxLife
            val baseAlpha = (lifeRatio * 0.7f).coerceIn(0f, 0.7f)

            // Outer ring (most transparent)
            renderer.drawCircle(
                wave.x, wave.y, wave.radius,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha * 0.3f)
            )

            // Middle ring
            renderer.drawCircle(
                wave.x, wave.y, wave.radius * 0.75f,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha * 0.6f)
            )

            // Inner core (most opaque)
            renderer.drawCircle(
                wave.x, wave.y, wave.radius * 0.5f,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha)
            )
        }
        particles.forEach { particle ->
            if (renderer.isCircleVisible(particle.x, particle.y, particle.radius)) {
                renderer.drawCircleBatch(particle.x, particle.y, particle.radius, particle.color)
            }
        }
        renderer.flushCircleBatch()

        if (state == GameState.READY || balls.any { it.stuckToPaddle }) {
            renderAimGuide(renderer)
        }

        if (activeEffects.containsKey(PowerUpType.LASER) || shieldCharges > 0) {
            val glowAlpha = if (activeEffects.containsKey(PowerUpType.LASER)) 0.55f else 0.35f
            renderer.drawRect(
                paddle.x - paddle.width / 2f - 1.2f,
                paddle.y - paddle.height / 2f - 0.6f,
                paddle.width + 2.4f,
                paddle.height + 1.2f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], glowAlpha)
            )
        }
        renderer.drawRect(paddle.x - paddle.width / 2f, paddle.y - paddle.height / 2f, paddle.width, paddle.height, theme.paddle)
        if (cosmeticTier >= 2) {
            renderer.drawRect(
                paddle.x - paddle.width / 2f,
                paddle.y + paddle.height * 0.38f,
                paddle.width,
                paddle.height * 0.08f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.6f)
            )
        }
        if (shieldHitPulse > 0f) {
            val pulseAlpha = (shieldHitPulse * 0.65f).coerceIn(0f, 0.65f)
            val pulseWidth = paddle.width + 3.2f * shieldHitPulse
            val pulseHeight = paddle.height + 1.4f * shieldHitPulse
            renderer.drawRect(
                paddle.x - pulseWidth / 2f,
                paddle.y - pulseHeight / 2f,
                pulseWidth,
                pulseHeight,
                fillColor(tempColor, shieldHitColor[0], shieldHitColor[1], shieldHitColor[2], pulseAlpha)
            )
            val hitX = shieldHitX.coerceIn(paddle.x - paddle.width / 2f, paddle.x + paddle.width / 2f)
            renderer.drawCircle(
                hitX,
                paddle.y + paddle.height / 2f + 0.6f,
                0.8f + 1.4f * shieldHitPulse,
                fillColor(tempColor, shieldHitColor[0], shieldHitColor[1], shieldHitColor[2], pulseAlpha)
            )
        }

        if (powerupCollectionPulse > 0f) {
            val pulseAlpha = (powerupCollectionPulse * 0.5f).coerceIn(0f, 0.5f)
            val pulseWidth = paddle.width + 2.8f * powerupCollectionPulse
            val pulseHeight = paddle.height + 1.6f * powerupCollectionPulse
            renderer.drawRect(
                paddle.x - pulseWidth / 2f,
                paddle.y - pulseHeight / 2f,
                pulseWidth,
                pulseHeight,
                fillColor(tempColor, 0.9f, 0.95f, 1f, pulseAlpha)
            )
        }

        balls.forEach { ball ->
            val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            val glowStrength = (speed / 70f).coerceIn(0.2f, 0.55f) + cosmeticTier * 0.04f
            val glowBase = when {
                ball.isFireball -> PowerUpType.FIREBALL.color
                pierceActive -> PowerUpType.PIERCE.color
                else -> theme.accent
            }

            ball.trail.forEach { point ->
                val lifeRatio = (point.life / point.maxLife).coerceIn(0f, 1f)
                val alpha = lifeRatio * (0.35f + cosmeticTier * 0.06f)
                renderer.drawCircle(
                    point.x,
                    point.y,
                    point.radius * lifeRatio,
                    fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], alpha)
                )
                if (cosmeticTier >= 2) {
                    renderer.drawCircle(
                        point.x,
                        point.y,
                        point.radius * lifeRatio * 0.6f,
                        fillColor(tempColor, theme.paddle[0], theme.paddle[1], theme.paddle[2], alpha * 0.6f)
                    )
                }
            }

            renderer.drawCircle(
                ball.x,
                ball.y,
                ball.radius * 1.8f,
                fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], glowStrength)
            )
            if (cosmeticTier >= 3) {
                renderer.drawCircle(
                    ball.x,
                    ball.y,
                    ball.radius * 2.4f,
                    fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], 0.18f + cosmeticTier * 0.04f)
                )
            }
            renderer.drawCircle(ball.x, ball.y, ball.radius, ball.color)
        }
    }

    private fun renderPowerup(renderer: Renderer2D, power: PowerUp) {
        val time = renderTimeSeconds
        val isNegative = power.type == PowerUpType.SHRINK || power.type == PowerUpType.OVERDRIVE
        val pulseSpeed = if (isNegative) 4f else 3f // Faster pulse for negative powerups
        val pulse = (kotlin.math.sin(time * pulseSpeed) * 0.5f + 0.5f)
        val wobble = kotlin.math.sin(time * 2.1f) * 0.03f // Gentle wobble rotation effect
        val size = power.size * (0.9f + pulse * 0.12f)

        // Enhanced outer glow effect - red tint for negative powerups
        val glowColor = if (isNegative) {
            fillColor(tempColor, 0.8f + pulse * 0.2f, 0.2f, 0.2f, 0.6f + pulse * 0.2f)
        } else {
            adjustColor(power.type.color, 0.25f + pulse * 0.2f, 0.6f)
        }
        renderer.drawRect(power.x - size * 0.6f, power.y - size * 0.6f, size * 1.2f, size * 1.2f, glowColor)
        // Rotating ring effect - simple pulsing ring
        val ringPulse = (kotlin.math.sin(time * 2.5f) * 0.5f + 0.5f)
        val ringSize = size * (0.75f + ringPulse * 0.15f)
        val ringAlpha = 0.12f + pulse * 0.08f
        renderer.drawCircle(
            power.x,
            power.y,
            ringSize,
            fillColor(tempColor, power.type.color[0], power.type.color[1], power.type.color[2], ringAlpha)
        )

        val innerRingAlpha = 0.08f + pulse * 0.12f
        renderer.drawCircle(
            power.x,
            power.y,
            size * 0.62f,
            fillColor(tempColor, power.type.color[0], power.type.color[1], power.type.color[2], innerRingAlpha)
        )

        // Main powerup body with gradient
        val outer = adjustColor(power.type.color, 0.7f, 1f)
        val inner = adjustColor(power.type.color, 1.1f + pulse * 0.05f, 1f)

        // Draw with rounded appearance using multiple rects
        val cornerInset = size * 0.1f
        val x = power.x - size / 2f + wobble * size * 0.5f
        val y = power.y - size / 2f + wobble * size * 0.3f
        renderer.drawRect(x, y, size, size, outer)
        renderer.drawRect(x + cornerInset, y + cornerInset, size - cornerInset * 2f, size - cornerInset * 2f, inner)
        val highlight = fillColor(tempColor, 1f, 1f, 1f, 0.18f + pulse * 0.12f)
        renderer.drawRect(x + size * 0.12f, y + size * 0.62f, size * 0.76f, size * 0.18f, highlight)

        // Add subtle glow effect for better visibility
        val outerGlowColor = adjustColor(power.type.color, 1.2f, 0.4f)
        renderer.drawCircle(power.x, power.y, size * 1.4f, outerGlowColor)

        val glyph = adjustColor(power.type.color, 1.5f, 0.95f)
        val glyphSoft = adjustColor(power.type.color, 1.15f, 0.78f)
        val outlineColor = adjustColor(power.type.color, 1.8f, 0.3f)
        renderer.drawCircle(power.x, power.y, size * 0.95f, outlineColor)

        when (power.type) {
            PowerUpType.MULTI_BALL -> {
                renderer.drawCircle(power.x, power.y + size * 0.16f, size * 0.12f, glyph)
                renderer.drawCircle(power.x - size * 0.14f, power.y - size * 0.08f, size * 0.12f, glyph)
                renderer.drawCircle(power.x + size * 0.14f, power.y - size * 0.08f, size * 0.12f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.22f, size * 0.04f, size * 0.44f, glyphSoft)
            }
            PowerUpType.LASER -> {
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.26f, size * 0.08f, size * 0.52f, glyph)
                renderer.drawRect(power.x + size * 0.10f, power.y - size * 0.26f, size * 0.08f, size * 0.52f, glyph)
                renderer.drawCircle(power.x - size * 0.14f, power.y + size * 0.28f, size * 0.06f, glyphSoft)
                renderer.drawCircle(power.x + size * 0.14f, power.y + size * 0.28f, size * 0.06f, glyphSoft)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.04f, size * 0.12f, size * 0.08f, glyphSoft)
            }
            PowerUpType.GUARDRAIL -> {
                renderer.drawRect(power.x - size * 0.32f, power.y - size * 0.02f, size * 0.64f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.26f, power.y - size * 0.2f, size * 0.08f, size * 0.18f, glyphSoft)
                renderer.drawRect(power.x + size * 0.18f, power.y - size * 0.2f, size * 0.08f, size * 0.18f, glyphSoft)
                renderer.drawRect(power.x - size * 0.22f, power.y + size * 0.08f, size * 0.44f, size * 0.06f, glyphSoft)
            }
            PowerUpType.SHIELD -> {
                renderer.drawCircle(power.x, power.y + size * 0.1f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.02f, size * 0.4f, size * 0.22f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y - size * 0.2f, size * 0.24f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.04f, power.y - size * 0.12f, size * 0.08f, size * 0.18f, glyphSoft)
            }
            PowerUpType.WIDE_PADDLE -> {
                renderer.drawRect(power.x - size * 0.32f, power.y - size * 0.06f, size * 0.64f, size * 0.12f, glyph)
                renderer.drawRect(power.x - size * 0.48f, power.y - size * 0.12f, size * 0.06f, size * 0.24f, glyphSoft)
                renderer.drawRect(power.x + size * 0.42f, power.y - size * 0.12f, size * 0.06f, size * 0.24f, glyphSoft)
                renderer.drawRect(power.x - size * 0.42f, power.y - size * 0.02f, size * 0.1f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.32f, power.y - size * 0.02f, size * 0.1f, size * 0.04f, glyphSoft)
            }
            PowerUpType.SHRINK -> {
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.08f, size * 0.12f, size * 0.16f, glyph)
                renderer.drawRect(power.x + size * 0.06f, power.y - size * 0.08f, size * 0.12f, size * 0.16f, glyph)
                renderer.drawRect(power.x - size * 0.05f, power.y - size * 0.06f, size * 0.1f, size * 0.12f, glyphSoft)
            }
            PowerUpType.SLOW -> {
                renderer.drawCircle(power.x, power.y, size * 0.19f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y, size * 0.04f, size * 0.16f, glyphSoft)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.08f, size * 0.14f, size * 0.04f, glyphSoft)
                renderer.drawCircle(power.x, power.y, size * 0.04f, glyph)
            }
            PowerUpType.OVERDRIVE -> {
                renderer.drawRect(power.x - size * 0.08f, power.y + size * 0.08f, size * 0.16f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.14f, power.y - size * 0.02f, size * 0.28f, size * 0.12f, glyphSoft)
                renderer.drawRect(power.x - size * 0.08f, power.y - size * 0.2f, size * 0.16f, size * 0.18f, glyph)
            }
            PowerUpType.FIREBALL -> {
                renderer.drawCircle(power.x, power.y + size * 0.04f, size * 0.18f, glyph)
                renderer.drawCircle(power.x + size * 0.05f, power.y + size * 0.18f, size * 0.09f, glyphSoft)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.22f, size * 0.12f, size * 0.18f, glyph)
                renderer.drawRect(power.x + size * 0.02f, power.y - size * 0.22f, size * 0.08f, size * 0.14f, glyphSoft)
            }
            PowerUpType.LIFE -> {
                renderer.drawCircle(power.x - size * 0.1f, power.y + size * 0.08f, size * 0.1f, glyph)
                renderer.drawCircle(power.x + size * 0.1f, power.y + size * 0.08f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.04f, size * 0.4f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.1f, power.y - size * 0.22f, size * 0.2f, size * 0.18f, glyph)
            }
            PowerUpType.MAGNET -> {
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.18f, size * 0.1f, size * 0.32f, glyph)
                renderer.drawRect(power.x + size * 0.12f, power.y - size * 0.18f, size * 0.1f, size * 0.32f, glyph)
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.22f, size * 0.44f, size * 0.08f, glyph)
                renderer.drawRect(power.x - size * 0.22f, power.y + size * 0.12f, size * 0.44f, size * 0.08f, glyphSoft)
            }
            PowerUpType.GRAVITY_WELL -> {
                val centerX = power.x
                val centerY = power.y
                val step = size * 0.08f
                for (i in 0..4) {
                    val angle = i * 1.1f + pulse
                    val radius = step * (i + 1)
                    val x = centerX + kotlin.math.cos(angle) * radius
                    val y = centerY + kotlin.math.sin(angle) * radius
                    renderer.drawCircle(x, y, size * 0.06f, glyph)
                }
                renderer.drawCircle(centerX, centerY, size * 0.08f, glyphSoft)
            }
            PowerUpType.BALL_SPLITTER -> {
                renderer.drawCircle(power.x, power.y + size * 0.12f, size * 0.1f, glyph)
                renderer.drawCircle(power.x - size * 0.12f, power.y - size * 0.06f, size * 0.1f, glyph)
                renderer.drawCircle(power.x + size * 0.12f, power.y - size * 0.06f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyphSoft)
            }
            PowerUpType.FREEZE -> {
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.02f, size * 0.4f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.14f, power.y - size * 0.14f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y - size * 0.14f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x - size * 0.14f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyphSoft)
            }
            PowerUpType.PIERCE -> {
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.22f, size * 0.04f, size * 0.44f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y + size * 0.12f, size * 0.24f, size * 0.06f, glyph)
                renderer.drawRect(power.x - size * 0.08f, power.y + size * 0.18f, size * 0.16f, size * 0.06f, glyphSoft)
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.08f, size * 0.44f, size * 0.16f, glyphSoft)
            }
            PowerUpType.RICOCHET -> {
                // Bouncing arrows
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.12f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.14f, size * 0.04f, size * 0.12f, glyph)
                renderer.drawRect(power.x + size * 0.02f, power.y + size * 0.02f, size * 0.12f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.1f, power.y + size * 0.06f, size * 0.04f, size * 0.12f, glyphSoft)
            }
            PowerUpType.TIME_WARP -> {
                // Clock/spiral
                renderer.drawCircle(power.x, power.y, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.18f, size * 0.04f, size * 0.12f, glyphSoft)
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.12f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyph)
            }
            PowerUpType.DOUBLE_SCORE -> {
                // Star/X shape
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.18f, size * 0.04f, size * 0.36f, glyph)
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.36f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y - size * 0.12f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.04f, power.y - size * 0.12f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x - size * 0.12f, power.y + size * 0.04f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.04f, power.y + size * 0.04f, size * 0.08f, size * 0.08f, glyphSoft)
            }
        }
    }

    private fun renderAimGuide(renderer: Renderer2D) {
        val ball = balls.firstOrNull() ?: return
        val angle = aimAngle.coerceIn(aimMinAngle, Math.PI.toFloat() - aimMinAngle)
        var dx = kotlin.math.cos(angle)
        var dy = kotlin.math.sin(angle)
        val startX = ball.x
        val startY = ball.y
        val radius = ball.radius
        val arrowLength = (worldHeight * 0.16f).coerceIn(12f, 22f)
        val arrowSteps = 7
        for (i in 1..arrowSteps) {
            val t = i.toFloat() / arrowSteps.toFloat()
            val size = 0.42f + t * 0.34f
            val alpha = (0.5f + t * 0.5f).coerceIn(0f, 1f) // Increased base alpha from 0.35f to 0.5f
            renderer.drawCircle(
                startX + dx * arrowLength * t,
                startY + dy * arrowLength * t,
                size,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], alpha)
            )
        }

        var segStartX = startX
        var segStartY = startY
        val maxSegments = 5

        repeat(maxSegments) { segment ->
            val hit = findAimCollision(segStartX, segStartY, dx, dy, radius)
            if (!hit.t.isFinite() || hit.t <= 0f) return
            val segmentLength = hit.t
            val steps = (segmentLength / (worldHeight * 0.06f))
                .toInt()
                .coerceIn(6, 16)
            val segmentAlpha = (0.38f - segment * 0.08f).coerceAtLeast(0.14f)
            val isFirstBounce = segment == 0
            val segmentColor = if (isFirstBounce) {
                // First bounce in contrasting color (dimmer version of accent)
                fillColor(tempColor, theme.accent[0] * 0.6f, theme.accent[1] * 0.6f, theme.accent[2] * 0.6f, 1f)
            } else {
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 1f)
            }
            for (i in 1..steps) {
                val t = i.toFloat() / steps.toFloat()
                val alpha = (segmentAlpha * (1f - t)).coerceIn(0f, segmentAlpha)
                val size = 0.32f + (1f - t) * 0.18f
                val finalColor = fillColor(tempColor, segmentColor[0], segmentColor[1], segmentColor[2], alpha)
                renderer.drawCircle(
                    segStartX + dx * segmentLength * t,
                    segStartY + dy * segmentLength * t,
                    size,
                    finalColor
                )
            }
            val impactX = segStartX + dx * segmentLength
            val impactY = segStartY + dy * segmentLength
            if (hit.hitsBrick) {
                renderer.drawCircle(
                    impactX,
                    impactY,
                    0.7f,
                    fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.88f)
                )
                return
            }
            if (hit.nx == 0f && hit.ny == 0f) {
                // Fallback: continue in current direction instead of stopping abruptly
                // This prevents the aim guide from cutting off unexpectedly
                segStartX = impactX + dx * 0.02f
                segStartY = impactY + dy * 0.02f
                // Don't return, continue to next segment
            } else {
                if (hit.nx != 0f) dx = -dx
                if (hit.ny != 0f) dy = -dy
                segStartX = impactX + dx * 0.02f
                segStartY = impactY + dy * 0.02f
            }
        }
    }

    private data class AimHit(val t: Float, val nx: Float, val ny: Float, val hitsBrick: Boolean = false)

    private fun findAimCollision(
        startX: Float,
        startY: Float,
        dirX: Float,
        dirY: Float,
        radius: Float
    ): AimHit {
        val wallHit = findAimWallCollision(startX, startY, dirX, dirY, radius)
        val brickHit = findAimBrickCollision(startX, startY, dirX, dirY, radius, wallHit.t)
        return if (brickHit != null && brickHit.t < wallHit.t) brickHit else wallHit
    }

    private fun findAimWallCollision(
        startX: Float,
        startY: Float,
        dirX: Float,
        dirY: Float,
        radius: Float
    ): AimHit {
        var bestT = Float.POSITIVE_INFINITY
        var normalX = 0f
        var normalY = 0f

        if (dirY > 0f) {
            val tTop = (worldHeight - radius - startY) / dirY
            if (tTop > 0f && tTop < bestT) {
                bestT = tTop
                normalX = 0f
                normalY = -1f
            }
        }
        if (dirX > 0f) {
            val tRight = (worldWidth - radius - startX) / dirX
            if (tRight > 0f && tRight < bestT) {
                bestT = tRight
                normalX = -1f
                normalY = 0f
            }
        } else if (dirX < 0f) {
            val tLeft = (radius - startX) / dirX
            if (tLeft > 0f && tLeft < bestT) {
                bestT = tLeft
                normalX = 1f
                normalY = 0f
            }
        }

        return AimHit(bestT, normalX, normalY, hitsBrick = false)
    }

    private fun findAimBrickCollision(
        startX: Float,
        startY: Float,
        dirX: Float,
        dirY: Float,
        radius: Float,
        maxDistance: Float
    ): AimHit? {
        val epsilon = 1e-5f
        var bestT = maxDistance
        var bestNx = 0f
        var bestNy = 0f
        var found = false

        bricks.forEach { brick ->
            if (!brick.alive) return@forEach
            val left = brick.x - radius
            val right = brick.x + brick.width + radius
            val bottom = brick.y - radius
            val top = brick.y + brick.height + radius

            val tNearX: Float
            val tFarX: Float
            if (abs(dirX) < epsilon) {
                if (startX <= left || startX >= right) return@forEach
                tNearX = Float.NEGATIVE_INFINITY
                tFarX = Float.POSITIVE_INFINITY
            } else {
                val tx1 = (left - startX) / dirX
                val tx2 = (right - startX) / dirX
                tNearX = min(tx1, tx2)
                tFarX = max(tx1, tx2)
            }

            val tNearY: Float
            val tFarY: Float
            if (abs(dirY) < epsilon) {
                if (startY <= bottom || startY >= top) return@forEach
                tNearY = Float.NEGATIVE_INFINITY
                tFarY = Float.POSITIVE_INFINITY
            } else {
                val ty1 = (bottom - startY) / dirY
                val ty2 = (top - startY) / dirY
                tNearY = min(ty1, ty2)
                tFarY = max(ty1, ty2)
            }

            val tEnter = max(tNearX, tNearY)
            val tExit = min(tFarX, tFarY)
            if (tExit <= 0f || tEnter <= 0f || tEnter >= tExit || tEnter >= bestT) return@forEach

            val normalX: Float
            val normalY: Float
            if (kotlin.math.abs(tNearX - tNearY) < 0.0001f) {
                if (kotlin.math.abs(dirX) >= kotlin.math.abs(dirY)) {
                    normalX = if (dirX > 0f) -1f else 1f
                    normalY = 0f
                } else {
                    normalX = 0f
                    normalY = if (dirY > 0f) -1f else 1f
                }
            } else if (tNearX > tNearY) {
                normalX = if (dirX > 0f) -1f else 1f
                normalY = 0f
            } else {
                normalX = 0f
                normalY = if (dirY > 0f) -1f else 1f
            }

            bestT = tEnter
            bestNx = normalX
            bestNy = normalY
            found = true
        }

        return if (found) AimHit(bestT, bestNx, bestNy, hitsBrick = true) else null
    }

    private fun drawStripe(renderer: Renderer2D, brick: Brick, color: FloatArray, count: Int) {
        if (count <= 0) return
        val stripeHeight = brick.height * 0.12f
        val gap = brick.height * 0.14f
        repeat(count) { index ->
            val y = brick.y + brick.height * 0.2f + index * gap
            renderer.drawRect(brick.x + brick.width * 0.08f, y, brick.width * 0.84f, stripeHeight, color)
        }
    }

    private fun drawInvaderShip(renderer: Renderer2D, brick: Brick, baseColor: FloatArray) {
        val time = renderTimeSeconds
        val hitPulse = brick.hitFlash.coerceIn(0f, 1f)
        val scale = 1f + hitPulse * 0.08f
        val wobble = if (hitPulse > 0f) {
            kotlin.math.sin(time * 18f + brick.gridX) * hitPulse * brick.width * 0.04f
        } else {
            0f
        }

        val baseW = brick.width
        val baseH = brick.height
        val w = baseW * scale
        val h = baseH * scale
        val x = brick.x + wobble - (w - baseW) * 0.5f
        val y = brick.y - (h - baseH) * 0.5f
        val variant = ((brick.gridX * 3 + brick.gridY * 5) % 4 + 4) % 4
        val tint = when (variant) {
            0 -> 1.0f
            1 -> 0.88f
            2 -> 1.12f
            else -> 0.96f
        }
        val pulseBoost = 1f + hitPulse * 0.25f

        val shadow = adjustColor(baseColor, 0.35f, 0.35f)
        renderer.drawRect(x + w * 0.04f, y + h * 0.04f, w * 0.92f, h * 0.92f, shadow)

        val bodyHeight = when (variant) {
            0 -> 0.48f
            1 -> 0.42f
            2 -> 0.52f
            else -> 0.46f
        } * h
        val bodyY = when (variant) {
            0 -> 0.26f
            1 -> 0.3f
            2 -> 0.22f
            else -> 0.28f
        } * h
        val body = adjustColor(baseColor, 0.95f * tint * pulseBoost, 1f)
        renderer.drawRect(x, y + bodyY, w, bodyHeight, body)

        val wingColor = adjustColor(baseColor, 1.15f * tint * pulseBoost, 1f)
        val wingHeight = h * if (variant == 2) 0.32f else 0.28f
        renderer.drawRect(x + w * 0.06f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)
        renderer.drawRect(x + w * 0.74f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)

        val cockpit = adjustColor(baseColor, 1.35f * pulseBoost, 1f)
        val cockpitRadius = h * when (variant) {
            1 -> 0.15f
            2 -> 0.2f
            else -> 0.18f
        }
        renderer.drawCircle(x + w * 0.5f, y + h * 0.58f, cockpitRadius, cockpit)

        if (variant == 2) {
            val light = adjustColor(baseColor, 1.6f, 0.9f)
            renderer.drawCircle(x + w * 0.38f, y + h * 0.56f, h * 0.08f, light)
            renderer.drawCircle(x + w * 0.62f, y + h * 0.56f, h * 0.08f, light)
        }

        val engine = adjustColor(baseColor, 1.5f * tint * pulseBoost, 0.9f)
        renderer.drawRect(x + w * 0.22f, y + h * 0.08f, w * 0.12f, h * 0.12f, engine)
        renderer.drawRect(x + w * 0.66f, y + h * 0.08f, w * 0.12f, h * 0.12f, engine)

        val rim = adjustColor(baseColor, 0.7f * tint, 1f)
        renderer.drawRect(x + w * 0.08f, y + h * 0.7f, w * 0.84f, h * 0.06f, rim)

        if (variant == 1) {
            val fin = adjustColor(baseColor, 1.25f, 0.9f)
            renderer.drawRect(x + w * 0.14f, y + h * 0.68f, w * 0.12f, h * 0.08f, fin)
            renderer.drawRect(x + w * 0.74f, y + h * 0.68f, w * 0.12f, h * 0.08f, fin)
        }
        if (variant == 3) {
            val ridge = adjustColor(baseColor, 1.1f, 0.85f)
            renderer.drawRect(x + w * 0.46f, y + h * 0.34f, w * 0.08f, h * 0.28f, ridge)
        }

        if (brick.fireFlash > 0f) {
            val flashAlpha = (brick.fireFlash * 0.8f).coerceIn(0f, 0.8f)
            val flashColor = adjustColor(baseColor, 1.5f, flashAlpha)
            val flashRadius = h * (0.16f + brick.fireFlash * 0.18f)
            renderer.drawCircle(x + w * 0.5f, y - h * 0.02f, flashRadius, flashColor)
            renderer.drawRect(x + w * 0.46f, y - h * 0.16f, w * 0.08f, h * 0.14f, adjustColor(baseColor, 1.8f, flashAlpha))
        }
    }

    private fun invaderKey(brick: Brick): Long {
        return (brick.gridX.toLong() shl 32) or (brick.gridY.toLong() and 0xffffffffL)
    }

    private fun adjustColor(color: FloatArray, factor: Float, alpha: Float): FloatArray {
        return floatArrayOf(
            (color[0] * factor).coerceIn(0f, 1f),
            (color[1] * factor).coerceIn(0f, 1f),
            (color[2] * factor).coerceIn(0f, 1f),
            alpha
        )
    }

    private fun fillColor(out: FloatArray, r: Float, g: Float, b: Float, a: Float): FloatArray {
        out[0] = r
        out[1] = g
        out[2] = b
        out[3] = a
        return out
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t.coerceIn(0f, 1f)
    }

    fun getObjectCount(): Int = balls.size + bricks.size + powerups.size + beams.size + enemyShots.size + particles.size + waves.size

    fun isGameRunning(): Boolean = state == GameState.RUNNING

    private fun applyLayoutTuning(aspectRatio: Float, preserveRowBoost: Boolean) {
        val tallness = ((aspectRatio - 1.25f) / 0.85f).coerceIn(0f, 1f)
        val isWide = aspectRatio < 1.45f
        val wideDensityRows = when {
            aspectRatio < 1.24f -> 6
            aspectRatio < 1.34f -> 5
            aspectRatio < 1.48f -> 4
            aspectRatio < 1.62f -> 3
            else -> 0
        }
        val slateVerticalBonus = when {
            aspectRatio < 1.24f -> 2
            aspectRatio < 1.46f -> 1
            else -> 0
        }
        val tallDensityRows = when {
            aspectRatio > 2.2f -> 3
            aspectRatio > 1.9f -> 2
            aspectRatio > 1.72f -> 1
            else -> 0
        }
        val wideDensityCols = when {
            aspectRatio < 1.32f -> 2
            aspectRatio < 1.5f -> 1
            else -> 0
        }
        // Shared "zoomed-out" baseline for all modes: smaller bricks, more distance to the paddle,
        // and consistent spacing so switching modes doesn't feel like a camera jump.
        brickAreaTopRatio = lerp(0.992f, 0.978f, tallness)
        brickAreaBottomRatio = lerp(0.69f, 0.62f, tallness)
        val wideVerticalExpansion = when {
            aspectRatio < 1.24f -> 0.12f
            aspectRatio < 1.34f -> 0.105f
            aspectRatio < 1.46f -> 0.085f
            else -> 0f
        }
        val tallVerticalExpansion = when {
            aspectRatio > 2.2f -> 0.07f
            aspectRatio > 1.9f -> 0.05f
            aspectRatio > 1.72f -> 0.03f
            else -> 0f
        }
        brickAreaBottomRatio = (brickAreaBottomRatio - wideVerticalExpansion - tallVerticalExpansion).coerceIn(0.48f, 0.7f)
        brickSpacing = lerp(0.3f, 0.36f, tallness)
        if (aspectRatio < 1.5f) {
            brickSpacing = (brickSpacing - 0.04f).coerceAtLeast(0.24f)
        } else if (aspectRatio > 2.1f) {
            brickSpacing = (brickSpacing - 0.02f).coerceAtLeast(0.24f)
        }
        if (!preserveRowBoost) {
            val densityBoost = (levelIndex / 6).coerceAtMost(2)
            val slateRowBoost = when {
                aspectRatio < 1.28f -> 3
                aspectRatio < 1.42f -> 2
                aspectRatio < 1.58f -> 1
                else -> 0
            }
            val slateColBoost = when {
                aspectRatio < 1.32f -> 2
                aspectRatio < 1.46f -> 1
                else -> 0
            }
            val baseRowBoost =
                (if (isWide) 5 else 4) + densityBoost + slateRowBoost + wideDensityRows + tallDensityRows
            val baseColBoost =
                (if (isWide) 4 else 3) + densityBoost + slateColBoost + wideDensityCols
            when (config.mode) {
                GameMode.RUSH -> {
                    layoutRowBoost = (baseRowBoost - 2).coerceAtLeast(0)
                    layoutColBoost = (baseColBoost - 2).coerceAtLeast(0)
                }
                GameMode.TIMED -> {
                    layoutRowBoost = (baseRowBoost - 1).coerceAtLeast(1)
                    layoutColBoost = (baseColBoost - 1).coerceAtLeast(1)
                }
                GameMode.GOD -> {
                    layoutRowBoost = (baseRowBoost - 1 + slateVerticalBonus).coerceAtLeast(1)
                    layoutColBoost = (baseColBoost - 1).coerceAtLeast(1)
                }
                GameMode.VOLLEY -> {
                    val volleyRowBase = when {
                        aspectRatio < 1.3f -> 3
                        aspectRatio < 1.6f -> 2
                        aspectRatio > 2.2f -> 1
                        else -> 1
                    }
                    layoutRowBoost =
                        (volleyRowBase + densityBoost / 2 + wideDensityRows / 2 + tallDensityRows + slateVerticalBonus)
                            .coerceAtLeast(1)
                    layoutColBoost = when {
                        aspectRatio < 1.35f -> 3
                        aspectRatio < 1.7f -> 2
                        aspectRatio > 2.15f -> 1
                        else -> 1
                    }
                }
                GameMode.TUNNEL -> {
                    val tunnelRowBase = when {
                        aspectRatio < 1.3f -> 4
                        aspectRatio < 1.6f -> 3
                        aspectRatio > 2.2f -> 2
                        else -> 2
                    }
                    layoutRowBoost =
                        (tunnelRowBase + densityBoost / 2 + wideDensityRows / 2 + tallDensityRows + slateVerticalBonus)
                            .coerceAtLeast(2)
                    layoutColBoost = when {
                        aspectRatio < 1.36f -> 2
                        aspectRatio < 1.7f -> 1
                        else -> 0
                    }
                }
                GameMode.SURVIVAL -> {
                    layoutRowBoost = baseRowBoost + 1
    private fun applyLayoutTuning(aspectRatio: Float, preserveRowBoost: Boolean) {
        val tallness = ((aspectRatio - 1.25f) / 0.85f).coerceIn(0f, 1f)
        val isSlate = aspectRatio < 1.45f
        
        // Base tuning interpolated by tallness (0.0 = 4:3 iPad, 1.0 = 20:9 Phone)
        brickAreaTopRatio = lerp(0.992f, 0.978f, tallness)
        brickAreaBottomRatio = lerp(0.69f, 0.62f, tallness)
        brickSpacing = lerp(0.32f, 0.38f, tallness)
        
        // Slate-specific optimizations for density
        if (isSlate) {
             // Push bricks slightly higher to clear HUD area better on squarer screens
            brickAreaBottomRatio += 0.025f 
            // Slightly smaller spacing for density
            brickSpacing *= 0.9f 
        }

        if (!preserveRowBoost) {
            val baseRowBoost = when {
                isSlate -> 4 // Significantly more rows on slates to fill the wider/shorter space
                aspectRatio < 1.75f -> 2
                else -> 0
            }
            val baseColBoost = when {
                isSlate -> 2 // More columns on slates
                aspectRatio < 1.6f -> 1
                else -> 0
            }
            when (config.mode) {
                GameMode.VOLLEY -> {
                    layoutRowBoost = baseRowBoost
                    layoutColBoost = baseColBoost
                }
                GameMode.GOD -> {
                    layoutRowBoost = baseRowBoost + 4
                    layoutColBoost = baseColBoost + 1
                }
                GameMode.ENDLESS -> {
                    layoutRowBoost = baseRowBoost + 1
                    layoutColBoost = baseColBoost + 1
                }
                else -> {
                    layoutRowBoost = baseRowBoost
                    layoutColBoost = baseColBoost
                }
            }
        }
        // Smaller bricks overall to avoid an overly zoomed-in feel.
        globalBrickScale = lerp(0.82f, 0.79f, tallness)
        
        if (isSlate) {
             // Compensate for the extra density/rows
            globalBrickScale = (globalBrickScale - 0.05f).coerceAtLeast(0.70f)
        } else if (aspectRatio > 2.15f) {
            globalBrickScale = (globalBrickScale - 0.015f).coerceAtLeast(0.75f)
        }
        
        if (config.mode == GameMode.RUSH) {
            globalBrickScale = (globalBrickScale + 0.03f).coerceAtMost(0.88f)
            brickSpacing = (brickSpacing + 0.03f).coerceAtMost(0.42f)
        } else if (config.mode == GameMode.VOLLEY) {
            globalBrickScale = (globalBrickScale + 0.008f).coerceAtMost(0.85f)
            brickAreaBottomRatio = (brickAreaBottomRatio - 0.02f).coerceAtLeast(0.61f)
        } else if (config.mode == GameMode.TUNNEL) {
            // Tunnel should occupy more vertical space so the fortress reads clearly.
            globalBrickScale = (globalBrickScale + 0.02f).coerceAtMost(0.87f)
            brickAreaBottomRatio = (brickAreaBottomRatio - 0.06f).coerceAtLeast(0.58f)
            brickSpacing = (brickSpacing + 0.02f).coerceAtMost(0.42f)
        } else if (config.mode == GameMode.TIMED) {
            globalBrickScale = (globalBrickScale + 0.01f).coerceAtMost(0.86f)
        } else if (config.mode == GameMode.SURVIVAL) {
            globalBrickScale = (globalBrickScale - 0.01f).coerceAtLeast(0.75f)
        }
        if (config.mode.invaders) {
            brickAreaBottomRatio = (brickAreaBottomRatio + lerp(0.035f, 0.05f, tallness)).coerceAtMost(0.79f)
            brickSpacing = brickSpacing * 0.95f
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

    private fun updateAimFromPaddle() {
        val center = worldWidth * 0.5f
        val sourceX = if (isDragging) paddle.targetX else paddle.x
        val delta = (sourceX - center) / center
        aimNormalizedTarget = delta.coerceIn(-1f, 1f)
    }

    private fun updateAimFromTouch() {
        val ball = balls.firstOrNull() ?: return updateAimFromPaddle()

        // Use direct finger-to-ball aiming when ball is stuck to paddle in READY state
        if (state == GameState.READY && ball.stuckToPaddle) {
            val dx = touchWorldX - ball.x
            val dy = touchWorldY - ball.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance > 0.1f) {
                val angle = atan2(dy, dx)
                // Convert angle to normalized aim value
                val centerAngle = Math.PI.toFloat() * 0.5f
                val maxDeflection = (centerAngle - aimMinAngle).coerceAtLeast(0.2f)
                val deflection = (centerAngle - angle).coerceIn(-maxDeflection, maxDeflection)
                aimNormalizedTarget = (deflection / maxDeflection).coerceIn(-1f, 1f)
                return
            }
        }

        // Fall back to paddle-relative aiming
        updateAimFromPaddle()
    }

    private fun applyAimFromNormalized(normalized: Float) {
        val clamped = normalized.coerceIn(-1f, 1f)
        val stabilized = if (abs(clamped) < aimCenterDeadZone) 0f else clamped
        aimHasInput = isDragging || abs(stabilized) > 0.001f
        val eased = stabilized * (0.8f + abs(stabilized) * 0.2f)
        val centerAngle = Math.PI.toFloat() * 0.5f
        val maxDeflection = (centerAngle - aimMinAngle).coerceAtLeast(0.2f)
        val signedDeflection = eased * maxDeflection
        aimAngle = (centerAngle - signedDeflection).coerceIn(aimMinAngle, Math.PI.toFloat() - aimMinAngle)
    }

    private fun syncAimForLaunch() {
        val stuckReadyBall = state == GameState.READY && balls.any { it.stuckToPaddle }
        if (isDragging && stuckReadyBall) {
            // Preserve finger-directed intent so launch trajectory matches the aim guide.
            updateAimFromTouch()
        } else {
            updateAimFromPaddle()
        }
        aimNormalized = aimNormalizedTarget
        applyAimFromNormalized(aimNormalized)
    }

    private fun updateAim(dt: Float) {
        if (isDragging) {
            aimNormalized = aimNormalizedTarget
        } else {
            val lerpFactor = if (dt > 0f) {
                1f - exp(-aimSmoothingRate * dt)
            } else {
                1f
            }
            aimNormalized += (aimNormalizedTarget - aimNormalized) * lerpFactor
        }
        applyAimFromNormalized(aimNormalized)
    }

    private fun clampPaddleX(worldX: Float): Float {
        return worldX.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
    }

    private fun syncStuckBallsToPaddle() {
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

    private fun updatePaddleFromTouch(worldX: Float, snapImmediately: Boolean) {
        val clamped = clampPaddleX(worldX)
        paddle.targetX = clamped
        if (!snapImmediately) return
        paddle.x = clamped
        if (state == GameState.READY) {
            attachBallToPaddle()
        }
        syncStuckBallsToPaddle()
    }

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return
        if (config.mode == GameMode.VOLLEY && state == GameState.RUNNING) {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN &&
                activeEffects.containsKey(PowerUpType.LASER)
            ) {
                shootLaser()
            }
            // Allow paddle movement in VOLLEY RUNNING state
            // Continue with normal touch handling below
        }

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
            MotionEvent.ACTION_UP -> "up"
            else -> "other"
        }
        logger?.logTouchInput(actionString, trackedX, trackedY, trackedPressure)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = actionPointerId
                val downX = pointerWorldX(activePointerId) ?: trackedX
                val downY = pointerWorldY(activePointerId) ?: trackedY
                touchWorldX = downX
                touchWorldY = downY
                updatePaddleFromTouch(
                    downX,
                    snapImmediately = state == GameState.READY || balls.any { it.stuckToPaddle }
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
                updatePaddleFromTouch(
                    moveX,
                    snapImmediately = state == GameState.READY || balls.any { it.stuckToPaddle }
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
                        updatePaddleFromTouch(
                            touchWorldX,
                            snapImmediately = state == GameState.READY || balls.any { it.stuckToPaddle }
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
                } else if (magnetActive && balls.any { it.stuckToPaddle }) {
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
        val clearedBoardWhilePaused =
            state == GameState.PAUSED &&
                config.mode.godMode &&
                bricks.none { it.alive && it.type != BrickType.UNBREAKABLE }
        // Defensive checks to prevent invalid level advancement
        if (!awaitingNextLevel && !clearedBoardWhilePaused) {
            logger?.logError("nextLevel called when not awaiting next level (state=$state, lives=$lives)")
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

    private fun resetLevel(first: Boolean) {
        logger?.logLevelStart(levelIndex + 1, currentLayout?.theme?.name ?: "unknown")
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
        volleyTurnActive = false
        volleyQueuedBalls = 0
        volleyLaunchTimer = 0f
        volleyTurnCount = 0
        volleyAdvanceRows = 0
        volleyLaunchX = paddle.x
        volleyReturnAnchorX = Float.NaN
        volleyReturnSumX = 0f
        volleyReturnCount = 0
        tunnelShotsFired = 0
        tunnelGateFlash = 0f
        speedMultiplier = 1f
        levelClearFlash = 0f
        activeEffects.clear()
        balls.clear()
        beams.clear()
        powerups.clear()
        enemyShots.clear()
        particles.clear()
        waves.clear()
        lastPowerupSnapshot = emptyList()
        lastComboReported = 0

        if (config.mode.godMode && !godModeTipShown) {
            listener.onTip("God mode: bottom shield is always active.")
            godModeTipShown = true
        }
        if (config.mode == GameMode.VOLLEY) {
            volleyBallCount = if (first) 5 else volleyBallCount.coerceIn(5, 20)
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
        logger?.logLevelStart(levelIndex, theme.name)
    }

    private fun buildSpatialHash() {
        spatialHash.clear()
        for (brick in bricks) {
            if (!brick.alive) continue
            val minX = (brick.x / spatialHashCellSize).toInt()
            val maxX = ((brick.x + brick.width) / spatialHashCellSize).toInt()
            val minY = (brick.y / spatialHashCellSize).toInt()
            val maxY = ((brick.y + brick.height) / spatialHashCellSize).toInt()

            for (cellX in minX..maxX) {
                for (cellY in minY..maxY) {
                    val cellKey = cellX to cellY
                    spatialHash.getOrPut(cellKey) { mutableListOf() }.add(brick)
                }
            }
        }
        spatialHashDirty = false
    }

    private fun getNearbyBricks(ball: Ball): List<Brick> {
        nearbyBrickBuffer.clear()
        nearbyBrickSeen.clear()
        val ballMinX = ((ball.x - ball.radius) / spatialHashCellSize).toInt()
        val ballMaxX = ((ball.x + ball.radius) / spatialHashCellSize).toInt()
        val ballMinY = ((ball.y - ball.radius) / spatialHashCellSize).toInt()
        val ballMaxY = ((ball.y + ball.radius) / spatialHashCellSize).toInt()

        for (cellX in ballMinX..ballMaxX) {
            for (cellY in ballMinY..ballMaxY) {
                val cellKey = cellX to cellY
                spatialHash[cellKey]?.forEach { brick ->
                    if (nearbyBrickSeen.add(brick)) {
                        nearbyBrickBuffer.add(brick)
                    }
                }
            }
        }
        if (nearbyBrickBuffer.isEmpty() && bricks.isNotEmpty()) {
            // Safety fallback: avoid collision loss if hash is temporarily stale.
            return bricks
        }
        return nearbyBrickBuffer
    }

    private fun buildBricks(layout: LevelFactory.LevelLayout) {
        bricks.clear()
        val rows = layout.rows + layoutRowBoost
        val requestedCols = layout.cols + layoutColBoost
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
                remapVolleyColumn(sourceCol, requestedCols, cols)
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
            repeat(layoutRowBoost) { offset ->
                val rowIndex = baseRow + offset
                for (col in 0 until layout.cols) {
                    val seed = (levelIndex + 1) * 97 + rowIndex * 13 + col * 7
                    val roll = Random(seed).nextFloat()
                    val type = when {
                        roll > 0.9f -> BrickType.REINFORCED
                        roll < 0.03f -> BrickType.ARMORED
                        else -> BrickType.NORMAL
                    }
                    val baseHp = baseHitPoints(type)
                    val hp = if (type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * difficulty).roundToInt())
                    val cellX = spacing + (col + colOffset) * (baseBrickWidth + spacing)
                    val visualRow = if (config.mode == GameMode.VOLLEY) rowIndex + volleyAdvanceRows else rowIndex
                    val cellY = areaBottom + (rows - 1 - visualRow) * (baseBrickHeight + spacing * 0.5f)
                    val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
                    val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
                    val gridX = col + colOffset
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

        if (layoutColBoost > 0 && !config.mode.invaders) {
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
        dynamicBrickLayout = config.mode.invaders || bricks.any { it.type == BrickType.MOVING }
        buildSpatialHash()
    }

    private fun resolveBrickSpecForMode(type: BrickType, hitPoints: Int): Pair<BrickType, Int> {
        if (config.mode != GameMode.VOLLEY || type != BrickType.UNBREAKABLE) {
            return type to hitPoints
        }
        // Volley must remain fully breakable: convert unbreakables into durable breakable bricks.
        val fallbackType = if (levelIndex >= 4) BrickType.ARMORED else BrickType.REINFORCED
        val baseHp = baseHitPoints(fallbackType)
        val scaledHp = (baseHp * (1f + levelIndex * 0.06f)).roundToInt().coerceIn(baseHp, 9)
        return fallbackType to scaledHp
    }

    private fun effectiveVolleyColumns(requestedCols: Int): Int {
        val clampedRequested = requestedCols.coerceAtLeast(6)
        val target = when {
            currentAspectRatio <= 1.34f -> 12
            currentAspectRatio <= 1.55f -> 11
            currentAspectRatio <= 1.85f -> 10
            else -> 9
        }
        return min(clampedRequested, target)
    }

    private fun applyRowBoostTopPadding(baseBrickHeight: Float) {
        if (layoutRowBoost <= 0 || config.mode.invaders || config.mode == GameMode.VOLLEY) return
        val topPad = baseBrickHeight * 0.15f
        bricks.forEach { brick ->
            brick.y += topPad
            brick.baseY = brick.y
        }
    }

    private fun remapVolleyColumn(col: Int, originalCols: Int, targetCols: Int): Int {
        if (targetCols >= originalCols) return col.coerceIn(0, targetCols - 1)
        val ratio = (col.toFloat() + 0.5f) / originalCols.toFloat().coerceAtLeast(1f)
        return (ratio * targetCols).toInt().coerceIn(0, targetCols - 1)
    }

    private fun relayoutBricks() {
        val layout = currentLayout ?: return
        if (bricks.isEmpty()) return
        val rows = layout.rows + layoutRowBoost
        val requestedCols = layout.cols + layoutColBoost
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

    private fun baseHitPoints(type: BrickType): Int {
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

    private fun updateTimers(dt: Float) {
        if (state != GameState.RUNNING) return
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

    private fun applyCosmeticTier() {
        maxTrailPoints = 8 + cosmeticTier * 2
        trailLife = 0.28f + cosmeticTier * 0.04f
    }

    private fun updateDailyChallenges(type: ChallengeType, value: Int = 1) {
        val challenges = dailyChallenges ?: return
        val newlyCompleted = DailyChallengeManager.updateChallengeProgress(challenges, type, value)
        if (newlyCompleted.isNotEmpty()) {
            handleChallengeRewards(newlyCompleted)
        }
    }

    private fun handleChallengeRewards(completed: List<DailyChallenge>) {
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

    private fun updatePaddle(dt: Float) {
        val previousX = paddle.x
        val target = paddle.targetX
        val snapToFinger = isDragging && (state == GameState.READY || balls.any { it.stuckToPaddle })
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

    private fun updateBricks(dt: Float) {
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

    private fun collectAliveInvaders(): List<Brick> {
        aliveInvaderBuffer.clear()
        invaderBricks.forEach { invader ->
            if (invader.alive) {
                aliveInvaderBuffer.add(invader)
            }
        }
        return aliveInvaderBuffer
    }

    private fun updateInvaderFormation(dt: Float) {
        val invaders = collectAliveInvaders()
        if (invaders.isEmpty()) return
        invaderRowPhase += dt * (0.55f + levelIndex * 0.015f)
        invaderTurnSoundCooldown = max(0f, invaderTurnSoundCooldown - dt)
        val leftBound = 0.6f
        val rightBound = worldWidth - 0.6f

        fun rowDrift(row: Int): Float {
            return kotlin.math.sin(invaderRowPhase + row * invaderRowPhaseOffset) * invaderRowDrift
        }

        var minBaseX = Float.POSITIVE_INFINITY
        var maxBaseX = Float.NEGATIVE_INFINITY
        invaders.forEach { invader ->
            val x = invader.baseX + rowDrift(invader.gridY)
            minBaseX = min(minBaseX, x)
            maxBaseX = max(maxBaseX, x + invader.width)
        }

        val minOffsetAllowed = leftBound - minBaseX
        val maxOffsetAllowed = rightBound - maxBaseX

        val nextOffset = if (minOffsetAllowed > maxOffsetAllowed) {
            (minOffsetAllowed + maxOffsetAllowed) * 0.5f
        } else {
            var proposed = invaderFormationOffset + invaderSpeed * invaderDirection * dt
            if (proposed < minOffsetAllowed) {
                val overshoot = minOffsetAllowed - proposed
                proposed = minOffsetAllowed + overshoot
                if (invaderDirection < 0f) {
                    invaderDirection = 1f
                    playInvaderTurnSound()
                }
            }
            if (proposed > maxOffsetAllowed) {
                val overshoot = proposed - maxOffsetAllowed
                proposed = maxOffsetAllowed - overshoot
                if (invaderDirection > 0f) {
                    invaderDirection = -1f
                    playInvaderTurnSound()
                }
            }
            proposed.coerceIn(minOffsetAllowed, maxOffsetAllowed)
        }

        invaderFormationOffset = nextOffset
        invaders.forEach { invader ->
            val drift = rowDrift(invader.gridY)
            invader.x = invader.baseX + invaderFormationOffset + drift
        }
    }

    private fun playInvaderTurnSound() {
        if (invaderTurnSoundCooldown > 0f) return
        audio.play(GameSound.BRICK_MOVING, 0.12f, 0.92f)
        invaderTurnSoundCooldown = invaderTurnSoundMinInterval
    }

    private fun compressInvaderX(rawX: Float, brickWidth: Float): Float {
        val center = worldWidth * 0.5f
        val brickCenter = rawX + brickWidth * 0.5f
        val compressedCenter = center + (brickCenter - center) * invaderFormationCompression
        return compressedCenter - brickWidth * 0.5f
    }

    private fun attachBallToPaddle() {
        balls.firstOrNull()?.let { ball ->
            ball.x = paddle.x
            ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
            ball.vx = 0f
            ball.vy = 0f
            ball.trail.clear()
            ball.trailTimer = 0f
        }
    }

    private fun launchBall() {
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

    private fun launchBallWithAim(ball: Ball, angleOffset: Float = 0f) {
        val levelBoost = (1f + levelIndex * speedBoostSlope()).coerceAtMost(speedBoostCap())
        val speed = config.mode.launchSpeed * levelBoost
        val angle = (aimAngle + angleOffset).coerceIn(aimMinAngle, Math.PI.toFloat() - aimMinAngle)
        ball.vx = speed * kotlin.math.cos(angle)
        ball.vy = (speed * kotlin.math.sin(angle)).coerceAtLeast(speed * 0.18f)
        ball.stuckToPaddle = false

        // Track tunnel shots fired
        if (config.mode == GameMode.TUNNEL) {
            tunnelShotsFired += 1
            tunnelGateFlash = 1f
        }
    }

    private fun releaseStuckBalls() {
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

    private fun launchVolleyTurn() {
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

    private fun updateVolleyLaunchQueue(dt: Float) {
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

    private fun spawnBall(spawnX: Float = paddle.x) {
        val ball = Ball(spawnX, paddle.y + 5f, 0.92f, 0f, 0f)
        if (fireballActive) {
            ball.isFireball = true
            ball.color = PowerUpType.FIREBALL.color
        } else if (pierceActive) {
            ball.color = PowerUpType.PIERCE.color
        }
        balls.add(ball)
    }

    private fun updateBalls(dt: Float) {
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
                            loseLife()
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
    }

    private fun resolveVolleyTurnIfReady() {
        if (!volleyTurnActive) return
        if (volleyQueuedBalls > 0 || balls.isNotEmpty()) return

        volleyTurnActive = false
        volleyTurnCount += 1
        volleyAdvanceRows += 1
        audio.play(GameSound.BRICK_MOVING, 0.36f, 0.88f)
        renderer?.triggerScreenShake(0.7f, 0.07f)
        if (shouldAwardVolleyBall(volleyTurnCount) && volleyBallCount < 20) {
            volleyBallCount += 1
            listener.onVolleyBallsUpdated(volleyBallCount)
            listener.onTip("Volley +1 ball (${volleyBallCount} total).")
            audio.play(GameSound.POWERUP, 0.32f, 1.1f)
        }

        relayoutBricks()
        if (hasVolleyBreach()) {
            logger?.logGameOver(score, levelIndex + 1, "volley_breach")
            listener.onTip("Breach! Bricks reached the launch line.")
            triggerGameOver()
            return
        }

        spawnVolleyTopRow()
        relayoutBricks()
        if (hasVolleyBreach()) {
            logger?.logGameOver(score, levelIndex + 1, "volley_breach")
            listener.onTip("Breach! Bricks reached the launch line.")
            triggerGameOver()
            return
        }

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
        volleyReturnAnchorX = Float.NaN
        volleyReturnSumX = 0f
        volleyReturnCount = 0
        spawnBall(paddle.x)
        syncAimForLaunch()
        state = GameState.READY
        updatePowerupStatus()
    }

    private fun shouldAwardVolleyBall(turnCount: Int): Boolean {
        return when {
            turnCount <= 3 -> true
            turnCount <= 10 -> turnCount % 2 == 0
            else -> turnCount % 3 == 0
        }
    }

    private fun spawnVolleyTopRow() {
        val layout = currentLayout ?: return
        val cols = effectiveVolleyColumns(layout.cols + layoutColBoost)
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
            volleyTurnCount < 2 -> 0.16f
            volleyTurnCount < 5 -> 0.09f
            else -> 0f
        }
        val density = (0.68f + levelIndex * 0.009f + volleyTurnCount * 0.005f - congestionPenalty - earlyEase)
            .coerceIn(0.46f, 0.87f)

        val ballRelief = ((volleyBallCount - 5).coerceAtLeast(0) * 0.04f).coerceAtMost(0.24f)
        val hpScale = (1f + levelIndex * 0.06f + volleyTurnCount * 0.024f - ballRelief).coerceAtLeast(0.85f)

        val danger = (volleyTurnCount / 10f).coerceIn(0f, 1f)
        val explosiveChance = (0.035f + danger * 0.05f).coerceAtMost(0.1f)
        val reinforcedChance = (0.11f + danger * 0.09f).coerceAtMost(0.23f)
        val armoredChance = if (volleyTurnCount < 2) 0f else (0.06f + danger * 0.08f).coerceAtMost(0.17f)
        val movingChance = if (volleyTurnCount < 4) 0f else 0.05f
        val phaseChance = if (volleyTurnCount < 6) 0f else 0.04f
        val spawningChance = if (volleyTurnCount < 8) 0f else 0.03f

        val forcedGapPrimary = random.nextInt(cols)
        val forcedGapSecondary = if (volleyTurnCount < 5 && cols >= 8) {
            (forcedGapPrimary + (cols / 3).coerceAtLeast(2)) % cols
        } else {
            -1
        }
        val forcedGapTertiary = if (volleyTurnCount < 8 && cols >= 11) {
            (forcedGapPrimary + (cols / 2)) % cols
        } else {
            -1
        }
        val forcedGaps = hashSetOf(forcedGapPrimary)
        if (forcedGapSecondary >= 0) forcedGaps.add(forcedGapSecondary)
        if (forcedGapTertiary >= 0) forcedGaps.add(forcedGapTertiary)
        var spawned = 0

        for (col in 0 until cols) {
            if (col in forcedGaps) continue
            if (occupied.contains(col)) continue
            if (random.nextFloat() > density) continue
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
                spawned = 1
            }
        }
        if (spawned > 0) {
            spatialHashDirty = true
            val spawnPitch = (0.94f + spawned.coerceAtMost(8) * 0.02f).coerceAtMost(1.1f)
            audio.play(GameSound.BRICK_SPAWNING, 0.34f, spawnPitch)
        }
    }

    private fun hasVolleyBreach(): Boolean {
        if (config.mode != GameMode.VOLLEY) return false
        val breachY = paddle.y + paddle.height * 0.5f + 1.8f
        return bricks.any { it.alive && it.y <= breachY }
    }

    private fun updateBallTrails(dt: Float) {
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

    private fun applyGravityWell(ball: Ball, dt: Float) {
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

    private fun handlePaddleCollision(ball: Ball) {
        if (ball.vy > 0f) return
        if (ball.y - ball.radius > paddle.y + paddle.height / 2f) return
        if (ball.y + ball.radius < paddle.y - paddle.height / 2f) return
        if (ball.x + ball.radius < paddle.x - paddle.width / 2f) return
        if (ball.x - ball.radius > paddle.x + paddle.width / 2f) return

        if (magnetActive) {
            val maxOffset = paddle.width * 0.4f
            ball.stuckToPaddle = true
            ball.stickOffset = (ball.x - paddle.x).coerceIn(-maxOffset, maxOffset)
            ball.x = paddle.x + ball.stickOffset
            ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
            ball.vx = 0f
            ball.vy = 0f
            audio.play(GameSound.POWERUP, 0.5f)
            if (!magnetCatchTipShown) {
                listener.onTip("Release to launch stuck balls.")
                magnetCatchTipShown = true
            }
            return
        }

        val hitPos = (ball.x - paddle.x) / (paddle.width / 2f)
        val spin = (paddleVelocity / 180f).coerceIn(-0.35f, 0.35f)
        val angle = (hitPos * 1.1f + spin).coerceIn(-1.15f, 1.15f)
        val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy).coerceAtLeast(28f)
        ball.vx = speed * angle
        ball.vy = abs(speed * (1.22f - abs(angle)))
        val minVy = speed * 0.35f
        if (ball.vy < minVy) {
            ball.vy = minVy
        }
        ball.y = paddle.y + paddle.height / 2f + ball.radius
        audio.play(GameSound.BOUNCE, 0.8f)
        spawnImpactSparks(ball.x, ball.y + ball.radius, theme.accent, 6, 16f)
    }

    private fun handleBrickCollision(ball: Ball) {
        val nearbyBricks = getNearbyBricks(ball)
        for (brick in nearbyBricks) {
            if (!brick.alive) continue
            if (!circleIntersectsRect(ball, brick)) continue

            // Resolve bounce before applying hit to ensure consistent collision response
            if (!fireballActive && !pierceActive) {
                bounceBallFromBrick(ball, brick)
            }
            val destroyed = brick.applyHit(fireballActive || pierceActive)

            if (destroyed) {
                updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
                runBricksBroken += 1
                spawnBrickDestructionFx(brick, ball.x, ball.y, intensity = 1f)

                if (brick.type == BrickType.BOSS) {
                    renderer?.triggerImpactFlash(0.4f)
                    renderer?.triggerScreenShake(3.6f, 0.24f)
                    if (waves.size < maxWaves) {
                        waves.add(
                            ExplosionWave(
                                x = brick.centerX,
                                y = brick.centerY,
                                radius = 1.5f,
                                color = brick.currentColor(theme).copyOf(),
                                life = 1.6f,
                                maxLife = 1.6f,
                                speed = 26f
                            )
                        )
                    }
                    spawnPowerup(brick.centerX, brick.centerY, randomPowerupType())
                    listener.onTip("Boss down! Powerup dropped.")
                }

                // Combo system: consecutive breaks within 2 seconds get multipliers
                comboTimer = 2f  // Reset combo timer
                val oldCombo = combo
                combo += 1

                // Spawn combo streak particles for milestones
                if (combo >= 5 && combo % 2 == 0 && combo > oldCombo) {
                    spawnComboStreakParticles(brick.centerX, brick.centerY, combo)
                }

                // Update daily challenges
                updateDailyChallenges(ChallengeType.COMBO_MULTIPLIER, combo)

                // Calculate multiplier based on combo
                val multiplier = when {
                    combo >= 10 -> 5f
                    combo >= 7 -> 3f
                    combo >= 4 -> 2f
                    combo >= 2 -> 1.5f
                    else -> 1f
                }

                // Combo flash effect for high multipliers
                if (multiplier >= 2f) {
                    renderer?.triggerComboFlash()
                }

                val baseScore = (brick.scoreValue * multiplier).roundToInt()
                addScore(baseScore)

                // Show combo feedback if significant
                if (combo >= 3) {
                    logger?.logComboAchieved(combo, multiplier, (brick.scoreValue * multiplier).toInt())
                    listener.onTip("Combo x${combo}!")
                }

                // Log brick destruction
                logger?.logBrickDestroyed(brick.type, Pair(brick.centerX, brick.centerY), combo)

                // Play appropriate sound for brick type
                val brickSound = when (brick.type) {
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

                // Calculate dynamic rate with combo scaling and random variation
                val baseRate = brickSoundRate(brick.type)
                val comboPitchBoost = (combo.coerceAtMost(10) * 0.02f).coerceAtMost(0.2f) // Up to +0.2 for high combos
                val randomVariation = (random.nextFloat() - 0.5f) * 0.3f // ±0.15 random variation
                val dynamicRate = (baseRate + comboPitchBoost + randomVariation).coerceIn(0.7f, 1.3f)

                audio.play(brickSound, 0.7f, dynamicRate)
                audio.haptic(GameHaptic.LIGHT)
                maybeSpawnPowerup(brick)
                if (brick.type == BrickType.EXPLOSIVE) {
                    triggerExplosion(brick)
                }
                if (brick.type == BrickType.SPAWNING) {
                    spawnChildBricks(brick)
                }
            } else {
                combo = 0
                comboTimer = 0f
                audio.play(GameSound.BOUNCE, 0.5f)
            }

            spawnImpactSparks(ball.x, ball.y, brick.currentColor(theme), 4, 12f)
            reportScore()
            break
        }
    }

    private fun handleBrickCollisionFromBeam(beam: Beam) {
        for (brick in bricks) {
            if (!brick.alive) continue
            if (!beamIntersectsBrick(beam, brick)) continue
            val destroyed = brick.applyHit(true)
            if (destroyed) {
                addScore(brick.scoreValue)
                updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
                runBricksBroken += 1

                // Visual effects
                renderer?.triggerScreenShake(2f, 0.15f)
                spawnBrickDestructionFx(brick, beam.x, beam.y, intensity = 0.84f)
                // Play appropriate sound for brick type (softer for beam hits)
                val brickSound = when (brick.type) {
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
                audio.play(brickSound, 0.4f, brickSoundRate(brick.type)) // Softer for beam hits
                maybeSpawnPowerup(brick)
                if (brick.type == BrickType.EXPLOSIVE) {
                    triggerExplosion(brick)
                }
                if (brick.type == BrickType.SPAWNING) {
                    spawnChildBricks(brick)
                }
            }
            reportScore()
            break
        }
    }

    private fun handleBeamCollision() {
        val iterator = beams.iterator()
        while (iterator.hasNext()) {
            val beam = iterator.next()
            var hit = false
            for (brick in bricks) {
                if (!brick.alive) continue
                if (!beamIntersectsBrick(beam, brick)) continue
                hit = true
                handleBrickCollisionFromBeam(beam)
                break
            }
            if (hit) iterator.remove()
        }
    }

    private fun updateBeams(dt: Float) {
        laserCooldown = max(0f, laserCooldown - dt)
        beams.forEach { beam ->
            beam.y += beam.speed * dt
        }
        beams.removeAll { it.y > worldHeight + 4f }
        handleBeamCollision()
    }

    private fun updatePowerups(dt: Float) {
        val iterator = powerups.iterator()
        while (iterator.hasNext()) {
            val power = iterator.next()

            // Apply magnet attraction if active
            if (magnetActive) {
                val dx = paddle.x - power.x
                val dy = paddle.y - power.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance > 1f) {
                    val attractSpeed = 80f / (distance + 1f) // Stronger when closer
                    val attractX = dx / distance * attractSpeed * dt
                    val attractY = dy / distance * attractSpeed * dt
                    power.x += attractX
                    power.y += attractY
                }
                power.y -= power.speed * dt * 0.6f
            } else {
                power.y -= power.speed * dt
            }

            if (power.y < -4f) {
                iterator.remove()
                continue
            }
            if (powerIntersectsPaddle(power)) {
                logger?.logPowerupCollected(power.type, Pair(power.x, power.y))
                applyPowerup(power.type)
                updateDailyChallenges(ChallengeType.POWERUPS_COLLECTED)
                audio.play(GameSound.POWERUP, 0.8f)
                spawnPowerupBurst(power)
                powerupCollectionPulse = 1f
                iterator.remove()
            }
        }
    }

    private fun maybeShowPowerupTip(type: PowerUpType) {
        if (!settings.tipsEnabled) return
        if (!powerupTipShown.add(type)) return
        val message = when (type) {
            PowerUpType.MULTI_BALL -> "Multi-ball: extra balls in play."
            PowerUpType.LASER -> "Laser: tap FIRE or two-finger tap."
            PowerUpType.GUARDRAIL -> "Guardrail: bottom safety net."
            PowerUpType.LIFE -> "1UP: gain an extra life."
            PowerUpType.SHIELD -> "Shield: blocks invader shots."
            PowerUpType.WIDE_PADDLE -> "Wide paddle: bigger hit area."
            PowerUpType.SHRINK -> "Shrink: paddle size reduced."
            PowerUpType.SLOW -> "Slow: ball speed reduced."
            PowerUpType.OVERDRIVE -> "Overdrive: everything speeds up."
            PowerUpType.FIREBALL -> "Fireball: smash through bricks."
            PowerUpType.MAGNET -> "Magnet: balls stick to the paddle."
            PowerUpType.GRAVITY_WELL -> "Gravity well: bends ball paths."
            PowerUpType.BALL_SPLITTER -> "Splitter: duplicates balls."
            PowerUpType.FREEZE -> "Freeze: slows everything."
            PowerUpType.PIERCE -> "Pierce: balls ignore armor."
            PowerUpType.RICOCHET -> "Ricochet: balls bounce twice off walls."
            PowerUpType.TIME_WARP -> "Time Warp: slows time but keeps ball speed."
            PowerUpType.DOUBLE_SCORE -> "2x Score: double points for limited time."
        }
        listener.onTip(message)
    }

    private fun updateInvaderShots(dt: Float) {
        if (!config.mode.invaders) return
        invaderShotTimer -= dt
        val invaders = collectAliveInvaders()
        if (invaders.isEmpty()) return
        val ratio = invaders.size.toFloat() / invaderTotal.toFloat().coerceAtLeast(1f)
        val paceBoost = (1f - ratio).coerceIn(0f, 1f)
        invaderSpeed = invaderBaseSpeed * (1f + paceBoost * 0.5f)
        invaderShotCooldown = (invaderBaseShotCooldown * (1f - paceBoost * 0.4f)).coerceIn(0.4f, invaderBaseShotCooldown)
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
                val volleyShots = (2 + levelIndex / 3).coerceAtMost(4)
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
            val maxShots = 6 + (levelIndex / 2).coerceAtMost(6)
            if (enemyShots.size < maxShots) {
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
                handleInvaderShotHit(shot)
                iterator.remove()
            }
        }
    }

    private fun spawnInvaderShot(origin: Brick) {
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

    private fun handleInvaderShotHit(shot: EnemyShot) {
        spawnImpactSparks(shot.x, shot.y, shot.color, 6, 12f)
        if (invaderShield > 0f) {
            val damage = (12f + levelIndex * 1.2f).coerceAtMost(22f)
            invaderShield = max(0f, invaderShield - damage)
            listener.onShieldUpdated(invaderShield.toInt(), invaderShieldMax.toInt())
            shieldHitPulse = 1f
            shieldHitX = shot.x
            shieldHitColor = adjustColor(shot.color, 1.2f, 1f)
            audio.play(GameSound.BOUNCE, 0.65f)
            audio.haptic(GameHaptic.LIGHT)
            renderer?.triggerScreenShake(1.1f, 0.08f)
            if (!invaderShieldCritical && invaderShieldMax > 0f && invaderShield <= invaderShieldMax * 0.25f) {
                invaderShieldCritical = true
                listener.onTip("Shield critical! Avoid direct hits.")
                audio.play(GameSound.EXPLOSION, 0.35f)
            }
            if (invaderShield <= 0f && !invaderShieldAlerted) {
                invaderShieldAlerted = true
                shieldBreakPulse = 1f
                renderer?.triggerImpactFlash(0.25f)
                audio.play(GameSound.EXPLOSION, 0.55f)
                renderer?.triggerScreenShake(2f, 0.2f)
                listener.onTip("Shield down! Dodge the incoming fire.")
            }
        } else {
            loseLife()
        }
    }

    private fun shotIntersectsPaddle(shot: EnemyShot): Boolean {
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

    private fun updateParticles(dt: Float) {
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

    private fun updateWaves(dt: Float) {
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

    private fun updateEffects(dt: Float) {
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
        updatePowerupStatus()
    }

    private fun syncPaddleWidthFromEffects() {
        val wideActive = activeEffects.containsKey(PowerUpType.WIDE_PADDLE)
        val shrinkActive = activeEffects.containsKey(PowerUpType.SHRINK)
        paddle.width = when {
            wideActive && shrinkActive -> basePaddleWidth
            wideActive -> basePaddleWidth * (25f / 18f)
            shrinkActive -> basePaddleWidth * 0.7f
            else -> basePaddleWidth
        }
    }

    private fun clampBallSpeed(ball: Ball) {
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

    private fun speedBoostSlope(): Float = ModeBalance.pacingFor(config.mode).speedBoostSlope

    private fun speedBoostCap(): Float = ModeBalance.pacingFor(config.mode).speedBoostCap

    private fun minSpeedFactor(): Float = ModeBalance.pacingFor(config.mode).minSpeedFactor

    private fun maxSpeedFactor(): Float = ModeBalance.pacingFor(config.mode).maxSpeedFactor

    private fun difficultyForMode(): Float {
        val pacing = ModeBalance.pacingFor(config.mode)
        return (pacing.difficultyBase + levelIndex * pacing.difficultySlope).coerceAtMost(3.0f)
    }

    private fun applyPowerup(type: PowerUpType) {
        when (type) {
            PowerUpType.MULTI_BALL -> {
                val newBalls = balls.map {
                    val extra = Ball(it.x, it.y, it.radius, -it.vx * 0.8f, it.vy * 0.9f)
                    if (fireballActive) {
                        extra.isFireball = true
                        extra.color = PowerUpType.FIREBALL.color
                    } else if (pierceActive) {
                        extra.color = PowerUpType.PIERCE.color
                    }
                    extra
                }
                // Limit total balls to 12 to prevent performance issues
                val ballsToAdd = minOf(2, maxOf(0, 12 - balls.size))
                balls.addAll(newBalls.take(ballsToAdd))
                updateDailyChallenges(ChallengeType.MULTI_BALL_ACTIVE)
            }
            PowerUpType.LASER -> {
                activeEffects[type] = 12f
                if (!laserTipShown) {
                    listener.onTip("Laser active: two-finger tap to fire.")
                    laserTipShown = true
                }
            }
            PowerUpType.GUARDRAIL -> {
                guardrailActive = true
                activeEffects[type] = 10f
            }
            PowerUpType.LIFE -> {
                if (config.mode == GameMode.VOLLEY) {
                    volleyBallCount = (volleyBallCount + 1).coerceAtMost(20)
                    listener.onVolleyBallsUpdated(volleyBallCount)
                } else if (!config.mode.godMode) {
                    lives += 1
                    listener.onLivesUpdated(lives)
                }
            }
            PowerUpType.SHIELD -> {
                shieldCharges = min(2, shieldCharges + 1)
                activeEffects[type] = 12f
            }
            PowerUpType.WIDE_PADDLE -> {
                activeEffects[type] = 12f
                syncPaddleWidthFromEffects()
            }
            PowerUpType.SHRINK -> {
                activeEffects[type] = 10f
                syncPaddleWidthFromEffects()
            }
            PowerUpType.SLOW -> {
                speedMultiplier = 0.8f
                activeEffects[type] = 8f
            }
            PowerUpType.OVERDRIVE -> {
                speedMultiplier = 1.2f
                activeEffects[type] = 8f
            }
            PowerUpType.FIREBALL -> {
                fireballActive = true
                activeEffects[type] = 10f
                syncBallStyles()
            }
            PowerUpType.MAGNET -> {
                magnetActive = true
                activeEffects[type] = 15f
                if (!magnetTipShown) {
                    listener.onTip("Magnet active: balls stick to the paddle. Release to shoot.")
                    magnetTipShown = true
                }
            }
            PowerUpType.GRAVITY_WELL -> {
                gravityWellActive = true
                activeEffects[type] = 8f
            }
            PowerUpType.BALL_SPLITTER -> {
                val newBalls = balls.flatMap { ball ->
                    listOf(
                        Ball(ball.x, ball.y, ball.radius, ball.vx * 0.7f, ball.vy * 0.7f),
                        Ball(ball.x, ball.y, ball.radius, -ball.vx * 0.7f, ball.vy * 0.7f)
                    ).map { newBall ->
                        if (fireballActive) {
                            newBall.isFireball = true
                            newBall.color = PowerUpType.FIREBALL.color
                        }
                        if (pierceActive) {
                            newBall.color = PowerUpType.PIERCE.color
                        }
                        newBall
                    }
                }
                // Limit total balls to 12 to prevent performance issues
                val ballsToAdd = minOf(4, maxOf(0, 12 - balls.size))
                balls.addAll(newBalls.take(ballsToAdd))
                updateDailyChallenges(ChallengeType.MULTI_BALL_ACTIVE)
            }
            PowerUpType.FREEZE -> {
                freezeActive = true
                speedMultiplier = 0.1f // Almost frozen
                activeEffects[type] = 5f
            }
            PowerUpType.PIERCE -> {
                pierceActive = true
                activeEffects[type] = 12f
                syncBallStyles()
            }
            PowerUpType.RICOCHET -> {
                // Give balls extra wall bounces before losing effect
                balls.forEach { ball ->
                    ball.ricochetBounces = (ball.ricochetBounces ?: 0) + 2
                }
                activeEffects[type] = 15f
            }
            PowerUpType.TIME_WARP -> {
                // Slow down bricks/enemies but keep ball speed normal
                activeEffects[type] = 10f
            }
            PowerUpType.DOUBLE_SCORE -> {
                // 2x score multiplier
                activeEffects[type] = 8f
            }
        }
        audio.haptic(GameHaptic.MEDIUM)
        updatePowerupStatus()
    }

    private fun syncBallStyles() {
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

    private fun updatePowerupStatus() {
        if (config.mode == GameMode.VOLLEY) {
            val status = "Volley balls: $volleyBallCount • Turn ${volleyTurnCount + 1}"
            if (status != lastPowerupStatus) {
                lastPowerupStatus = status
                listener.onPowerupStatus(status)
            }
            emitPowerupSnapshot(emptyList())
            return
        }
        if (config.mode == GameMode.TUNNEL) {
            val segments = mutableListOf<String>()
            segments.add("Shots: $tunnelShotsFired")
            if (combo >= 2) {
                segments.add("Combo x$combo")
            }
            val status = segments.joinToString(" • ")
            if (status != lastPowerupStatus) {
                lastPowerupStatus = status
                listener.onPowerupStatus(status)
            }
            emitPowerupSnapshot(buildPowerupSnapshot())
            return
        }
        if (config.mode == GameMode.SURVIVAL) {
            val segments = mutableListOf<String>()
            segments.add("Speed: ${String.format(Locale.US, "%.1f", speedMultiplier)}x")
            if (combo >= 2) {
                segments.add("Combo x$combo")
            }
            val status = segments.joinToString(" • ")
            if (status != lastPowerupStatus) {
                lastPowerupStatus = status
                listener.onPowerupStatus(status)
            }
            emitPowerupSnapshot(buildPowerupSnapshot())
            return
        }
        if (config.mode == GameMode.ZEN) {
            // Zen mode: minimal HUD, no scores or lives
            val status = "Zen Mode"
            if (status != lastPowerupStatus) {
                lastPowerupStatus = status
                listener.onPowerupStatus(status)
            }
            // Don't show powerup chips in zen mode.
            emitPowerupSnapshot(emptyList())
            return
        }
        val segments = mutableListOf<String>()
        val effectText = if (activeEffects.isEmpty()) {
            "Powerups: none"
        } else {
            activeEffects.entries
                .sortedBy { it.key.ordinal }
                .joinToString(" • ", prefix = "Powerups: ") { (type, time) ->
                    if (type == PowerUpType.SHIELD) {
                        "${type.displayName} x$shieldCharges ${time.toInt()}s"
                    } else {
                        "${type.displayName} ${time.toInt()}s"
                    }
                }
        }
        segments.add(effectText)
        if (combo >= 2) {
            segments.add("Combo x$combo")
        }
        val status = segments.joinToString(" • ")
        if (status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        emitPowerupSnapshot(buildPowerupSnapshot())
    }

    private fun buildPowerupSnapshot(): List<PowerupStatus> {
        return activeEffects.entries
            .sortedBy { it.key.ordinal }
            .map { (type, time) ->
                PowerupStatus(
                    type = type,
                    remainingSeconds = time.toInt(),
                    charges = if (type == PowerUpType.SHIELD) shieldCharges else 0
                )
            }
    }

    private fun emitPowerupSnapshot(snapshot: List<PowerupStatus>) {
        if (snapshot != lastPowerupSnapshot || combo != lastComboReported) {
            lastPowerupSnapshot = snapshot
            lastComboReported = combo
            listener.onPowerupsUpdated(snapshot, combo)
        }
    }

    private fun addScore(points: Int) {
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

    private fun reportScore() {
        updateScoreChallenges()
        listener.onScoreUpdated(score)
    }

    private fun updateScoreChallenges() {
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

    private fun checkLevelCompletion() {
        val remaining = bricks.count { it.alive && it.type != BrickType.UNBREAKABLE }
        if (remaining == 0) {
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
            logger?.logLevelComplete(levelIndex, score, elapsedSeconds, remaining)
            levelClearFlash = 1.0f
            renderer?.triggerLevelClearFlash()
            spawnLevelCompleteConfetti()
            val summary = GameSummary(
                score = score,
                level = levelIndex + 1,
                durationSeconds = elapsedSeconds.toInt(),
                bricksBroken = runBricksBroken,
                livesLost = runLivesLost
            )

            if (config.mode.godMode) {
                // Auto-advance in God Mode to ensure continuous play
                logger?.logLevelAdvance(levelIndex + 1)
                levelIndex += 1
                resetLevel(first = false)
            } else {
                awaitingNextLevel = true
                state = GameState.PAUSED
                stateBeforePause = GameState.PAUSED
                listener.onLevelComplete(summary)
            }
        }
    }

    private fun loseLife() {
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

    private fun triggerGameOver() {
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
        listener.onGameOver(summary)
        state = GameState.GAME_OVER
    }

    private fun shootLaser() {
        if (laserCooldown > 0f) return
        laserCooldown = laserCooldownDuration
        val beamOffset = paddle.width / 3f
        beams.add(Beam(paddle.x - beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        beams.add(Beam(paddle.x + beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        updateDailyChallenges(ChallengeType.LASER_FIRED)
        audio.play(GameSound.LASER, 0.6f)
        listener.onLaserFired(laserCooldownDuration)
    }

    private fun maybeSpawnPowerup(brick: Brick) {
        if (config.mode == GameMode.VOLLEY) return
        val baseChance = when (brick.type) {
            BrickType.EXPLOSIVE -> 0.30f
            BrickType.REINFORCED, BrickType.ARMORED -> 0.2f
            BrickType.BOSS, BrickType.PHASE -> 0.27f
            BrickType.SPAWNING -> 0.18f
            BrickType.MOVING -> 0.15f
            BrickType.INVADER -> 0.16f
            else -> 0.12f
        }
        val levelBoost = (levelIndex * 0.0035f).coerceAtMost(0.06f)
        val modeBoost = ModeBalance.pacingFor(config.mode).dropChanceModeBoost
        val dropChance = (baseChance + levelBoost + modeBoost).coerceIn(0.08f, 0.4f)
        if (random.nextFloat() < dropChance) {
            spawnPowerup(brick.centerX, brick.centerY, randomPowerupType())
        }
    }

    private fun spawnPowerup(x: Float, y: Float, type: PowerUpType) {
        powerups.add(PowerUp(x, y, type, 18f))
        recordPowerup(type)
        maybeShowPowerupTip(type)
        if (type == PowerUpType.LASER) {
            powerupDropsSinceLaser = 0
        } else {
            powerupDropsSinceLaser += 1
        }
    }

    fun debugSpawnPowerup(type: PowerUpType) {
        powerups.clear()
        val spawnX = worldWidth * 0.5f
        val spawnY = (worldHeight * 0.55f).coerceIn(paddle.y + 10f, worldHeight * 0.8f)
        spawnPowerup(spawnX, spawnY, type)
    }

    private fun triggerExplosion(brick: Brick) {
        audio.play(GameSound.EXPLOSION, 0.8f)
        audio.haptic(GameHaptic.HEAVY)
        renderer?.triggerImpactFlash(0.3f)
        renderer?.triggerScreenShake(2.8f, 0.18f)
        val radius = 1
        for (neighbor in bricks) {
            if (!neighbor.alive || neighbor.gridX < 0 || neighbor.gridY < 0 || !neighbor.isNeighbor(brick, radius)) continue
            if (neighbor == brick) continue
            val destroyed = neighbor.applyHit(true)
            if (destroyed) {
                addScore(neighbor.scoreValue)
                updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
                runBricksBroken += 1
                spawnBrickDestructionFx(neighbor, brick.centerX, brick.centerY, intensity = 0.92f)
                maybeSpawnPowerup(neighbor)
            }
        }
        reportScore()
    }

    private fun spawnImpactSparks(x: Float, y: Float, baseColor: FloatArray, count: Int, speed: Float) {
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

    private fun spawnBrickDestructionFx(brick: Brick, impactX: Float, impactY: Float, intensity: Float) {
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

        if (shakeStrength > 0f) {
            renderer?.triggerScreenShake(shakeStrength * fxScale, shakeDuration)
        }
        if (brick.type == BrickType.EXPLOSIVE || brick.type == BrickType.BOSS) {
            renderer?.triggerImpactFlash(0.14f + 0.1f * fxScale)
        }
    }

    private fun spawnInvaderBurst(brick: Brick, intensity: Float = 1f) {
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
        renderer?.triggerScreenShake(1.2f * fxScale, 0.12f)
    }

    private fun brickSoundRate(type: BrickType): Float {
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

    private fun randomPowerupType(): PowerUpType {
        if (powerupDropsSinceLaser >= 5 && !activeEffects.containsKey(PowerUpType.LASER)) {
            return PowerUpType.LASER
        }
        val weights = mutableMapOf(
            PowerUpType.MULTI_BALL to 1.0f,
            PowerUpType.LASER to 1.0f,
            PowerUpType.GUARDRAIL to 0.9f,
            PowerUpType.SHIELD to 0.95f,
            PowerUpType.WIDE_PADDLE to 1.0f,
            PowerUpType.SHRINK to 0.35f,
            PowerUpType.SLOW to 0.85f,
            PowerUpType.OVERDRIVE to 0.35f,
            PowerUpType.MAGNET to 0.9f,
            PowerUpType.LIFE to 0.55f,
            PowerUpType.FIREBALL to 0.95f,
            PowerUpType.GRAVITY_WELL to 0.8f,
            PowerUpType.BALL_SPLITTER to 0.85f,
            PowerUpType.FREEZE to 0.75f,
            PowerUpType.PIERCE to 0.9f,
            PowerUpType.RICOCHET to 0.8f,
            PowerUpType.TIME_WARP to 0.7f,
            PowerUpType.DOUBLE_SCORE to 0.6f
        )
        when (config.mode) {
            GameMode.TIMED -> {
                weights[PowerUpType.MULTI_BALL] = (weights[PowerUpType.MULTI_BALL] ?: 0f) + 0.25f
                weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.2f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.15f
                weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.8f }
            }
            GameMode.RUSH -> {
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.35f
                weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.25f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.2f
                weights[PowerUpType.LIFE] = (weights[PowerUpType.LIFE] ?: 0f) + 0.1f
            }
            GameMode.ENDLESS -> {
                weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.28f
                weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.22f
                weights[PowerUpType.GRAVITY_WELL] = (weights[PowerUpType.GRAVITY_WELL] ?: 0f) + 0.18f
                weights[PowerUpType.BALL_SPLITTER] = (weights[PowerUpType.BALL_SPLITTER] ?: 0f) + 0.18f
            }
            GameMode.SURVIVAL -> {
                weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.2f
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.2f
                weights[PowerUpType.LIFE] = (weights[PowerUpType.LIFE] ?: 0f) + 0.05f
                weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.7f }
                weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.7f }
            }
            GameMode.GOD -> {
                weights[PowerUpType.LIFE] = 0.15f
                weights[PowerUpType.SHRINK] = 0.1f
                weights[PowerUpType.OVERDRIVE] = 0.1f
            }
            GameMode.TUNNEL -> {
                weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.34f
                weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.22f
                weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.12f
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.14f
                weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.7f }
                weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.72f }
            }
            GameMode.INVADERS -> {
                weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.3f
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.2f
                weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.35f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.1f
            }
            else -> Unit
        }
        if (levelIndex <= 2) {
            weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.45f }
            weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.55f }
        }
        if (powerupsSinceDefense >= 3) {
            weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.4f
            weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.45f
            weights[PowerUpType.WIDE_PADDLE] = (weights[PowerUpType.WIDE_PADDLE] ?: 0f) + 0.25f
        }
        if (powerupsSinceOffense >= 3) {
            weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.3f
            weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.28f
            weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.24f
            weights[PowerUpType.BALL_SPLITTER] = (weights[PowerUpType.BALL_SPLITTER] ?: 0f) + 0.2f
        }
        if (powerupsSinceControl >= 4) {
            weights[PowerUpType.MAGNET] = (weights[PowerUpType.MAGNET] ?: 0f) + 0.3f
            weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.2f
            weights[PowerUpType.FREEZE] = (weights[PowerUpType.FREEZE] ?: 0f) + 0.2f
            weights[PowerUpType.GRAVITY_WELL] = (weights[PowerUpType.GRAVITY_WELL] ?: 0f) + 0.18f
        }
        if (activeEffects.isNotEmpty()) {
            activeEffects.keys.forEach { type ->
                weights[type]?.let { weights[type] = it * 0.55f }
            }
        }
        if (recentPowerups.isNotEmpty()) {
            val recentCounts = recentPowerups.groupingBy { it }.eachCount()
            recentCounts.forEach { (type, count) ->
                val penalty = when (count) {
                    1 -> 0.65f
                    2 -> 0.4f
                    else -> 0.25f
                }
                weights[type]?.let { weights[type] = it * penalty }
            }
            val recent = recentPowerups.toList()
            val last = recent.lastOrNull()
            if (last != null) {
                weights[last]?.let { weights[last] = it * 0.35f }
            }
            if (recent.size >= 2 && recent[recent.lastIndex] == recent[recent.lastIndex - 1]) {
                val repeatedBucket = powerupBucket(recent[recent.lastIndex])
                weights.keys.toList().forEach { type ->
                    val current = weights[type] ?: return@forEach
                    weights[type] = if (powerupBucket(type) == repeatedBucket) {
                        current * 0.42f
                    } else {
                        current * 1.16f
                    }
                }
            }
        }
        val total = weights.values.sum().coerceAtLeast(0.01f)
        val roll = random.nextFloat() * total
        var acc = 0f
        for ((type, weight) in weights) {
            acc += weight
            if (roll <= acc) return type
        }
        return PowerUpType.MULTI_BALL
    }

    private enum class PowerupBucket { OFFENSE, DEFENSE, CONTROL, RISK }

    private fun powerupBucket(type: PowerUpType): PowerupBucket {
        return when (type) {
            PowerUpType.MULTI_BALL,
            PowerUpType.LASER,
            PowerUpType.FIREBALL,
            PowerUpType.BALL_SPLITTER,
            PowerUpType.PIERCE -> PowerupBucket.OFFENSE
            PowerUpType.GUARDRAIL,
            PowerUpType.SHIELD,
            PowerUpType.LIFE,
            PowerUpType.WIDE_PADDLE -> PowerupBucket.DEFENSE
            PowerUpType.SLOW,
            PowerUpType.FREEZE,
            PowerUpType.MAGNET,
            PowerUpType.GRAVITY_WELL -> PowerupBucket.CONTROL
            PowerUpType.SHRINK,
            PowerUpType.OVERDRIVE,
            PowerUpType.RICOCHET,
            PowerUpType.TIME_WARP,
            PowerUpType.DOUBLE_SCORE -> PowerupBucket.RISK
        }
    }

    private fun recordPowerup(type: PowerUpType) {
        recentPowerups.addLast(type)
        while (recentPowerups.size > recentPowerupLimit) {
            recentPowerups.removeFirst()
        }
        when (powerupBucket(type)) {
            PowerupBucket.OFFENSE -> {
                powerupsSinceOffense = 0
                powerupsSinceDefense += 1
                powerupsSinceControl += 1
            }
            PowerupBucket.DEFENSE -> {
                powerupsSinceDefense = 0
                powerupsSinceOffense += 1
                powerupsSinceControl += 1
            }
            PowerupBucket.CONTROL -> {
                powerupsSinceControl = 0
                powerupsSinceOffense += 1
                powerupsSinceDefense += 1
            }
            PowerupBucket.RISK -> {
                powerupsSinceOffense += 1
                powerupsSinceDefense += 1
                powerupsSinceControl += 1
            }
        }
        powerupsSinceOffense = powerupsSinceOffense.coerceAtMost(12)
        powerupsSinceDefense = powerupsSinceDefense.coerceAtMost(12)
        powerupsSinceControl = powerupsSinceControl.coerceAtMost(12)
    }

    private fun spawnPowerupBurst(power: PowerUp) {
        val available = maxParticles - particles.size
        val count = min(8, max(0, available))
        repeat(count) { index ->
            val angle = (index / 6f) * (Math.PI.toFloat() * 2f)
            val speed = 14f + random.nextFloat() * 10f
            particles.add(
                Particle(
                    x = power.x,
                    y = power.y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    radius = 0.5f,
                    life = 0.45f,
                    color = power.type.color
                )
            )
        }
    }

    private fun spawnComboStreakParticles(x: Float, y: Float, combo: Int) {
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

    private fun spawnLevelCompleteConfetti() {
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

    private fun spawnChildBricks(parentBrick: Brick) {
        // Spawn 2-3 smaller bricks around the destroyed spawning brick
        val spawnCount = 2 + kotlin.random.Random(parentBrick.gridX * 13 + parentBrick.gridY * 19).nextInt(2)
        val childSize = parentBrick.width * 0.6f
        val childHeight = parentBrick.height * 0.6f

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
        }
        spatialHashDirty = true
    }

    private fun powerIntersectsPaddle(power: PowerUp): Boolean {
        val halfSize = power.size * 0.5f
        val powerLeft = power.x - halfSize
        val powerRight = power.x + halfSize
        val powerBottom = power.y - halfSize
        val powerTop = power.y + halfSize
        val paddleLeft = paddle.x - paddle.width / 2f
        val paddleRight = paddle.x + paddle.width / 2f
        val paddleBottom = paddle.y - paddle.height / 2f
        val paddleTop = paddle.y + paddle.height / 2f
        return powerRight > paddleLeft && powerLeft < paddleRight && powerTop > paddleBottom && powerBottom < paddleTop
    }

    private fun circleIntersectsRect(ball: Ball, brick: Brick): Boolean {
        val closestX = ball.x.coerceIn(brick.x, brick.x + brick.width)
        val closestY = ball.y.coerceIn(brick.y, brick.y + brick.height)
        val dx = ball.x - closestX
        val dy = ball.y - closestY
        return dx * dx + dy * dy <= ball.radius * ball.radius
    }

    private fun bounceBallFromBrick(ball: Ball, brick: Brick) {
        val overlapLeft = ball.x + ball.radius - brick.x
        val overlapRight = brick.x + brick.width - (ball.x - ball.radius)
        val overlapBottom = ball.y + ball.radius - brick.y
        val overlapTop = brick.y + brick.height - (ball.y - ball.radius)

        val minOverlapX = min(overlapLeft, overlapRight)
        val minOverlapY = min(overlapBottom, overlapTop)

        if (minOverlapX < minOverlapY) {
            if (overlapLeft < overlapRight) {
                ball.x = brick.x - ball.radius
                ball.vx = -abs(ball.vx)
            } else {
                ball.x = brick.x + brick.width + ball.radius
                ball.vx = abs(ball.vx)
            }
        } else {
            if (overlapBottom < overlapTop) {
                ball.y = brick.y - ball.radius
                ball.vy = -abs(ball.vy)
            } else {
                ball.y = brick.y + brick.height + ball.radius
                ball.vy = abs(ball.vy)
            }
        }
    }

    private fun beamIntersectsBrick(beam: Beam, brick: Brick): Boolean {
        val beamLeft = beam.x - beam.width / 2f
        val beamRight = beam.x + beam.width / 2f
        val beamBottom = beam.y - beam.height / 2f
        val beamTop = beam.y + beam.height / 2f
        val brickLeft = brick.x
        val brickRight = brick.x + brick.width
        val brickBottom = brick.y
        val brickTop = brick.y + brick.height
        return beamRight > brickLeft && beamLeft < brickRight && beamTop > brickBottom && beamBottom < brickTop
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
        private val ROW_BANDS = arrayOf(
            floatArrayOf(0.08f, -0.02f, -0.05f),
            floatArrayOf(-0.03f, 0.06f, 0.02f),
            floatArrayOf(0.02f, 0.04f, -0.06f),
            floatArrayOf(-0.05f, -0.01f, 0.07f),
            floatArrayOf(0.06f, -0.04f, 0.03f)
        )
        private val COL_BANDS = arrayOf(
            floatArrayOf(0.04f, 0.01f, -0.03f),
            floatArrayOf(-0.02f, 0.05f, 0.02f),
            floatArrayOf(0.03f, -0.04f, 0.04f),
            floatArrayOf(-0.04f, -0.02f, 0.05f)
        )
        private val COOL_VARIANTS = arrayOf(
            floatArrayOf(0.32f, 0.84f, 0.98f),
            floatArrayOf(0.45f, 0.75f, 0.99f),
            floatArrayOf(0.46f, 0.88f, 0.76f),
            floatArrayOf(0.62f, 0.64f, 0.98f),
            floatArrayOf(0.86f, 0.62f, 0.95f),
            floatArrayOf(0.95f, 0.7f, 0.45f),
            floatArrayOf(0.56f, 0.94f, 0.5f),
            floatArrayOf(0.94f, 0.58f, 0.78f)
        )
        private val WARM_VARIANTS = arrayOf(
            floatArrayOf(0.98f, 0.56f, 0.34f),
            floatArrayOf(0.95f, 0.72f, 0.4f),
            floatArrayOf(0.98f, 0.45f, 0.5f),
            floatArrayOf(0.88f, 0.68f, 0.3f),
            floatArrayOf(0.94f, 0.55f, 0.74f),
            floatArrayOf(0.74f, 0.76f, 0.4f),
            floatArrayOf(0.7f, 0.58f, 0.9f),
            floatArrayOf(0.89f, 0.46f, 0.36f)
        )
        private val BALANCED_VARIANTS = arrayOf(
            floatArrayOf(0.54f, 0.84f, 0.97f),
            floatArrayOf(0.91f, 0.64f, 0.42f),
            floatArrayOf(0.45f, 0.9f, 0.65f),
            floatArrayOf(0.89f, 0.56f, 0.84f),
            floatArrayOf(0.96f, 0.79f, 0.38f),
            floatArrayOf(0.59f, 0.63f, 0.99f),
            floatArrayOf(0.94f, 0.5f, 0.56f),
            floatArrayOf(0.7f, 0.88f, 0.5f)
        )
        private val BIAS_NORMAL = floatArrayOf(0f, 0f, 0f)
        private val BIAS_REINFORCED = floatArrayOf(0.04f, -0.02f, 0.03f)
        private val BIAS_ARMORED = floatArrayOf(-0.02f, 0.04f, -0.01f)
        private val BIAS_EXPLOSIVE = floatArrayOf(0.12f, -0.08f, -0.07f)
        private val BIAS_UNBREAKABLE = floatArrayOf(-0.04f, -0.03f, 0.06f)
        private val BIAS_MOVING = floatArrayOf(-0.01f, 0.08f, 0.03f)
        private val BIAS_SPAWNING = floatArrayOf(0.03f, 0.01f, 0.08f)
        private val BIAS_PHASE = floatArrayOf(0.08f, 0.06f, -0.04f)
        private val BIAS_BOSS = floatArrayOf(0.12f, -0.07f, -0.05f)
        private val BIAS_INVADER = floatArrayOf(0.03f, 0.08f, 0.1f)
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
    private var cachedThemeName: String? = null
    private var cachedHitPoints: Int = -1
    private var cachedColor: FloatArray? = null
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
                        hitPoints = maxHitPoints / (phase + 1)
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

    private fun variantsForTheme(themeName: String): Array<FloatArray> {
        return when (themeName) {
            "Sunset", "Lava", "Ember" -> WARM_VARIANTS
            "Neon", "Cobalt", "Circuit", "Invaders", "Vapor" -> COOL_VARIANTS
            else -> BALANCED_VARIANTS
        }
    }

    private fun biasForType(): FloatArray {
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

    private fun tintMixForType(): Float {
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

    private fun diversityForType(): Float {
        return when (type) {
            BrickType.NORMAL -> 1f
            BrickType.MOVING, BrickType.PHASE, BrickType.SPAWNING, BrickType.INVADER -> 0.9f
            BrickType.BOSS -> 0.65f
            BrickType.UNBREAKABLE -> 0.55f
            else -> 0.75f
        }
    }

    private fun mix(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }

    private fun positiveMod(value: Int, size: Int): Int {
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
