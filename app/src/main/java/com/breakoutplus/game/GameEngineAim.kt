package com.breakoutplus.game

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.exp
import kotlin.math.sqrt

internal fun GameEngine.findAimCollision(
    startX: Float,
    startY: Float,
    dirX: Float,
    dirY: Float,
    radius: Float
): GameEngine.AimHit {
    val wallHit = findAimWallCollision(startX, startY, dirX, dirY, radius)
    val brickHit = findAimBrickCollision(startX, startY, dirX, dirY, radius, wallHit.t)
    return if (brickHit != null && brickHit.t < wallHit.t) brickHit else wallHit
}



internal fun GameEngine.findAimWallCollision(
    startX: Float,
    startY: Float,
    dirX: Float,
    dirY: Float,
    radius: Float
): GameEngine.AimHit {
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

    return GameEngine.AimHit(bestT, normalX, normalY, hitsBrick = false)
}



internal fun GameEngine.findAimBrickCollision(
    startX: Float,
    startY: Float,
    dirX: Float,
    dirY: Float,
    radius: Float,
    maxDistance: Float
): GameEngine.AimHit? {
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

    return if (found) GameEngine.AimHit(bestT, bestNx, bestNy, hitsBrick = true) else null
}



internal fun GameEngine.updateAimFromPaddle() {
    val center = worldWidth * 0.5f
    val sourceX = if (isDragging) paddle.targetX else paddle.x
    val delta = (sourceX - center) / center
    aimNormalizedTarget = delta.coerceIn(-1f, 1f)
}



internal fun GameEngine.updateAimFromTouch() {
    val ball = balls.firstOrNull() ?: return updateAimFromPaddle()

    // Use direct finger-to-ball aiming when ball is stuck to paddle
    if (ball.stuckToPaddle) {
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



internal fun GameEngine.applyAimFromNormalized(normalized: Float) {
    val clamped = normalized.coerceIn(-1f, 1f)
    val stabilized = if (abs(clamped) < aimCenterDeadZone) 0f else clamped
    aimHasInput = isDragging || abs(stabilized) > 0.001f
    val eased = stabilized * (0.8f + abs(stabilized) * 0.2f)
    val centerAngle = Math.PI.toFloat() * 0.5f
    val maxDeflection = (centerAngle - aimMinAngle).coerceAtLeast(0.2f)
    val signedDeflection = eased * maxDeflection
    aimAngle = (centerAngle - signedDeflection).coerceIn(aimMinAngle, Math.PI.toFloat() - aimMinAngle)
}



internal fun GameEngine.syncAimForLaunch() {
    val stuckReadyBall = state == GameState.READY && hasStuckBall()
    if (isDragging && stuckReadyBall) {
        // Preserve finger-directed intent so launch trajectory matches the aim guide.
        updateAimFromTouch()
    } else {
        updateAimFromPaddle()
    }
    aimNormalized = aimNormalizedTarget
    applyAimFromNormalized(aimNormalized)
}



internal fun GameEngine.updateAim(dt: Float) {
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



internal fun GameEngine.launchBallWithAim(ball: Ball, angleOffset: Float = 0f) {
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
        maybeSpawnTunnelSupplyDrop()
    }
}

