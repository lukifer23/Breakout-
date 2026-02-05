package com.breakoutplus.game

import android.view.MotionEvent
import com.breakoutplus.SettingsManager
import com.breakoutplus.game.LevelFactory.buildLevel
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.ceil
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

    private var paddle = Paddle(x = worldWidth / 2f, y = 8f, width = 18f, height = 2.6f)
    private var basePaddleWidth = paddle.width
    private var paddleVelocity = 0f

    private var state = GameState.READY
    private var stateBeforePause = state
    private var score = 0
    private var levelIndex = 0
    private var lives = config.mode.baseLives
    private var timeRemaining = config.mode.timeLimitSeconds.toFloat()
    private var lastReportedSecond = -1
    private var elapsedSeconds = 0f
    private var levelStartTime = 0f
    private var combo = 0
    private var comboTimer = 0f
    private var guardrailActive = false
    private var shieldCharges = 0
    private var laserCooldown = 0f
    private var speedMultiplier = 1f
    private var fireballActive = false
    private var magnetActive = false
    private var gravityWellActive = false
    private var freezeActive = false
    private var pierceActive = false
    private var explosiveTipShown = false
    private var lastPowerupStatus = ""
    private var lostLifeThisLevel = false
    private var laserTipShown = false
    private var magnetTipShown = false
    private var magnetCatchTipShown = false
    private var godModeTipShown = false
    private var invaderDirection = 1f
    private var invaderSpeed = 6f
    private var invaderShotTimer = 0f
    private var invaderShotCooldown = 1.6f
    private var invaderShield = 0f
    private var invaderShieldMax = 0f
    private var invaderShieldAlerted = false
    private var aimNormalized = 0f
    private var aimAngle = 0.72f
    private var aimDirection = 1f
    private var aimHasInput = false

    private var theme: LevelTheme = LevelThemes.DEFAULT
    private var currentLayout: LevelFactory.LevelLayout? = null
    private var currentAspectRatio = worldHeight / worldWidth
    private var brickAreaTopRatio = 0.92f
    private var brickAreaBottomRatio = 0.52f
    private var brickSpacing = 0.42f
    private var layoutRowBoost = 0
    private var layoutColBoost = 0
    private var invaderScale = 1f
    private var screenFlash = 0f
    private var levelClearFlash = 0f
    private val hitFlashDecayRate = 2.0f
    private val maxParticles = 240
    private val maxWaves = 10
    private val trailLife = 0.28f
    private val maxTrailPoints = 14
    private val aimMinAngle = 0.35f
    private val aimMaxAngle = 1.15f

    init {
        logger?.logSessionStart(config.mode)
        listener.onModeUpdated(config.mode)
        resetLevel(first = true)
        listener.onLivesUpdated(lives)
        reportScore()
        listener.onLevelUpdated(levelIndex + 1)
        listener.onPowerupStatus("Powerups: none")
        lastPowerupStatus = "Powerups: none"
        if (config.mode.timeLimitSeconds > 0) {
            listener.onTimeUpdated(timeRemaining.toInt())
        } else {
            listener.onTimeUpdated(0)
        }
    }

    fun onResize(width: Int, height: Int) {
        worldWidth = 100f
        worldHeight = worldWidth * (height.toFloat() / width.toFloat())
        paddle.y = 8f
        basePaddleWidth = 18f
        paddle.width = basePaddleWidth
        paddle.height = 2.6f
        currentAspectRatio = worldHeight / worldWidth
        applyLayoutTuning(currentAspectRatio, preserveRowBoost = true)
        relayoutBricks()
    }

    fun update(delta: Float) {
        val dt = delta * speedMultiplier
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) {
            return
        }
        updateTimers(dt)
        updatePaddle(delta)
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
            updateBalls(dt)
            updateBallTrails(dt)
            updateBeams(dt)
            updatePowerups(dt)
            updateInvaderShots(dt)
            updateParticles(dt)
            updateWaves(dt)
            checkLevelCompletion()
        }
    }

    fun render(renderer: Renderer2D) {
        renderer.setWorldSize(worldWidth, worldHeight)
        // Enhanced background with subtle gradient and flash effect
        val flashIntensity = screenFlash + levelClearFlash * 0.8f
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
            val gradientColor = floatArrayOf(
                bgTop[0] * (1f - ratio) + bgBottom[0] * ratio,
                bgTop[1] * (1f - ratio) + bgBottom[1] * ratio,
                bgTop[2] * (1f - ratio) + bgBottom[2] * ratio,
                1f
            )
            renderer.drawRect(0f, y, worldWidth, stepHeight, gradientColor)
        }

        // Add theme-specific background effects
        val time = System.nanoTime() / 1_000_000_000f
        when (theme.name) {
            "Neon" -> {
                // Animated grid pattern
                val gridSize = 8f
                for (x in 0 until (worldWidth / gridSize).toInt()) {
                    for (y in 0 until (worldHeight / gridSize).toInt()) {
                        if ((x + y) % 2 == 0) {
                            val alpha = (kotlin.math.sin(time * 2f + x * 0.5f + y * 0.3f) * 0.5f + 0.5f) * 0.1f
                            renderer.drawRect(x * gridSize, y * gridSize, gridSize, gridSize,
                                            floatArrayOf(0.3f, 0.9f, 1f, alpha))
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
                    renderer.drawCircle(x, y, size, floatArrayOf(1f, 0.6f, 0.3f, 0.3f))
                }
            }
            "Aurora" -> {
                // Wave patterns
                for (i in 0 until 8) {
                    val waveY = worldHeight * 0.3f + kotlin.math.sin(time + i * 0.8f) * worldHeight * 0.2f
                    val alpha = (kotlin.math.sin(time * 1.5f + i) * 0.5f + 0.5f) * 0.15f
                    renderer.drawRect(0f, waveY, worldWidth, 2f,
                                    floatArrayOf(0.3f, 0.8f, 0.5f, alpha))
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
                    renderer.drawCircle(x, y, size, floatArrayOf(0.6f, 0.8f, 1f, alpha))
                }
            }
        }

        if (guardrailActive) {
            val pulse = (kotlin.math.sin(time * 3f) * 0.5f + 0.5f)
            val guardColor = floatArrayOf(theme.accent[0], theme.accent[1], theme.accent[2], 0.5f + pulse * 0.4f)
            renderer.drawRect(0f, 2f, worldWidth, 0.6f, guardColor)
        }

        bricks.filter { it.alive }.forEach { brick ->
            val color = brick.currentColor(theme)

            if (brick.type == BrickType.INVADER) {
                drawInvaderShip(renderer, brick, color)
                return@forEach
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
                BrickType.UNBREAKABLE -> drawStripe(renderer, brick, adjustColor(color, 0.7f, 1f), 3)
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
                        0 -> floatArrayOf(0f, 1f, 0f, 0.8f) // Green
                        1 -> floatArrayOf(1f, 1f, 0f, 0.8f) // Yellow
                        else -> floatArrayOf(1f, 0f, 0f, 0.8f) // Red
                    }
                    val barHeight = brick.height * 0.1f
                    val barY = brick.y + brick.height - barHeight
                    renderer.drawRect(brick.x, barY, brick.width, barHeight, phaseColor)
                }
                BrickType.BOSS -> {
                    // Boss indicator (pulsing border)
                    val time = System.nanoTime() / 1_000_000_000f
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
        }

        beams.forEach { beam ->
            renderer.drawBeam(beam.x, beam.y, beam.width, beam.height, beam.color)
        }

        enemyShots.forEach { shot ->
            val glow = adjustColor(shot.color, 1.2f, 0.45f)
            renderer.drawCircle(shot.x, shot.y, shot.radius * 1.8f, glow)
            val trailAlpha = 0.35f
            renderer.drawRect(
                shot.x - shot.radius * 0.25f,
                shot.y + shot.radius * 0.6f,
                shot.radius * 0.5f,
                shot.radius * 1.6f,
                floatArrayOf(shot.color[0], shot.color[1], shot.color[2], trailAlpha)
            )
            renderer.drawCircle(shot.x, shot.y, shot.radius, shot.color)
        }

        waves.forEach { wave ->
            renderer.drawCircle(wave.x, wave.y, wave.radius, wave.color)
        }
        particles.forEach { particle ->
            renderer.drawCircle(particle.x, particle.y, particle.radius, particle.color)
        }

        if (state == GameState.READY || balls.any { it.stuckToPaddle }) {
            renderAimGuide(renderer)
        }

        if (activeEffects.containsKey(PowerUpType.LASER) || shieldCharges > 0) {
            val glowAlpha = if (activeEffects.containsKey(PowerUpType.LASER)) 0.55f else 0.35f
            val glowColor = floatArrayOf(theme.accent[0], theme.accent[1], theme.accent[2], glowAlpha)
            renderer.drawRect(
                paddle.x - paddle.width / 2f - 1.2f,
                paddle.y - paddle.height / 2f - 0.6f,
                paddle.width + 2.4f,
                paddle.height + 1.2f,
                glowColor
            )
        }
        renderer.drawRect(paddle.x - paddle.width / 2f, paddle.y - paddle.height / 2f, paddle.width, paddle.height, theme.paddle)

        balls.forEach { ball ->
            val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            val glowStrength = (speed / 70f).coerceIn(0.2f, 0.55f)
            val glowBase = when {
                ball.isFireball -> PowerUpType.FIREBALL.color
                pierceActive -> PowerUpType.PIERCE.color
                else -> theme.accent
            }

            ball.trail.forEach { point ->
                val lifeRatio = (point.life / point.maxLife).coerceIn(0f, 1f)
                val alpha = lifeRatio * 0.45f
                val trailColor = floatArrayOf(glowBase[0], glowBase[1], glowBase[2], alpha)
                renderer.drawCircle(point.x, point.y, point.radius * lifeRatio, trailColor)
            }

            val glowColor = floatArrayOf(glowBase[0], glowBase[1], glowBase[2], glowStrength)
            renderer.drawCircle(ball.x, ball.y, ball.radius * 1.8f, glowColor)
            renderer.drawCircle(ball.x, ball.y, ball.radius, ball.color)
        }
    }

    private fun renderPowerup(renderer: Renderer2D, power: PowerUp) {
        val time = System.nanoTime() / 1_000_000_000f
        val pulse = (kotlin.math.sin(time * 3f) * 0.5f + 0.5f)
        val size = power.size * (0.9f + pulse * 0.12f)

        // Enhanced outer glow effect
        val glowColor = adjustColor(power.type.color, 0.25f + pulse * 0.2f, 0.6f)
        renderer.drawRect(power.x - size * 0.6f, power.y - size * 0.6f, size * 1.2f, size * 1.2f, glowColor)

        // Main powerup body with gradient
        val outer = adjustColor(power.type.color, 0.7f, 1f)
        val inner = adjustColor(power.type.color, 1.1f + pulse * 0.05f, 1f)

        // Draw with rounded appearance using multiple rects
        val cornerInset = size * 0.1f
        val x = power.x - size / 2f
        val y = power.y - size / 2f
        renderer.drawRect(x, y, size, size, outer)
        renderer.drawRect(x + cornerInset, y + cornerInset, size - cornerInset * 2f, size - cornerInset * 2f, inner)

        val glyph = floatArrayOf(0.95f, 0.96f, 1f, 1f)
        when (power.type) {
            PowerUpType.MULTI_BALL -> {
                renderer.drawCircle(power.x - size * 0.18f, power.y, size * 0.16f, glyph)
                renderer.drawCircle(power.x + size * 0.18f, power.y, size * 0.16f, glyph)
            }
            PowerUpType.LASER -> {
                renderer.drawRect(power.x - size * 0.08f, power.y - size * 0.25f, size * 0.16f, size * 0.5f, glyph)
            }
            PowerUpType.GUARDRAIL -> {
                renderer.drawRect(power.x - size * 0.28f, power.y - size * 0.05f, size * 0.56f, size * 0.1f, glyph)
            }
            PowerUpType.SHIELD -> {
                renderer.drawCircle(power.x, power.y + size * 0.02f, size * 0.22f, glyph)
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.16f, size * 0.36f, size * 0.16f, glyph)
            }
            PowerUpType.WIDE_PADDLE -> {
                renderer.drawRect(power.x - size * 0.3f, power.y - size * 0.08f, size * 0.6f, size * 0.16f, glyph)
            }
            PowerUpType.SLOW -> {
                renderer.drawCircle(power.x, power.y, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.3f, size * 0.04f, size * 0.24f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y + size * 0.06f, size * 0.16f, size * 0.04f, glyph)
            }
            PowerUpType.FIREBALL -> {
                renderer.drawCircle(power.x, power.y, size * 0.2f, glyph)
                renderer.drawRect(power.x - size * 0.04f, power.y + size * 0.14f, size * 0.08f, size * 0.18f, glyph)
            }
            PowerUpType.LIFE -> {
                renderer.drawRect(power.x - size * 0.08f, power.y - size * 0.22f, size * 0.16f, size * 0.44f, glyph)
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.08f, size * 0.44f, size * 0.16f, glyph)
            }
            PowerUpType.MAGNET -> {
                // Horseshoe magnet shape
                renderer.drawRect(power.x - size * 0.15f, power.y - size * 0.1f, size * 0.3f, size * 0.2f, glyph)
                renderer.drawRect(power.x - size * 0.25f, power.y - size * 0.05f, size * 0.1f, size * 0.1f, glyph)
                renderer.drawRect(power.x + size * 0.15f, power.y - size * 0.05f, size * 0.1f, size * 0.1f, glyph)
            }
            PowerUpType.GRAVITY_WELL -> {
                // Spiral or vortex shape
                val centerX = power.x
                val centerY = power.y
                val radius = size * 0.2f
                for (i in 0..3) {
                    val angle = i * Math.PI.toFloat() / 2f
                    val x = centerX + kotlin.math.cos(angle) * radius
                    val y = centerY + kotlin.math.sin(angle) * radius
                    renderer.drawCircle(x, y, size * 0.08f, glyph)
                }
            }
            PowerUpType.BALL_SPLITTER -> {
                // Three balls in triangle
                renderer.drawCircle(power.x, power.y - size * 0.12f, size * 0.12f, glyph)
                renderer.drawCircle(power.x - size * 0.1f, power.y + size * 0.08f, size * 0.12f, glyph)
                renderer.drawCircle(power.x + size * 0.1f, power.y + size * 0.08f, size * 0.12f, glyph)
            }
            PowerUpType.FREEZE -> {
                // Snowflake pattern
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.02f, size * 0.4f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.15f, power.y - size * 0.15f, size * 0.3f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.15f, power.y + size * 0.11f, size * 0.3f, size * 0.04f, glyph)
            }
            PowerUpType.PIERCE -> {
                // Arrow through brick
                renderer.drawRect(power.x - size * 0.15f, power.y - size * 0.08f, size * 0.3f, size * 0.16f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyph)
                renderer.drawRect(power.x - size * 0.08f, power.y - size * 0.12f, size * 0.04f, size * 0.04f, glyph)
                renderer.drawRect(power.x + size * 0.04f, power.y - size * 0.12f, size * 0.04f, size * 0.04f, glyph)
            }
        }
    }

    private fun renderAimGuide(renderer: Renderer2D) {
        val ball = balls.firstOrNull() ?: return
        val dir = if (aimDirection == 0f) 1f else aimDirection
        val angle = aimAngle.coerceIn(aimMinAngle, aimMaxAngle)
        val dx = kotlin.math.cos(angle) * dir
        val dy = kotlin.math.sin(angle)
        val length = worldHeight * 0.24f
        val steps = 8
        for (i in 1..steps) {
            val t = i.toFloat() / steps.toFloat()
            val alpha = (0.45f * (1f - t)).coerceIn(0f, 0.45f)
            val color = floatArrayOf(theme.accent[0], theme.accent[1], theme.accent[2], alpha)
            val size = 0.35f + (1f - t) * 0.2f
            renderer.drawCircle(ball.x + dx * length * t, ball.y + dy * length * t, size, color)
        }
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
        val x = brick.x
        val y = brick.y
        val w = brick.width
        val h = brick.height
        val variant = ((brick.gridX * 3 + brick.gridY * 5) % 4 + 4) % 4
        val tint = when (variant) {
            0 -> 1.0f
            1 -> 0.88f
            2 -> 1.12f
            else -> 0.96f
        }

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
        val body = adjustColor(baseColor, 0.95f * tint, 1f)
        renderer.drawRect(x, y + bodyY, w, bodyHeight, body)

        val wingColor = adjustColor(baseColor, 1.15f * tint, 1f)
        val wingHeight = h * if (variant == 2) 0.32f else 0.28f
        renderer.drawRect(x + w * 0.06f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)
        renderer.drawRect(x + w * 0.74f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)

        val cockpit = adjustColor(baseColor, 1.35f, 1f)
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

        val engine = adjustColor(baseColor, 1.5f * tint, 0.9f)
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
    }

    private fun adjustColor(color: FloatArray, factor: Float, alpha: Float): FloatArray {
        return floatArrayOf(
            (color[0] * factor).coerceIn(0f, 1f),
            (color[1] * factor).coerceIn(0f, 1f),
            (color[2] * factor).coerceIn(0f, 1f),
            alpha
        )
    }

    fun getObjectCount(): Int = balls.size + bricks.size + powerups.size + beams.size + enemyShots.size + particles.size + waves.size

    private fun applyLayoutTuning(aspectRatio: Float, preserveRowBoost: Boolean) {
        val isWide = aspectRatio < 1.45f
        brickAreaTopRatio = if (isWide) 0.965f else 0.92f
        brickAreaBottomRatio = if (isWide) 0.48f else 0.52f
        brickSpacing = if (isWide) 0.36f else 0.42f
        if (!preserveRowBoost) {
            layoutRowBoost = if (isWide) 1 else 0
            layoutColBoost = 0
        }
        if (config.mode.invaders) {
            brickAreaTopRatio = if (isWide) 0.93f else 0.9f
            brickAreaBottomRatio = if (isWide) 0.68f else 0.66f
            brickSpacing = if (isWide) 0.7f else 0.62f
            invaderScale = if (isWide) 0.6f else 0.58f
        } else {
            invaderScale = 1f
        }
    }

    private fun updateAimFromInput(inputX: Float) {
        val delta = (inputX - paddle.x) / (paddle.width * 0.5f)
        aimNormalized = delta.coerceIn(-1.2f, 1.2f)
        if (abs(aimNormalized) > 0.08f) {
            aimDirection = if (aimNormalized >= 0f) 1f else -1f
            aimHasInput = true
        }
        val strength = abs(aimNormalized).coerceIn(0f, 1f)
        // Small aim offsets should fire more vertically; larger offsets shallow the angle.
        aimAngle = aimMaxAngle - (aimMaxAngle - aimMinAngle) * strength
    }

    private fun resolveAimDirection(): Float {
        return if (aimDirection == 0f) 1f else aimDirection
    }

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return
        val x = event.x / viewWidth * worldWidth
        val y = worldHeight - (event.y / viewHeight * worldHeight) // Invert Y: Android screen (0,0)=top-left, world (0,0)=bottom-left

        // Log touch input
        val actionString = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "down"
            MotionEvent.ACTION_MOVE -> "move"
            MotionEvent.ACTION_UP -> "up"
            else -> "other"
        }
        logger?.logTouchInput(actionString, x, y, event.pressure)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                paddle.targetX = x
                updateAimFromInput(x)
            }
            MotionEvent.ACTION_MOVE -> {
                paddle.targetX = x
                updateAimFromInput(x)
            }
            MotionEvent.ACTION_UP -> {
                if (state == GameState.READY) {
                    // Launch on tap/release for intuitive starts.
                    launchBall()
                    state = GameState.RUNNING
                    listener.onTip("Tap with two fingers to fire when laser is active")
                } else if (magnetActive && balls.any { it.stuckToPaddle }) {
                    releaseStuckBalls()
                }
            }
        }
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN &&
            activeEffects.containsKey(PowerUpType.LASER)
        ) {
            shootLaser()
        }
    }

    fun pause() {
        stateBeforePause = state
        state = GameState.PAUSED
    }

    fun resume() {
        if (state == GameState.PAUSED) state = stateBeforePause
    }

    fun nextLevel() {
        levelIndex += 1
        resetLevel(first = false)
    }

    private fun resetLevel(first: Boolean) {
        state = GameState.READY
        combo = 0
        lostLifeThisLevel = false
        guardrailActive = config.mode.godMode
        shieldCharges = 0
        fireballActive = false
        magnetActive = false
        gravityWellActive = false
        freezeActive = false
        pierceActive = false
        explosiveTipShown = false
        aimHasInput = false
        aimNormalized = 0f
        aimAngle = 0.72f
        aimDirection = if (random.nextBoolean()) 1f else -1f
        speedMultiplier = 1f
        activeEffects.clear()
        balls.clear()
        beams.clear()
        powerups.clear()
        enemyShots.clear()
        particles.clear()
        waves.clear()

        if (config.mode.godMode && !godModeTipShown) {
            listener.onTip("God mode: bottom shield is always active.")
            godModeTipShown = true
        }

        applyLayoutTuning(currentAspectRatio, preserveRowBoost = false)

        val difficulty = 1f + levelIndex * 0.08f
        val level = if (config.mode.invaders) {
            invaderDirection = if (random.nextBoolean()) 1f else -1f
            invaderSpeed = (22f + levelIndex * 1.2f).coerceAtMost(30f)
            invaderShotCooldown = (1.45f - levelIndex * 0.05f).coerceIn(0.6f, 1.45f)
            invaderShotTimer = invaderShotCooldown * (0.6f + random.nextFloat() * 0.8f)
            invaderShieldMax = (100f + levelIndex * 4f).coerceAtMost(130f)
            invaderShield = invaderShieldMax
            invaderShieldAlerted = false
            listener.onShieldUpdated(invaderShield.toInt(), invaderShieldMax.toInt())
            LevelFactory.buildInvaderLevel(levelIndex, difficulty)
        } else {
            invaderShield = 0f
            invaderShieldMax = 0f
            invaderShieldAlerted = false
            listener.onShieldUpdated(0, 0)
            buildLevel(levelIndex, difficulty, config.mode.endless)
        }
        currentLayout = level
        theme = level.theme
        buildBricks(level)

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
        listener.onLevelUpdated(levelIndex + 1)
        listener.onPowerupStatus("Powerups: none")
        lastPowerupStatus = "Powerups: none"
        listener.onTip(level.tip)
        logger?.logLevelStart(levelIndex, theme.name)
    }

    private fun buildBricks(layout: LevelFactory.LevelLayout) {
        bricks.clear()
        val rows = layout.rows + layoutRowBoost
        val cols = layout.cols + layoutColBoost
        val spacing = brickSpacing
        val areaTop = worldHeight * brickAreaTopRatio
        val areaBottom = worldHeight * brickAreaBottomRatio
        val areaHeight = areaTop - areaBottom
        val baseBrickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val baseBrickWidth = (worldWidth - spacing * (cols + 1)) / cols
        val sizeScale = if (config.mode.invaders) invaderScale else 1f
        val brickHeight = baseBrickHeight * sizeScale
        val brickWidth = baseBrickWidth * sizeScale
        val colOffset = layoutColBoost / 2
        layout.bricks.forEach { spec ->
            val cellX = spacing + (spec.col + colOffset) * (baseBrickWidth + spacing)
            val cellY = areaBottom + (rows - 1 - spec.row) * (baseBrickHeight + spacing * 0.5f)
            val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
            val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
            val brick = Brick(
                gridX = spec.col,
                gridY = spec.row,
                x = x,
                y = y,
                width = brickWidth,
                height = brickHeight,
                hitPoints = spec.hitPoints,
                maxHitPoints = spec.hitPoints,
                type = spec.type
            )

            // Set up special properties for dynamic bricks
            when (spec.type) {
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
                    val cellY = areaBottom + (rows - 1 - rowIndex) * (baseBrickHeight + spacing * 0.5f)
                    val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
                    val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
                    bricks.add(
                        Brick(
                            gridX = col,
                            gridY = rowIndex,
                            x = x,
                            y = y,
                            width = brickWidth,
                            height = brickHeight,
                            hitPoints = hp,
                            maxHitPoints = hp,
                            type = type
                        )
                    )
                }
            }
        }
    }

    private fun relayoutBricks() {
        val layout = currentLayout ?: return
        if (bricks.isEmpty()) return
        val rows = layout.rows + layoutRowBoost
        val cols = layout.cols + layoutColBoost
        val spacing = brickSpacing
        val areaTop = worldHeight * brickAreaTopRatio
        val areaBottom = worldHeight * brickAreaBottomRatio
        val areaHeight = areaTop - areaBottom
        val baseBrickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val baseBrickWidth = (worldWidth - spacing * (cols + 1)) / cols
        val sizeScale = if (config.mode.invaders) invaderScale else 1f
        val brickHeight = baseBrickHeight * sizeScale
        val brickWidth = baseBrickWidth * sizeScale
        val colOffset = layoutColBoost / 2
        bricks.forEach { brick ->
            if (brick.gridX < 0 || brick.gridY < 0) return@forEach
            val cellX = spacing + (brick.gridX + colOffset) * (baseBrickWidth + spacing)
            val cellY = areaBottom + (rows - 1 - brick.gridY) * (baseBrickHeight + spacing * 0.5f)
            val x = cellX + (baseBrickWidth - brickWidth) * 0.5f
            val y = cellY + (baseBrickHeight - brickHeight) * 0.5f
            brick.x = x
            brick.y = y
            brick.width = brickWidth
            brick.height = brickHeight
        }
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

    private fun updatePaddle(dt: Float) {
        val previousX = paddle.x
        val target = paddle.targetX
        val speed = 60f + settings.sensitivity * 140f
        val delta = target - paddle.x
        val maxMove = speed * dt
        if (abs(delta) > 0.05f) {
            paddle.x += delta.coerceIn(-maxMove, maxMove)
        }
        paddle.x = paddle.x.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
        paddleVelocity = if (dt > 0f) (paddle.x - previousX) / dt else 0f
    }

    private fun updateBricks(dt: Float) {
        if (config.mode.invaders) {
            updateInvaderFormation(dt)
        }
        bricks.forEach { brick ->
            when (brick.type) {
                BrickType.MOVING -> {
                    // Initialize velocity if not set
                    if (brick.vx == 0f) {
                        brick.vx = (kotlin.random.Random(brick.gridX * 31 + brick.gridY * 17).nextFloat() - 0.5f) * 20f
                    }
                    // Move horizontally and bounce off edges
                    brick.x += brick.vx * dt
                    if (brick.x <= 0.6f || brick.x + brick.width >= worldWidth - 0.6f) {
                        brick.vx = -brick.vx
                        brick.x = brick.x.coerceIn(0.6f, worldWidth - brick.width - 0.6f)
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
        }
    }

    private fun updateInvaderFormation(dt: Float) {
        val invaders = bricks.filter { it.alive && it.type == BrickType.INVADER }
        if (invaders.isEmpty()) return
        val leftBound = 1.5f
        val rightBound = worldWidth - 1.5f
        val minX = invaders.minOf { it.x }
        val maxX = invaders.maxOf { it.x + it.width }
        val span = maxX - minX
        val available = rightBound - leftBound
        if (span >= available) {
            val clampShift = leftBound - minX
            invaders.forEach { it.x += clampShift }
            return
        }

        var dx = invaderSpeed * invaderDirection * dt
        if (minX + dx < leftBound) {
            dx = leftBound - minX
            invaderDirection = 1f
            audio.play(GameSound.BRICK_MOVING, 0.25f)
        } else if (maxX + dx > rightBound) {
            dx = rightBound - maxX
            invaderDirection = -1f
            audio.play(GameSound.BRICK_MOVING, 0.25f)
        }
        invaders.forEach { it.x += dx }
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
        balls.firstOrNull()?.let { ball ->
            if (ball.vx == 0f && ball.vy == 0f) {
                launchBallWithAim(ball)
                aimHasInput = false
                aimNormalized = 0f
                // Start background music when gameplay begins
                audio.startMusic()
            }
        }
    }

    private fun launchBallWithAim(ball: Ball, angleOffset: Float = 0f) {
        val dir = resolveAimDirection()
        val levelBoost = (1f + levelIndex * 0.015f).coerceAtMost(1.35f)
        val speed = config.mode.launchSpeed * levelBoost
        val jitter = (random.nextFloat() - 0.5f) * 0.04f
        val angle = (aimAngle + angleOffset + jitter).coerceIn(aimMinAngle, aimMaxAngle)
        ball.vx = speed * kotlin.math.cos(angle) * dir
        ball.vy = speed * kotlin.math.sin(angle)
        ball.stuckToPaddle = false
    }

    private fun releaseStuckBalls() {
        val stuck = balls.filter { it.stuckToPaddle }
        if (stuck.isEmpty()) return
        val spread = 0.08f
        val center = (stuck.size - 1) / 2f
        stuck.forEachIndexed { index, ball ->
            val offset = (index - center) * spread
            launchBallWithAim(ball, offset)
        }
        aimHasInput = false
        aimNormalized = 0f
        audio.startMusic()
    }

    private fun spawnBall() {
        val ball = Ball(paddle.x, paddle.y + 5f, 1.0f, 0f, 0f)
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
                } else if (ball.x + ball.radius > worldWidth) {
                    ball.x = worldWidth - ball.radius
                    ball.vx = -abs(ball.vx)
                    audio.play(GameSound.BOUNCE, 0.6f)
                }

                if (ball.y + ball.radius > worldHeight) {
                    ball.y = worldHeight - ball.radius
                    ball.vy = -abs(ball.vy)
                    audio.play(GameSound.BOUNCE, 0.6f)
                }

                if (ball.y - ball.radius < 0f) {
                    if (config.mode.godMode) {
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

                handlePaddleCollision(ball)
                handleBrickCollision(ball)
            }

            if (!removed) {
                clampBallSpeed(ball)
            }
        }
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
        for (brick in bricks) {
            if (!brick.alive) continue
            if (!circleIntersectsRect(ball, brick)) continue

            val destroyed = brick.applyHit(fireballActive || pierceActive)
            if (!fireballActive && !pierceActive) {
                bounceBallFromBrick(ball, brick)
            }

            if (destroyed) {
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.BRICKS_DESTROYED) }
                // Add particle burst for brick destruction
                val available = maxParticles - particles.size
                val count = min(5, max(0, available))
                repeat(count) {
                    val angle = random.nextFloat() * Math.PI.toFloat() * 2f
                    val speed = random.nextFloat() * 12f + 4f
                    particles.add(
                        Particle(
                            x = brick.centerX,
                            y = brick.centerY,
                            vx = kotlin.math.cos(angle) * speed,
                            vy = kotlin.math.sin(angle) * speed,
                            radius = 0.4f + random.nextFloat() * 0.2f,
                            life = 0.4f + random.nextFloat() * 0.2f,
                            color = brick.currentColor(theme)
                        )
                    )
                }

                if (brick.type == BrickType.BOSS) {
                    screenFlash = 0.4f
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
                    powerups.add(PowerUp(brick.centerX, brick.centerY, randomPowerupType(), 18f))
                    listener.onTip("Boss down! Powerup dropped.")
                }

                // Combo system: consecutive breaks within 2 seconds get multipliers
                comboTimer = 2f  // Reset combo timer
                combo += 1

                // Update daily challenges
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.COMBO_MULTIPLIER, combo) }

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

                val baseScore = brick.scoreValue * multiplier
                score += baseScore.toInt()

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
                    BrickType.INVADER -> GameSound.BRICK_MOVING
                }
                audio.play(brickSound, 0.7f)
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
                score += brick.scoreValue
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.BRICKS_DESTROYED) }

                // Visual effects
                renderer?.triggerScreenShake(2f, 0.15f)
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
                    BrickType.INVADER -> GameSound.BRICK_MOVING
                }
                audio.play(brickSound, 0.4f) // Softer for beam hits
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
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.POWERUPS_COLLECTED) }
                audio.play(GameSound.POWERUP, 0.8f)
                spawnPowerupBurst(power)
                iterator.remove()
            }
        }
    }

    private fun updateInvaderShots(dt: Float) {
        if (!config.mode.invaders) return
        invaderShotTimer -= dt
        if (invaderShotTimer <= 0f) {
            val maxShots = 6 + (levelIndex / 2).coerceAtMost(6)
            if (enemyShots.size < maxShots) {
                spawnInvaderShot()
            }
            invaderShotTimer = invaderShotCooldown * (0.7f + random.nextFloat() * 0.7f)
        }

        val iterator = enemyShots.iterator()
        while (iterator.hasNext()) {
            val shot = iterator.next()
            shot.x += shot.vx * dt
            shot.y += shot.vy * dt
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

    private fun spawnInvaderShot() {
        val invaders = bricks.filter { it.alive && it.type == BrickType.INVADER }
        if (invaders.isEmpty()) return
        val origin = invaders[random.nextInt(invaders.size)]
        val baseSpeed = (28f + levelIndex * 1.2f).coerceAtMost(42f)
        val spread = 6f
        val vx = (random.nextFloat() - 0.5f) * spread
        val shotColor = adjustColor(origin.currentColor(theme), 1.1f, 1f)
        val shot = EnemyShot(
            x = origin.centerX,
            y = origin.y - origin.height * 0.2f,
            radius = 0.7f,
            vx = vx,
            vy = -baseSpeed,
            color = shotColor
        )
        enemyShots.add(shot)
        audio.play(GameSound.LASER, 0.45f)
    }

    private fun handleInvaderShotHit(shot: EnemyShot) {
        spawnImpactSparks(shot.x, shot.y, shot.color, 6, 12f)
        if (invaderShield > 0f) {
            val damage = (12f + levelIndex * 1.2f).coerceAtMost(22f)
            invaderShield = max(0f, invaderShield - damage)
            listener.onShieldUpdated(invaderShield.toInt(), invaderShieldMax.toInt())
            audio.play(GameSound.BOUNCE, 0.65f)
            audio.haptic(GameHaptic.LIGHT)
            if (invaderShield <= 0f && !invaderShieldAlerted) {
                invaderShieldAlerted = true
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
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - dt
            if (remaining <= 0f) {
                when (entry.key) {
                    PowerUpType.WIDE_PADDLE -> paddle.width = basePaddleWidth
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
                    else -> Unit
                }
                iterator.remove()
            } else {
                entry.setValue(remaining)
            }
        }
        speedMultiplier = when {
            activeEffects.containsKey(PowerUpType.FREEZE) -> 0.1f
            activeEffects.containsKey(PowerUpType.SLOW) -> 0.8f
            else -> 1f
        }
        if (ballStyleDirty) {
            syncBallStyles()
        }
        // Update screen flash
        screenFlash = max(0f, screenFlash - dt * 3f)
        levelClearFlash = max(0f, levelClearFlash - dt * 1.5f)
        updatePowerupStatus()
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

    private fun speedBoostSlope(): Float = when (config.mode) {
        GameMode.CLASSIC -> 0.015f
        GameMode.TIMED -> 0.020f
        GameMode.ENDLESS -> 0.017f
        GameMode.GOD -> 0.010f
        GameMode.RUSH -> 0.023f
        GameMode.INVADERS -> 0.016f
    }

    private fun speedBoostCap(): Float = when (config.mode) {
        GameMode.CLASSIC -> 1.35f
        GameMode.TIMED -> 1.45f
        GameMode.ENDLESS -> 1.4f
        GameMode.GOD -> 1.25f
        GameMode.RUSH -> 1.5f
        GameMode.INVADERS -> 1.4f
    }

    private fun minSpeedFactor(): Float = when (config.mode) {
        GameMode.CLASSIC -> 0.6f
        GameMode.TIMED -> 0.7f
        GameMode.ENDLESS -> 0.65f
        GameMode.GOD -> 0.5f
        GameMode.RUSH -> 0.72f
        GameMode.INVADERS -> 0.6f
    }

    private fun maxSpeedFactor(): Float = when (config.mode) {
        GameMode.CLASSIC -> 1.6f
        GameMode.TIMED -> 1.75f
        GameMode.ENDLESS -> 1.7f
        GameMode.GOD -> 1.45f
        GameMode.RUSH -> 1.85f
        GameMode.INVADERS -> 1.6f
    }

    private fun difficultyForMode(): Float {
        val base = when (config.mode) {
            GameMode.CLASSIC -> 1.0f
            GameMode.TIMED -> 1.05f
            GameMode.ENDLESS -> 1.0f
            GameMode.GOD -> 0.9f
            GameMode.RUSH -> 1.1f
            GameMode.INVADERS -> 1.05f
        }
        val slope = when (config.mode) {
            GameMode.CLASSIC -> 0.075f
            GameMode.TIMED -> 0.095f
            GameMode.ENDLESS -> 0.085f
            GameMode.GOD -> 0.05f
            GameMode.RUSH -> 0.11f
            GameMode.INVADERS -> 0.08f
        }
        return (base + levelIndex * slope).coerceAtMost(3.0f)
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
                balls.addAll(newBalls.take(2))
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.MULTI_BALL_ACTIVE) }
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
                if (!config.mode.godMode) {
                    lives += 1
                    listener.onLivesUpdated(lives)
                }
            }
            PowerUpType.SHIELD -> {
                shieldCharges = min(2, shieldCharges + 1)
                activeEffects[type] = 12f
            }
            PowerUpType.WIDE_PADDLE -> {
                paddle.width = basePaddleWidth * 1.4f
                activeEffects[type] = 12f
            }
            PowerUpType.SLOW -> {
                speedMultiplier = 0.8f
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
                balls.addAll(newBalls.take(4)) // Limit to 4 new balls max
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.MULTI_BALL_ACTIVE) }
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
        val segments = mutableListOf<String>()
        if (activeEffects.isEmpty()) {
            segments.add("Powerups: none")
        } else {
            val list = activeEffects.entries
                .sortedBy { it.key.ordinal }
                .joinToString(" • ") { (type, time) ->
                if (type == PowerUpType.SHIELD) {
                    "${type.displayName} x$shieldCharges ${time.toInt()}s"
                } else {
                    "${type.displayName} ${time.toInt()}s"
                }
            }
            segments.add("Powerups: $list")
        }
        if (combo >= 2) {
            segments.add("Combo x$combo")
        }
        val status = segments.joinToString(" • ")
        if (status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        val snapshot = activeEffects.entries
            .sortedBy { it.key.ordinal }
            .map { (type, time) ->
                PowerupStatus(
                    type = type,
                    remainingSeconds = time.toInt(),
                    charges = if (type == PowerUpType.SHIELD) shieldCharges else 0
                )
            }
        listener.onPowerupsUpdated(snapshot, combo)
    }

    private fun reportScore() {
        updateScoreChallenges()
        listener.onScoreUpdated(score)
    }

    private fun updateScoreChallenges() {
        dailyChallenges?.forEach { challenge ->
            if (challenge.type == ChallengeType.SCORE_ACHIEVED && !challenge.completed) {
                if (score > challenge.progress) {
                    challenge.progress = score
                }
                if (challenge.progress >= challenge.targetValue) {
                    challenge.completed = true
                    challenge.rewardGranted = true
                }
            }
        }
    }

    private fun checkLevelCompletion() {
        val remaining = bricks.count { it.alive && it.type != BrickType.UNBREAKABLE }
        if (remaining == 0) {
            val levelDuration = elapsedSeconds - levelStartTime
            dailyChallenges?.let { challenges ->
                if (!lostLifeThisLevel) {
                    DailyChallengeManager.updateChallengeProgress(challenges, ChallengeType.PERFECT_LEVEL)
                }
                challenges.forEach { challenge ->
                    if (challenge.type == ChallengeType.TIME_UNDER_LIMIT && !challenge.completed) {
                        if (levelDuration <= challenge.targetValue) {
                            DailyChallengeManager.completeChallenge(challenge)
                        }
                    }
                }
            }
            logger?.logLevelComplete(levelIndex, score, elapsedSeconds, remaining)
            levelClearFlash = 1.0f
            renderer?.triggerLevelClearFlash()
            val summary = GameSummary(score, levelIndex + 1, elapsedSeconds.toInt())
            listener.onLevelComplete(summary)
            state = GameState.PAUSED
        }
    }

    private fun loseLife() {
        // Reset combo on life loss
        combo = 0
        comboTimer = 0f
        lostLifeThisLevel = true

        if (config.mode.godMode) {
            aimDirection = if (random.nextBoolean()) 1f else -1f
            spawnBall()
            state = GameState.READY
            return
        }
        lives -= 1
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
            aimDirection = if (random.nextBoolean()) 1f else -1f
            spawnBall()
            state = GameState.READY
        }
    }

    private fun triggerGameOver() {
        val summary = GameSummary(score, levelIndex + 1, elapsedSeconds.toInt())
        audio.play(GameSound.GAME_OVER, 1f)
        audio.haptic(GameHaptic.HEAVY)
        audio.stopMusic() // Stop background music on game over
        listener.onGameOver(summary)
        state = GameState.GAME_OVER
    }

    private fun shootLaser() {
        if (laserCooldown > 0f) return
        laserCooldown = 0.4f
        val beamOffset = paddle.width / 3f
        beams.add(Beam(paddle.x - beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        beams.add(Beam(paddle.x + beamOffset, paddle.y + paddle.height / 2f, 0.5f, 6f, 90f, PowerUpType.LASER.color))
        dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.LASER_FIRED) }
        audio.play(GameSound.LASER, 0.7f)
    }

    private fun maybeSpawnPowerup(brick: Brick) {
        val dropChance = when (brick.type) {
            BrickType.EXPLOSIVE -> 0.25f
            BrickType.REINFORCED, BrickType.ARMORED -> 0.15f
            BrickType.BOSS, BrickType.PHASE -> 0.20f
            BrickType.SPAWNING -> 0.12f
            BrickType.MOVING -> 0.10f
            else -> 0.08f
        }
        if (random.nextFloat() < dropChance) {
            val type = randomPowerupType()
            powerups.add(PowerUp(brick.centerX, brick.centerY, type, 18f))
        }
    }

    private fun triggerExplosion(brick: Brick) {
        audio.play(GameSound.EXPLOSION, 0.8f)
        audio.haptic(GameHaptic.HEAVY)
        screenFlash = 0.3f
        renderer?.triggerScreenShake(2.8f, 0.18f)
        val radius = 1
        bricks.filter { it.alive && it.gridX >= 0 && it.gridY >= 0 && it.isNeighbor(brick, radius) }.forEach { neighbor ->
            if (neighbor == brick) return@forEach
            val destroyed = neighbor.applyHit(true)
            if (destroyed) {
                score += neighbor.scoreValue
                dailyChallenges?.let { DailyChallengeManager.updateChallengeProgress(it, ChallengeType.BRICKS_DESTROYED) }
                maybeSpawnPowerup(neighbor)
            }
        }
        reportScore()

        // Add expanding shockwave
        if (waves.size < maxWaves) {
            waves.add(
                ExplosionWave(
                    x = brick.centerX,
                    y = brick.centerY,
                    radius = 1f,
                    color = brick.currentColor(theme).copyOf(),
                    life = 1.2f,
                    maxLife = 1.2f,
                    speed = 20f
                )
            )
        }

        val available = maxParticles - particles.size
        val count = min(16, max(0, available))
        repeat(count) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = random.nextFloat() * 22f + 6f
            particles.add(
                Particle(
                    x = brick.centerX,
                    y = brick.centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    radius = 0.5f + random.nextFloat() * 0.3f,
                    life = 0.7f + random.nextFloat() * 0.3f,
                    color = brick.currentColor(theme)
                )
            )
        }
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

    private fun randomPowerupType(): PowerUpType {
        val weights = mutableMapOf(
            PowerUpType.MULTI_BALL to 1.25f,
            PowerUpType.LASER to 1.05f,
            PowerUpType.GUARDRAIL to 0.95f,
            PowerUpType.SHIELD to 0.9f,
            PowerUpType.WIDE_PADDLE to 0.95f,
            PowerUpType.SLOW to 0.9f,
            PowerUpType.MAGNET to 0.85f,
            PowerUpType.LIFE to 0.55f,
            PowerUpType.FIREBALL to 0.7f,
            PowerUpType.GRAVITY_WELL to 0.7f,
            PowerUpType.BALL_SPLITTER to 0.7f,
            PowerUpType.FREEZE to 0.6f,
            PowerUpType.PIERCE to 0.7f
        )
        when (config.mode) {
            GameMode.TIMED -> {
                weights[PowerUpType.MULTI_BALL] = (weights[PowerUpType.MULTI_BALL] ?: 0f) + 0.25f
                weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.2f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.15f
            }
            GameMode.RUSH -> {
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.35f
                weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.25f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.2f
                weights[PowerUpType.LIFE] = (weights[PowerUpType.LIFE] ?: 0f) + 0.1f
            }
            GameMode.ENDLESS -> {
                weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.2f
                weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.2f
                weights[PowerUpType.GRAVITY_WELL] = (weights[PowerUpType.GRAVITY_WELL] ?: 0f) + 0.15f
                weights[PowerUpType.BALL_SPLITTER] = (weights[PowerUpType.BALL_SPLITTER] ?: 0f) + 0.1f
            }
            GameMode.GOD -> {
                weights[PowerUpType.LIFE] = 0.15f
            }
            GameMode.INVADERS -> {
                weights[PowerUpType.SHIELD] = (weights[PowerUpType.SHIELD] ?: 0f) + 0.3f
                weights[PowerUpType.GUARDRAIL] = (weights[PowerUpType.GUARDRAIL] ?: 0f) + 0.2f
                weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.1f
            }
            else -> Unit
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
            bricks.add(childBrick)
        }
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
    var stickOffset: Float = 0f
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
    var hitPoints: Int,
    val maxHitPoints: Int,
    val type: BrickType,
    var alive: Boolean = true
) {
    var hitFlash = 0f
    // Dynamic brick properties
    var vx: Float = 0f  // Horizontal velocity for moving bricks
    var vy: Float = 0f  // Vertical velocity
    var phase: Int = 0  // Current phase for phase bricks
    var maxPhase: Int = 1  // Total phases for phase bricks
    var spawnCount: Int = 0  // Number of spawns left for spawning bricks
    var lastHitTime: Float = 0f  // Timestamp of last hit for special effects
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
        BrickType.INVADER -> 90
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
        val base = theme.brickPalette[type] ?: theme.accent
        val ratio = if (type == BrickType.UNBREAKABLE) 1f else (hitPoints.toFloat() / maxHitPoints.toFloat()).coerceIn(0.4f, 1f)

        // Significant variance for huge color variety between bricks
        val gridSeed = (gridX * 7 + gridY * 11 + type.ordinal * 17) % 100  // Unique seed per brick position and type
        val random = java.util.Random(gridSeed.toLong())
        var positionVarianceR = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance
        var positionVarianceG = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance
        var positionVarianceB = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance
        if (type == BrickType.INVADER) {
            val varianceBoost = 1.6f
            positionVarianceR *= varianceBoost
            positionVarianceG *= varianceBoost
            positionVarianceB *= varianceBoost
        }

        // Additional type-based color shifts for even more variety
        val typeShift = when (type) {
            BrickType.NORMAL -> floatArrayOf(0f, 0f, 0f)
            BrickType.REINFORCED -> floatArrayOf(0.1f, -0.05f, 0.05f)
            BrickType.ARMORED -> floatArrayOf(-0.05f, 0.1f, -0.05f)
            BrickType.EXPLOSIVE -> floatArrayOf(0.15f, -0.1f, -0.1f)
            BrickType.UNBREAKABLE -> floatArrayOf(-0.1f, -0.1f, 0.1f)
            BrickType.MOVING -> floatArrayOf(-0.05f, 0.15f, 0.05f)
            BrickType.SPAWNING -> floatArrayOf(0.05f, 0f, 0.15f)
            BrickType.PHASE -> floatArrayOf(0.2f, 0.1f, -0.1f)
            BrickType.BOSS -> floatArrayOf(0.3f, -0.2f, -0.2f)
            BrickType.INVADER -> {
                val variant = ((gridX + gridY) % 4 + 4) % 4
                when (variant) {
                    0 -> floatArrayOf(0.15f, 0.1f, 0.2f)
                    1 -> floatArrayOf(-0.05f, 0.18f, 0.25f)
                    2 -> floatArrayOf(0.2f, -0.05f, 0.12f)
                    else -> floatArrayOf(0.05f, 0.22f, -0.05f)
                }
            }
        }

        // Strong theme-specific color adjustments for maximum variety
        val themeHueShift = when (theme.name) {
            "Neon" -> floatArrayOf(0.1f, 0.2f, 0.3f)      // Boost cyan/blue
            "Sunset" -> floatArrayOf(0.3f, -0.1f, -0.2f)   // Boost red, reduce blue
            "Cobalt" -> floatArrayOf(-0.1f, 0.1f, 0.4f)    // Boost blue, reduce red
            "Aurora" -> floatArrayOf(0f, 0.3f, 0.1f)       // Boost green
            "Forest" -> floatArrayOf(-0.2f, 0.2f, -0.1f)   // Boost green, reduce red
            "Lava" -> floatArrayOf(0.4f, -0.2f, -0.3f)     // Boost red, reduce blue/green
            else -> floatArrayOf(0f, 0f, 0f)
        }

        val finalColor = floatArrayOf(
            (base[0] * ratio + positionVarianceR + themeHueShift[0] + typeShift[0]).coerceIn(0.05f, 0.98f),
            (base[1] * ratio + positionVarianceG + themeHueShift[1] + typeShift[1]).coerceIn(0.05f, 0.98f),
            (base[2] * ratio + positionVarianceB + themeHueShift[2] + typeShift[2]).coerceIn(0.05f, 0.98f),
            1f
        )

        if (hitFlash <= 0f) return finalColor
        return floatArrayOf(
            min(1f, finalColor[0] + 0.4f),
            min(1f, finalColor[1] + 0.4f),
            min(1f, finalColor[2] + 0.4f),
            1f
        )
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
    SLOW("Slow", floatArrayOf(0.63f, 0.76f, 1f, 1f)),
    FIREBALL("Fireball", floatArrayOf(1f, 0.36f, 0.27f, 1f)),
    MAGNET("Magnet", floatArrayOf(0.8f, 0.4f, 1f, 1f)),
    GRAVITY_WELL("Gravity Well", floatArrayOf(0.4f, 0.6f, 1f, 1f)),
    BALL_SPLITTER("Ball Splitter", floatArrayOf(1f, 0.8f, 0.2f, 1f)),
    FREEZE("Freeze", floatArrayOf(0.3f, 0.8f, 1f, 1f)),
    PIERCE("Pierce", floatArrayOf(0.9f, 0.5f, 0.1f, 1f))
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
    val color: FloatArray
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
    val speed: Float
)
