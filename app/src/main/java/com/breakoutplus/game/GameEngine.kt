package com.breakoutplus.game

import android.view.MotionEvent
import com.breakoutplus.game.LevelFactory.buildLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
    private val balls = mutableListOf<Ball>()
    private val bricks = mutableListOf<Brick>()
    private val powerups = mutableListOf<PowerUp>()
    private val beams = mutableListOf<Beam>()
    private val particles = mutableListOf<Particle>()
    private val waves = mutableListOf<ExplosionWave>()
    private val activeEffects = mutableMapOf<PowerUpType, Float>()

    private var worldWidth = 100f
    private var worldHeight = 160f

    private var paddle = Paddle(x = worldWidth / 2f, y = 8f, width = 18f, height = 2.6f)
    private var basePaddleWidth = paddle.width

    private var state = GameState.READY
    private var stateBeforePause = state
    private var score = 0
    private var levelIndex = 0
    private var lives = config.mode.baseLives
    private var timeRemaining = config.mode.timeLimitSeconds.toFloat()
    private var lastReportedSecond = -1
    private var elapsedSeconds = 0f
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
    private var lastPowerupStatus = ""

    private var theme: LevelTheme = LevelThemes.DEFAULT
    private var currentLayout: LevelFactory.LevelLayout? = null
    private var touchDownTime = 0L
    private var paddlePositioned = false
    private var screenFlash = 0f

    init {
        logger?.logSessionStart(config.mode)
        listener.onModeUpdated(config.mode)
        resetLevel(first = true)
        listener.onLivesUpdated(lives)
        listener.onScoreUpdated(score)
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
        relayoutBricks()
    }

    fun update(delta: Float) {
        val dt = delta * speedMultiplier
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) {
            return
        }
        updateTimers(delta)
        updatePaddle(delta)
        updateBricks(dt)
        updateEffects(delta)

        if (state == GameState.READY) {
            attachBallToPaddle()
        }

        if (state == GameState.RUNNING) {
            updateBalls(dt)
            updateBeams(dt)
            updatePowerups(dt)
            updateParticles(dt)
            updateWaves(dt)
            checkLevelCompletion()
        }
    }

    fun render(renderer: Renderer2D) {
        renderer.setWorldSize(worldWidth, worldHeight)
        // Enhanced background with subtle gradient and flash effect
        val bgTop = if (screenFlash > 0f) {
            adjustColor(theme.background, 1.1f + screenFlash, 1f)
        } else {
            adjustColor(theme.background, 1.1f, 1f)
        }
        val bgBottom = if (screenFlash > 0f) {
            adjustColor(theme.background, 0.9f + screenFlash, 1f)
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
        }

        if (guardrailActive) {
            val pulse = (kotlin.math.sin(time * 3f) * 0.5f + 0.5f)
            val guardColor = floatArrayOf(theme.accent[0], theme.accent[1], theme.accent[2], 0.5f + pulse * 0.4f)
            renderer.drawRect(0f, 2f, worldWidth, 0.6f, guardColor)
        }

        bricks.filter { it.alive }.forEach { brick ->
            val color = brick.currentColor(theme)

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

        waves.forEach { wave ->
            renderer.drawCircle(wave.x, wave.y, wave.radius, wave.color)
        }
        particles.forEach { particle ->
            renderer.drawCircle(particle.x, particle.y, particle.radius, particle.color)
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

    private fun drawStripe(renderer: Renderer2D, brick: Brick, color: FloatArray, count: Int) {
        if (count <= 0) return
        val stripeHeight = brick.height * 0.12f
        val gap = brick.height * 0.14f
        repeat(count) { index ->
            val y = brick.y + brick.height * 0.2f + index * gap
            renderer.drawRect(brick.x + brick.width * 0.08f, y, brick.width * 0.84f, stripeHeight, color)
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

    fun getObjectCount(): Int = balls.size + bricks.size + powerups.size + beams.size + particles.size + waves.size

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return
        val x = event.x / viewWidth * worldWidth
        val y = event.y / viewHeight * worldHeight

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
                touchDownTime = System.currentTimeMillis()
                paddlePositioned = false
                paddle.targetX = x
            }
            MotionEvent.ACTION_MOVE -> {
                paddle.targetX = x
                if (state == GameState.READY) {
                    paddlePositioned = true
                }
            }
            MotionEvent.ACTION_UP -> {
                val touchDuration = System.currentTimeMillis() - touchDownTime
                if (state == GameState.READY && (paddlePositioned || touchDuration < 300)) {
                    // Launch ball on release if paddle was moved or quick tap
                    launchBall()
                    state = GameState.RUNNING
                    listener.onTip("Tap with two fingers to fire when laser is active")
                }
            }
        }
        if (event.pointerCount > 1 && activeEffects.containsKey(PowerUpType.LASER)) {
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
        guardrailActive = false
        shieldCharges = 0
        fireballActive = false
        magnetActive = false
        gravityWellActive = false
        freezeActive = false
        pierceActive = false
        speedMultiplier = 1f
        activeEffects.clear()
        touchDownTime = 0L
        paddlePositioned = false
        balls.clear()
        beams.clear()
        powerups.clear()
        particles.clear()
        waves.clear()

        val difficulty = 1f + levelIndex * 0.08f
        val level = buildLevel(levelIndex, difficulty, config.mode.endless)
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

        spawnBall()
        listener.onLevelUpdated(levelIndex + 1)
        listener.onPowerupStatus("Powerups: none")
        lastPowerupStatus = "Powerups: none"
        listener.onTip(level.tip)
        logger?.logLevelStart(levelIndex, theme.name)
    }

    private fun buildBricks(layout: LevelFactory.LevelLayout) {
        bricks.clear()
        val rows = layout.rows
        val cols = layout.cols
        val spacing = 0.45f
        val areaTop = worldHeight * 0.88f
        val areaBottom = worldHeight * 0.55f
        val areaHeight = areaTop - areaBottom
        val brickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val brickWidth = (worldWidth - spacing * (cols + 1)) / cols
        layout.bricks.forEach { spec ->
            val x = spacing + spec.col * (brickWidth + spacing)
            val y = areaBottom + (rows - 1 - spec.row) * (brickHeight + spacing * 0.5f)
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
    }

    private fun relayoutBricks() {
        val layout = currentLayout ?: return
        if (bricks.isEmpty()) return
        val rows = layout.rows
        val cols = layout.cols
        val spacing = 0.45f
        val areaTop = worldHeight * 0.88f
        val areaBottom = worldHeight * 0.55f
        val areaHeight = areaTop - areaBottom
        val brickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val brickWidth = (worldWidth - spacing * (cols + 1)) / cols
        bricks.forEach { brick ->
            if (brick.gridX < 0 || brick.gridY < 0) return@forEach
            val x = spacing + brick.gridX * (brickWidth + spacing)
            val y = areaBottom + (rows - 1 - brick.gridY) * (brickHeight + spacing * 0.5f)
            brick.x = x
            brick.y = y
            brick.width = brickWidth
            brick.height = brickHeight
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

    private fun updatePaddle(dt: Float) {
        val target = paddle.targetX
        val speed = 60f + config.settings.sensitivity * 140f
        val delta = target - paddle.x
        val maxMove = speed * dt
        if (abs(delta) > 0.05f) {
            paddle.x += delta.coerceIn(-maxMove, maxMove)
        }
        paddle.x = paddle.x.coerceIn(paddle.width / 2f, worldWidth - paddle.width / 2f)
    }

    private fun updateBricks(dt: Float) {
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
                else -> {
                    // Static bricks, no movement
                }
            }
        }
    }

    private fun attachBallToPaddle() {
        balls.firstOrNull()?.let { ball ->
            ball.x = paddle.x
            ball.y = paddle.y + paddle.height / 2f + ball.radius + 0.5f
            ball.vx = 0f
            ball.vy = 0f
        }
    }

    private fun launchBall() {
        balls.firstOrNull()?.let { ball ->
            if (ball.vx == 0f && ball.vy == 0f) {
                val angle = random.nextFloat() * 0.6f + 0.3f
                val dir = if (random.nextBoolean()) 1f else -1f
                val levelBoost = (1f + levelIndex * 0.015f).coerceAtMost(1.35f)
                val speed = config.mode.launchSpeed * levelBoost
                ball.vx = speed * kotlin.math.cos(angle) * dir
                ball.vy = speed * kotlin.math.sin(angle)
            }
        }
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
            if (gravityWellActive) {
                applyGravityWell(ball, dt)
            }
            ball.x += ball.vx * dt
            ball.y += ball.vy * dt

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
                if (guardrailActive) {
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
                    continue
                }
            }

            handlePaddleCollision(ball)
            handleBrickCollision(ball)
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
        if (ball.y - ball.radius > paddle.y + paddle.height / 2f) return
        if (ball.y + ball.radius < paddle.y - paddle.height / 2f) return
        if (ball.x + ball.radius < paddle.x - paddle.width / 2f) return
        if (ball.x - ball.radius > paddle.x + paddle.width / 2f) return

        val hitPos = (ball.x - paddle.x) / (paddle.width / 2f)
        val angle = hitPos * 1.1f
        val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy).coerceAtLeast(24f)
        ball.vx = speed * angle
        ball.vy = abs(speed * (1.2f - abs(angle)))
        ball.y = paddle.y + paddle.height / 2f + ball.radius
        audio.play(GameSound.BOUNCE, 0.8f)
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

            listener.onScoreUpdated(score)
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
                }
                audio.play(brickSound, 0.4f) // Softer for beam hits
                maybeSpawnPowerup(brick)
                if (brick.type == BrickType.EXPLOSIVE) {
                    triggerExplosion(brick)
                }
            }
            listener.onScoreUpdated(score)
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
                iterator.remove()
            }
        }
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
                    PowerUpType.GUARDRAIL -> guardrailActive = false
                    PowerUpType.LASER -> Unit
                    PowerUpType.SHIELD -> shieldCharges = 0
                    PowerUpType.FIREBALL -> {
                        fireballActive = false
                        ballStyleDirty = true
                    }
                    PowerUpType.MAGNET -> magnetActive = false
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
        updatePowerupStatus()
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
            }
            PowerUpType.LASER -> {
                activeEffects[type] = 12f
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
        val status = if (activeEffects.isEmpty()) {
            "Powerups: none"
        } else {
            val list = activeEffects.entries.joinToString(" • ") { (type, time) ->
                "${type.displayName} ${time.toInt()}s"
            }
            "Powerups: $list"
        }
        if (status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
    }

    private fun checkLevelCompletion() {
        val remaining = bricks.count { it.alive && it.type != BrickType.UNBREAKABLE }
        if (remaining == 0) {
            logger?.logLevelComplete(levelIndex, score, elapsedSeconds, remaining)
            val summary = GameSummary(score, levelIndex + 1, elapsedSeconds.toInt())
            listener.onLevelComplete(summary)
            state = GameState.PAUSED
        }
    }

    private fun loseLife() {
        // Reset combo on life loss
        combo = 0
        comboTimer = 0f

        if (config.mode.godMode) {
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
            spawnBall()
            state = GameState.READY
        }
    }

    private fun triggerGameOver() {
        val summary = GameSummary(score, levelIndex + 1, elapsedSeconds.toInt())
        audio.play(GameSound.GAME_OVER, 1f)
        audio.haptic(GameHaptic.HEAVY)
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
            val type = PowerUpType.values()[random.nextInt(PowerUpType.values().size)]
            powerups.add(PowerUp(brick.centerX, brick.centerY, type, 18f))
        }
    }

    private fun triggerExplosion(brick: Brick) {
        audio.play(GameSound.EXPLOSION, 0.8f)
        audio.haptic(GameHaptic.HEAVY)
        screenFlash = 0.3f
        val radius = 1
        bricks.filter { it.alive && it.isNeighbor(brick, radius) }.forEach { neighbor ->
            if (neighbor == brick) return@forEach
            val destroyed = neighbor.applyHit(true)
            if (destroyed) {
                score += neighbor.scoreValue
                maybeSpawnPowerup(neighbor)
            }
        }
        listener.onScoreUpdated(score)

        // Add expanding shockwave
        waves.add(
            ExplosionWave(
                x = brick.centerX,
                y = brick.centerY,
                radius = 1f,
                color = brick.currentColor(theme).copyOf(),
                life = 0.8f,
                maxLife = 0.8f,
                speed = 15f
            )
        )

        repeat(8) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = random.nextFloat() * 18f + 8f
            particles.add(
                Particle(
                    x = brick.centerX,
                    y = brick.centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    radius = 0.6f,
                    life = 0.6f,
                    color = brick.currentColor(theme)
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
        return power.x in (paddle.x - paddle.width / 2f)..(paddle.x + paddle.width / 2f) &&
            power.y in (paddle.y - paddle.height / 2f)..(paddle.y + paddle.height / 2f)
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
            ball.vx = if (overlapLeft < overlapRight) -abs(ball.vx) else abs(ball.vx)
        } else {
            ball.vy = if (overlapBottom < overlapTop) -abs(ball.vy) else abs(ball.vy)
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
    var color: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f)
) {
    val defaultColor: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f)
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
                        // Boss gets stronger in later phases
                        hitPoints = maxHitPoints * (phase + 1) / phase
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
        val positionVarianceR = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance
        val positionVarianceG = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance
        val positionVarianceB = (random.nextFloat() - 0.5f) * 0.5f  // ±0.25f variance

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
        hitFlash = max(0f, hitFlash - 0.03f)
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

enum class BrickType { NORMAL, REINFORCED, ARMORED, EXPLOSIVE, UNBREAKABLE, MOVING, SPAWNING, PHASE, BOSS }

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
