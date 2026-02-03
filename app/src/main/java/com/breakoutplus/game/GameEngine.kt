package com.breakoutplus.game

import android.view.MotionEvent
import com.breakoutplus.game.LevelFactory.buildLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    private val config: GameConfig,
    private val listener: GameEventListener,
    private val audio: GameAudioManager
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
    private var score = 0
    private var levelIndex = 0
    private var lives = config.mode.baseLives
    private var timeRemaining = config.mode.timeLimitSeconds.toFloat()
    private var lastReportedSecond = -1
    private var elapsedSeconds = 0f
    private var combo = 0
    private var guardrailActive = false
    private var shieldCharges = 0
    private var laserCooldown = 0f
    private var speedMultiplier = 1f
    private var fireballActive = false

    private var theme: LevelTheme = LevelThemes.DEFAULT
    private var currentLayout: LevelFactory.LevelLayout? = null

    init {
        listener.onModeUpdated(config.mode)
        resetLevel(first = true)
        listener.onLivesUpdated(lives)
        listener.onScoreUpdated(score)
        listener.onLevelUpdated(levelIndex + 1)
        listener.onPowerupStatus("Powerups: none")
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
        updateTimers(dt)
        updatePaddle(dt)
        updateEffects(dt)

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
        renderer.drawRect(0f, 0f, worldWidth, worldHeight, theme.background)

        if (guardrailActive) {
            renderer.drawRect(0f, 2f, worldWidth, 0.6f, theme.accent)
        }

        bricks.filter { it.alive }.forEach { brick ->
            val color = brick.currentColor(theme)
            renderer.drawRect(brick.x, brick.y, brick.width, brick.height, color)
            val highlight = adjustColor(color, 1.15f, 1f)
            val shadow = adjustColor(color, 0.75f, 1f)
            val highlightHeight = brick.height * 0.14f
            val shadowHeight = brick.height * 0.12f
            renderer.drawRect(brick.x, brick.y + brick.height - highlightHeight, brick.width, highlightHeight, highlight)
            renderer.drawRect(brick.x, brick.y, brick.width, shadowHeight, shadow)

            when (brick.type) {
                BrickType.REINFORCED -> drawStripe(renderer, brick, adjustColor(color, 0.85f, 1f), 1)
                BrickType.ARMORED -> drawStripe(renderer, brick, adjustColor(color, 0.78f, 1f), 2)
                BrickType.UNBREAKABLE -> drawStripe(renderer, brick, adjustColor(color, 0.7f, 1f), 3)
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

        renderer.drawRect(paddle.x - paddle.width / 2f, paddle.y - paddle.height / 2f, paddle.width, paddle.height, theme.paddle)

        balls.forEach { ball ->
            renderer.drawCircle(ball.x, ball.y, ball.radius, ball.color)
        }
    }

    private fun renderPowerup(renderer: Renderer2D, power: PowerUp) {
        val size = power.size
        val outer = adjustColor(power.type.color, 0.6f, 1f)
        val inner = power.type.color
        val x = power.x - size / 2f
        val y = power.y - size / 2f
        renderer.drawRect(x, y, size, size, outer)
        renderer.drawRect(x + size * 0.12f, y + size * 0.12f, size * 0.76f, size * 0.76f, inner)

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

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return
        val x = event.x / viewWidth * worldWidth
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                paddle.targetX = x
                if (state == GameState.READY) {
                    launchBall()
                    state = GameState.RUNNING
                    listener.onTip("Tap with two fingers to fire when laser is active")
                }
                if (event.pointerCount > 1 && activeEffects.containsKey(PowerUpType.LASER)) {
                    shootLaser()
                }
            }
        }
    }

    fun pause() {
        state = GameState.PAUSED
    }

    fun resume() {
        if (state == GameState.PAUSED) state = GameState.RUNNING
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
        speedMultiplier = 1f
        activeEffects.clear()
        balls.clear()
        beams.clear()
        powerups.clear()
        particles.clear()

        val difficulty = 1f + levelIndex * 0.08f
        val level = buildLevel(levelIndex, difficulty)
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
        listener.onTip(level.tip)
    }

    private fun buildBricks(layout: LevelFactory.LevelLayout) {
        bricks.clear()
        val rows = layout.rows
        val cols = layout.cols
        val spacing = 0.6f
        val areaTop = worldHeight * 0.88f
        val areaBottom = worldHeight * 0.55f
        val areaHeight = areaTop - areaBottom
        val brickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val brickWidth = (worldWidth - spacing * (cols + 1)) / cols
        layout.bricks.forEach { spec ->
            val x = spacing + spec.col * (brickWidth + spacing)
            val y = areaBottom + (rows - 1 - spec.row) * (brickHeight + spacing * 0.5f)
            bricks.add(
                Brick(
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
            )
        }
    }

    private fun relayoutBricks() {
        val layout = currentLayout ?: return
        if (bricks.isEmpty()) return
        val rows = layout.rows
        val cols = layout.cols
        val spacing = 0.6f
        val areaTop = worldHeight * 0.88f
        val areaBottom = worldHeight * 0.55f
        val areaHeight = areaTop - areaBottom
        val brickHeight = (areaHeight - spacing * (rows - 1)) / rows
        val brickWidth = (worldWidth - spacing * (cols + 1)) / cols
        bricks.forEach { brick ->
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
                ball.vx = 26f * kotlin.math.cos(angle) * dir
                ball.vy = 32f * kotlin.math.sin(angle)
            }
        }
    }

    private fun spawnBall() {
        val ball = Ball(paddle.x, paddle.y + 5f, 1.2f, 0f, 0f)
        if (fireballActive) {
            ball.isFireball = true
            ball.color = PowerUpType.FIREBALL.color
        }
        balls.add(ball)
    }

    private fun updateBalls(dt: Float) {
        val iterator = balls.iterator()
        while (iterator.hasNext()) {
            val ball = iterator.next()
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

            val destroyed = brick.applyHit(fireballActive)
            if (!fireballActive) {
                bounceBallFromBrick(ball, brick)
            }

            if (destroyed) {
                combo += 1
                score += brick.scoreValue * combo
                audio.play(GameSound.BRICK, 0.7f)
                audio.haptic(GameHaptic.LIGHT)
                maybeSpawnPowerup(brick)
                if (brick.type == BrickType.EXPLOSIVE) {
                    triggerExplosion(brick)
                }
            } else {
                combo = 0
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
            power.y -= power.speed * dt
            if (power.y < -4f) {
                iterator.remove()
                continue
            }
            if (powerIntersectsPaddle(power)) {
                applyPowerup(power.type)
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
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - dt
            if (remaining <= 0f) {
                when (entry.key) {
                    PowerUpType.WIDE_PADDLE -> paddle.width = basePaddleWidth
                    PowerUpType.SLOW -> speedMultiplier = 1f
                    PowerUpType.GUARDRAIL -> guardrailActive = false
                    PowerUpType.LASER -> Unit
                    PowerUpType.SHIELD -> Unit
                    PowerUpType.FIREBALL -> {
                        fireballActive = false
                        balls.forEach { it.isFireball = false; it.color = it.defaultColor }
                    }
                    else -> Unit
                }
                iterator.remove()
            } else {
                entry.setValue(remaining)
            }
        }
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
                balls.forEach { it.isFireball = true; it.color = PowerUpType.FIREBALL.color }
                activeEffects[type] = 10f
            }
        }
        audio.haptic(GameHaptic.MEDIUM)
        updatePowerupStatus()
    }

    private fun updatePowerupStatus() {
        if (activeEffects.isEmpty()) {
            listener.onPowerupStatus("Powerups: none")
            return
        }
        val list = activeEffects.keys.joinToString(" • ") { it.displayName }
        listener.onPowerupStatus("Powerups: $list")
    }

    private fun checkLevelCompletion() {
        val remaining = bricks.count { it.alive && it.type != BrickType.UNBREAKABLE }
        if (remaining == 0) {
            val summary = GameSummary(score, levelIndex + 1, elapsedSeconds.toInt())
            listener.onLevelComplete(summary)
            state = GameState.PAUSED
        }
    }

    private fun loseLife() {
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
        audio.play(GameSound.LASER, 0.7f)
    }

    private fun maybeSpawnPowerup(brick: Brick) {
        val dropChance = when (brick.type) {
            BrickType.EXPLOSIVE -> 0.3f
            BrickType.REINFORCED, BrickType.ARMORED -> 0.25f
            else -> 0.18f
        }
        if (random.nextFloat() < dropChance) {
            val type = PowerUpType.values()[random.nextInt(PowerUpType.values().size)]
            powerups.add(PowerUp(brick.centerX, brick.centerY, type, 18f))
        }
    }

    private fun triggerExplosion(brick: Brick) {
        audio.play(GameSound.EXPLOSION, 0.8f)
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
    val scoreValue: Int = when (type) {
        BrickType.NORMAL -> 50
        BrickType.REINFORCED -> 80
        BrickType.ARMORED -> 120
        BrickType.EXPLOSIVE -> 150
        BrickType.UNBREAKABLE -> 200
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
        hitPoints -= if (forceBreak && type == BrickType.UNBREAKABLE) 2 else 1
        hitFlash = 0.2f
        if (hitPoints <= 0) {
            alive = false
            return true
        }
        return false
    }

    fun currentColor(theme: LevelTheme): FloatArray {
        val base = theme.brickPalette[type] ?: theme.accent
        val ratio = if (type == BrickType.UNBREAKABLE) 1f else (hitPoints.toFloat() / maxHitPoints.toFloat()).coerceIn(0.4f, 1f)
        val variance = ((gridX + gridY) % 3) * 0.04f
        val scaled = floatArrayOf(
            (base[0] * ratio + variance).coerceIn(0f, 1f),
            (base[1] * ratio + variance).coerceIn(0f, 1f),
            (base[2] * ratio + variance).coerceIn(0f, 1f),
            1f
        )
        if (hitFlash <= 0f) return scaled
        hitFlash = max(0f, hitFlash - 0.03f)
        return floatArrayOf(
            min(1f, scaled[0] + 0.25f),
            min(1f, scaled[1] + 0.25f),
            min(1f, scaled[2] + 0.25f),
            1f
        )
    }

    fun isNeighbor(other: Brick, radius: Int): Boolean {
        return abs(gridX - other.gridX) <= radius && abs(gridY - other.gridY) <= radius
    }
}

enum class BrickType { NORMAL, REINFORCED, ARMORED, EXPLOSIVE, UNBREAKABLE }

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
    FIREBALL("Fireball", floatArrayOf(1f, 0.36f, 0.27f, 1f))
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
