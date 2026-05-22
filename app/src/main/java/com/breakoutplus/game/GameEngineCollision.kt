package com.breakoutplus.game

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt


internal fun GameEngine.handlePaddleCollision(ball: Ball) {
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



internal fun GameEngine.handleBrickCollision(ball: Ball) {
    val nearbyBricks = getNearbyBricks(ball)
    for (brick in nearbyBricks) {
        if (!brick.alive) continue
        if (!GameCollisionSystem.circleIntersectsRect(ball, brick)) continue

        // Resolve bounce before applying hit to ensure consistent collision response
        if (!fireballActive && !pierceActive) {
            GameCollisionSystem.bounceBallFromBrick(ball, brick)
        }
        val destroyed = brick.applyHit(fireballActive || pierceActive)

        if (destroyed) {
            handleBrickDestroyedByBall(ball, brick)
        } else {
            combo = 0
            comboTimer = 0f
            audio.play(GameSound.BOUNCE, 0.5f)
        }
        markTunnelGateIntegrityDirtyIfGateBrick(brick)

        spawnImpactSparks(ball.x, ball.y, brick.currentColor(theme), 4, 12f)
        reportScore()
        break
    }
}



internal fun GameEngine.handleBrickDestroyedByBall(ball: Ball, brick: Brick) {
    updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
    runBricksBroken += 1
    onBrickDestroyed(brick)
    spawnBrickDestructionFx(brick, ball.x, ball.y, intensity = 1f)

    if (brick.type == BrickType.BOSS) {
        emitVisualFeedback(GameEngine.VisualFeedbackEvent.BOSS_BREAK)
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

    comboTimer = 2f
    val oldCombo = combo
    combo += 1

    if (combo >= 5 && combo % 2 == 0 && combo > oldCombo) {
        spawnComboStreakParticles(brick.centerX, brick.centerY, combo)
    }

    updateDailyChallenges(ChallengeType.COMBO_MULTIPLIER, combo)

    val multiplier = BrickCollisionFeedback.comboMultiplier(combo)
    if (BrickCollisionFeedback.shouldTriggerComboFlash(multiplier)) {
        emitVisualFeedback(GameEngine.VisualFeedbackEvent.COMBO_STREAK)
    }

    val baseScore = (brick.scoreValue * multiplier).roundToInt()
    addScore(baseScore)

    if (combo >= 3) {
        logger?.logComboAchieved(combo, multiplier, (brick.scoreValue * multiplier).toInt())
        listener.onTip("Combo x${combo}!")
    }

    logger?.logBrickDestroyed(brick.type, Pair(brick.centerX, brick.centerY), combo)

    val brickSound = brickSoundFor(brick.type)
    val baseRate = brickSoundRate(brick.type)
    val dynamicRate = BrickCollisionFeedback.dynamicBrickSoundRate(
        baseRate = baseRate,
        combo = combo,
        randomUnit = random.nextFloat()
    )

    audio.play(brickSound, 0.7f, dynamicRate)
    audio.haptic(GameHaptic.LIGHT)
    maybeSpawnPowerup(brick)
    if (brick.type == BrickType.EXPLOSIVE) {
        triggerExplosion(brick)
    }
    if (brick.type == BrickType.SPAWNING) {
        spawnChildBricks(brick)
    }
}



internal fun GameEngine.handleBrickCollisionFromBeam(beam: Beam, brick: Brick) {
    val destroyed = brick.applyHit(true)
    markTunnelGateIntegrityDirtyIfGateBrick(brick)
    if (destroyed) {
        addScore(brick.scoreValue)
        updateDailyChallenges(ChallengeType.BRICKS_DESTROYED)
        runBricksBroken += 1
        onBrickDestroyed(brick)

        // Visual effects
        emitVisualFeedback(GameEngine.VisualFeedbackEvent.BEAM_BRICK_BREAK)
        spawnBrickDestructionFx(brick, beam.x, beam.y, intensity = 0.84f)
        val brickSound = brickSoundFor(brick.type)
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
}



internal fun GameEngine.handleBeamCollision() {
    if (spatialHashDirty || spatialHash.isEmpty()) {
        buildSpatialHash()
    }
    val iterator = beams.iterator()
    while (iterator.hasNext()) {
        val beam = iterator.next()
        var hitBrick: Brick? = null
        val nearby = getNearbyBricksAt(beam.x, beam.y, max(beam.width, beam.height) * 0.6f)
        for (brick in nearby) {
            if (!brick.alive) continue
            if (!GameCollisionSystem.beamIntersectsBrick(beam, brick)) continue
            hitBrick = brick
            break
        }
        if (hitBrick != null) {
            handleBrickCollisionFromBeam(beam, hitBrick)
            iterator.remove()
        }
    }
}



internal fun GameEngine.handleInvaderShotHit(shot: EnemyShot): Boolean {
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
        emitVisualFeedback(GameEngine.VisualFeedbackEvent.INVADER_SHIELD_HIT)
        if (!invaderShieldCritical && invaderShieldMax > 0f && invaderShield <= invaderShieldMax * 0.25f) {
            invaderShieldCritical = true
            listener.onTip("Shield critical! Avoid direct hits.")
            audio.play(GameSound.EXPLOSION, 0.35f)
        }
        if (invaderShield <= 0f && !invaderShieldAlerted) {
            invaderShieldAlerted = true
            shieldBreakPulse = 1f
            audio.play(GameSound.EXPLOSION, 0.55f)
            emitVisualFeedback(GameEngine.VisualFeedbackEvent.INVADER_SHIELD_BREAK)
            listener.onTip("Shield down! Dodge the incoming fire.")
        }
        return false
    } else {
        return true
    }
}



internal fun GameEngine.buildSpatialHash() {
    for (bucket in spatialHash.values) {
        bucket.clear()
    }
    for (brick in bricks) {
        if (!brick.alive) continue
        val minX = (brick.x / spatialHashCellSize).toInt()
        val maxX = ((brick.x + brick.width) / spatialHashCellSize).toInt()
        val minY = (brick.y / spatialHashCellSize).toInt()
        val maxY = ((brick.y + brick.height) / spatialHashCellSize).toInt()

        for (cellX in minX..maxX) {
            for (cellY in minY..maxY) {
                val cellKey = spatialKey(cellX, cellY)
                spatialHash.getOrPut(cellKey) { mutableListOf() }.add(brick)
            }
        }
    }
    spatialHashDirty = false
}

