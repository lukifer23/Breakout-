package com.breakoutplus.game

import kotlin.math.max
import kotlin.math.min
import java.util.Locale
import kotlin.math.roundToInt


internal fun GameEngine.maybeShowPowerupTip(type: PowerUpType) {
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



internal fun GameEngine.applyPowerup(type: PowerUpType) {
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
                volleyBallCount = (volleyBallCount + 1).coerceAtMost(VolleyModeSystem.MAX_BALL_COUNT)
                listener.onVolleyBallsUpdated(volleyBallCount)
            } else if (!config.mode.relaxedMode) {
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
    markActiveEffectsDirty()
    updatePowerupStatus()
}



internal fun GameEngine.updatePowerupStatus(force: Boolean = false) {
    if (force) {
        powerupStatusTick = 0f
    }
    if (config.mode == GameMode.VOLLEY) {
        val volleyMetrics = currentVolleyMetrics()
        val status = ModeStatusText.volley(
            volleyBallCount = volleyBallCount,
            turnNumber = volleyTurnCount + 1,
            depthRows = volleyAdvanceRows,
            aliveBreakables = volleyMetrics.aliveBreakables,
            laneClearance = volleyMetrics.laneClearance,
            locale = Locale.getDefault()
        )
        if (force || status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        emitPowerupSnapshot(buildPowerupSnapshot())
        return
    }
    if (config.mode == GameMode.TUNNEL) {
        val tunnelMetrics = ModeBoardMetrics.tunnelBoardMetrics(
            bricks = bricks,
            gateZone = tunnelGateZone()
        )
        cachedTunnelGateIntegrityPercent = tunnelMetrics.gateIntegrityPercent
        tunnelGateIntegrityDirty = false
        val safeTotalBreakables = tunnelMetrics.totalBreakables.coerceAtLeast(1)
        val breachPercent = (((safeTotalBreakables - tunnelMetrics.aliveBreakables).toFloat() / safeTotalBreakables.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        val gatePressure = ModeBoardMetrics.tunnelBreakthroughPressure(
            gateIntegrityPercent = tunnelMetrics.gateIntegrityPercent,
            tunnelShotsFired = tunnelShotsFired
        )
        val hasBreakthroughActive = hasBreakthroughActiveEffect()
        val hasBreakthroughDropQueued = hasQueuedBreakthroughDrop()
        val supplyGate = TunnelModeSystem.supplyDropGate(
            gatePressure = gatePressure,
            gateIntegrityPercent = tunnelMetrics.gateIntegrityPercent,
            hasBreakthroughActive = hasBreakthroughActive,
            hasBreakthroughDropQueued = hasBreakthroughDropQueued
        )
        val shotsSinceDrop = (tunnelShotsFired - lastTunnelSupplyShot).coerceAtLeast(0)
        tunnelSupplyReadinessPercent = TunnelModeSystem.supplyReadinessPercent(
            shotsSinceDrop = shotsSinceDrop,
            requiredShots = supplyGate.requiredShots
        )
        val status = ModeStatusText.tunnel(
            shotsFired = tunnelShotsFired,
            gateIntegrityPercent = tunnelMetrics.gateIntegrityPercent,
            breachPercent = breachPercent,
            combo = combo,
            supplyReadinessPercent = tunnelSupplyReadinessPercent
        )
        if (force || status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        emitPowerupSnapshot(buildPowerupSnapshot())
        return
    }
    if (config.mode == GameMode.SURVIVAL) {
        val status = ModeStatusText.survival(speedMultiplier = speedMultiplier, combo = combo)
        if (force || status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        emitPowerupSnapshot(buildPowerupSnapshot())
        return
    }
    if (config.mode == GameMode.ZEN) {
        // Zen mode: minimal HUD, no scores or lives
        val status = "Zen Mode"
        if (force || status != lastPowerupStatus) {
            lastPowerupStatus = status
            listener.onPowerupStatus(status)
        }
        // Don't show powerup chips in zen mode.
        emitPowerupSnapshot(emptyList())
        return
    }
    val segments = mutableListOf<String>()
    val effectText = ModeStatusText.powerups(
        sortedActiveEffects().map { (type, time) ->
            ModeStatusText.EffectStatus(
                type = type,
                remainingSeconds = displaySeconds(time),
                charges = if (type == PowerUpType.SHIELD) shieldCharges else 0
            )
        }
    )
    segments.add(effectText)
    if (combo >= 2) {
        segments.add("Combo x$combo")
    }
    val status = segments.joinToString(" • ")
    if (force || status != lastPowerupStatus) {
        lastPowerupStatus = status
        listener.onPowerupStatus(status)
    }
    emitPowerupSnapshot(buildPowerupSnapshot())
}

internal fun GameEngine.powerupPriority(type: PowerUpType): Int {
    return when (type) {
        PowerUpType.SHIELD,
        PowerUpType.GUARDRAIL -> 0
        PowerUpType.LASER,
        PowerUpType.FIREBALL,
        PowerUpType.PIERCE,
        PowerUpType.DOUBLE_SCORE -> 1
        PowerUpType.WIDE_PADDLE,
        PowerUpType.SHRINK,
        PowerUpType.MAGNET -> 2
        PowerUpType.SLOW,
        PowerUpType.FREEZE,
        PowerUpType.TIME_WARP,
        PowerUpType.OVERDRIVE -> 3
        PowerUpType.MULTI_BALL,
        PowerUpType.BALL_SPLITTER,
        PowerUpType.GRAVITY_WELL,
        PowerUpType.LIFE,
        PowerUpType.RICOCHET -> 4
    }
}



internal fun GameEngine.buildPowerupSnapshot(): List<PowerupStatus> {
    val effects = sortedActiveEffects()
    powerupSnapshotBuffer.clear()
    for ((type, time) in effects) {
        powerupSnapshotBuffer.add(
            PowerupStatus(
                type = type,
                remainingSeconds = displaySeconds(time),
                charges = if (type == PowerUpType.SHIELD) shieldCharges else 0
            )
        )
    }
    return powerupSnapshotBuffer
}

internal fun GameEngine.powerupSnapshotsEqual(
    current: List<PowerupStatus>,
    previous: List<PowerupStatus>
): Boolean {
    if (current.size != previous.size) return false
    for (i in current.indices) {
        if (current[i] != previous[i]) return false
    }
    return true
}

internal fun GameEngine.emitPowerupSnapshot(snapshot: List<PowerupStatus>) {
    if (!powerupSnapshotsEqual(snapshot, lastPowerupSnapshot) || combo != lastComboReported) {
        lastPowerupSnapshot = snapshot.toList()
        lastComboReported = combo
        listener.onPowerupsUpdated(lastPowerupSnapshot, combo)
    }
}

internal fun GameEngine.markActiveEffectsDirty() {
    sortedEffectsDirty = true
}

internal fun GameEngine.sortedActiveEffects(): List<Map.Entry<PowerUpType, Float>> {
    if (sortedEffectsDirty) {
        sortedEffectsCache = activeEffects.entries.sortedWith(
            compareBy<Map.Entry<PowerUpType, Float>> { powerupPriority(it.key) }
                .thenByDescending { it.value }
        )
        sortedEffectsDirty = false
    }
    return sortedEffectsCache
}



internal fun GameEngine.maybeSpawnPowerup(brick: Brick) {
    if (config.mode == GameMode.VOLLEY) return
    val baseChance = PowerupDropModel.baseChance(brick.type)
    val modeBoost = ModeBalance.pacingFor(config.mode).dropChanceModeBoost
    var selectionHint = GameEngine.PowerupSelectionHint.DEFAULT
    var dynamicBoost = 0f
    if (config.mode == GameMode.TUNNEL) {
        val gatePressure = tunnelBreakthroughPressure()
        val gateBrick = isTunnelGateBrick(brick)
        val pacing = TunnelModeSystem.breakthroughPacing(
            gatePressure = gatePressure,
            tunnelShotsFired = tunnelShotsFired,
            gateBrickHit = gateBrick
        )
        dynamicBoost += pacing.dynamicBoost
        if (pacing.preferBreakthroughHint) {
            selectionHint = GameEngine.PowerupSelectionHint.TUNNEL_BREAKTHROUGH
        }
    }
    val dropChance = PowerupDropModel.dropChance(
        baseChance = baseChance,
        levelIndex = levelIndex,
        modeBoost = modeBoost,
        dynamicBoost = dynamicBoost
    )
    if (random.nextFloat() < dropChance) {
        spawnPowerup(brick.centerX, brick.centerY, randomPowerupType(selectionHint))
    }
}



internal fun GameEngine.spawnPowerup(x: Float, y: Float, type: PowerUpType) {
    powerups.add(PowerUp(x, y, type, 18f))
    recordPowerup(type)
    maybeShowPowerupTip(type)
    if (type == PowerUpType.LASER) {
        powerupDropsSinceLaser = 0
    } else {
        powerupDropsSinceLaser += 1
    }
}



internal fun GameEngine.randomPowerupType(hint: GameEngine.PowerupSelectionHint = GameEngine.PowerupSelectionHint.DEFAULT): PowerUpType {
    val laserGuaranteeThreshold = when (hint) {
        GameEngine.PowerupSelectionHint.VOLLEY_SUPPLY,
        GameEngine.PowerupSelectionHint.VOLLEY_SUPPLY_CRITICAL,
        GameEngine.PowerupSelectionHint.TUNNEL_BREAKTHROUGH -> 4
        GameEngine.PowerupSelectionHint.DEFAULT -> 5
    }
    if (powerupDropsSinceLaser >= laserGuaranteeThreshold && !activeEffects.containsKey(PowerUpType.LASER)) {
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
    when (hint) {
        GameEngine.PowerupSelectionHint.VOLLEY_SUPPLY -> {
            weights[PowerUpType.GUARDRAIL] = 0.02f
            weights[PowerUpType.SHIELD] = 0.04f
            weights[PowerUpType.LIFE] = (weights[PowerUpType.LIFE] ?: 0f) + 0.35f
            weights[PowerUpType.WIDE_PADDLE] = (weights[PowerUpType.WIDE_PADDLE] ?: 0f) + 0.22f
            weights[PowerUpType.MAGNET] = (weights[PowerUpType.MAGNET] ?: 0f) + 0.18f
            weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.3f
            weights[PowerUpType.MULTI_BALL] = (weights[PowerUpType.MULTI_BALL] ?: 0f) + 0.24f
            weights[PowerUpType.BALL_SPLITTER] = (weights[PowerUpType.BALL_SPLITTER] ?: 0f) + 0.22f
            weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.18f
            weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.15f
            weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.35f }
            weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.45f }
        }
        GameEngine.PowerupSelectionHint.VOLLEY_SUPPLY_CRITICAL -> {
            weights[PowerUpType.GUARDRAIL] = 0.01f
            weights[PowerUpType.SHIELD] = 0.02f
            weights[PowerUpType.LIFE] = (weights[PowerUpType.LIFE] ?: 0f) + 0.45f
            weights[PowerUpType.WIDE_PADDLE] = (weights[PowerUpType.WIDE_PADDLE] ?: 0f) + 0.3f
            weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.24f
            weights[PowerUpType.FREEZE] = (weights[PowerUpType.FREEZE] ?: 0f) + 0.24f
            weights[PowerUpType.TIME_WARP] = (weights[PowerUpType.TIME_WARP] ?: 0f) + 0.18f
            weights[PowerUpType.MAGNET] = (weights[PowerUpType.MAGNET] ?: 0f) + 0.2f
            weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.24f
            weights[PowerUpType.MULTI_BALL] = (weights[PowerUpType.MULTI_BALL] ?: 0f) + 0.18f
            weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.3f }
            weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.4f }
        }
        GameEngine.PowerupSelectionHint.TUNNEL_BREAKTHROUGH -> {
            weights[PowerUpType.PIERCE] = (weights[PowerUpType.PIERCE] ?: 0f) + 0.42f
            weights[PowerUpType.FIREBALL] = (weights[PowerUpType.FIREBALL] ?: 0f) + 0.34f
            weights[PowerUpType.LASER] = (weights[PowerUpType.LASER] ?: 0f) + 0.24f
            weights[PowerUpType.MULTI_BALL] = (weights[PowerUpType.MULTI_BALL] ?: 0f) + 0.18f
            weights[PowerUpType.BALL_SPLITTER] = (weights[PowerUpType.BALL_SPLITTER] ?: 0f) + 0.16f
            weights[PowerUpType.GRAVITY_WELL] = (weights[PowerUpType.GRAVITY_WELL] ?: 0f) + 0.16f
            weights[PowerUpType.SLOW] = (weights[PowerUpType.SLOW] ?: 0f) + 0.12f
            weights[PowerUpType.WIDE_PADDLE] = (weights[PowerUpType.WIDE_PADDLE] ?: 0f) + 0.08f
            weights[PowerUpType.SHRINK]?.let { weights[PowerUpType.SHRINK] = it * 0.6f }
            weights[PowerUpType.OVERDRIVE]?.let { weights[PowerUpType.OVERDRIVE] = it * 0.7f }
        }
        GameEngine.PowerupSelectionHint.DEFAULT -> Unit
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



internal fun GameEngine.powerupBucket(type: PowerUpType): GameEngine.PowerupBucket {
    return when (type) {
        PowerUpType.MULTI_BALL,
        PowerUpType.LASER,
        PowerUpType.FIREBALL,
        PowerUpType.BALL_SPLITTER,
        PowerUpType.PIERCE -> GameEngine.PowerupBucket.OFFENSE
        PowerUpType.GUARDRAIL,
        PowerUpType.SHIELD,
        PowerUpType.LIFE,
        PowerUpType.WIDE_PADDLE -> GameEngine.PowerupBucket.DEFENSE
        PowerUpType.SLOW,
        PowerUpType.FREEZE,
        PowerUpType.MAGNET,
        PowerUpType.GRAVITY_WELL -> GameEngine.PowerupBucket.CONTROL
        PowerUpType.SHRINK,
        PowerUpType.OVERDRIVE,
        PowerUpType.RICOCHET,
        PowerUpType.TIME_WARP,
        PowerUpType.DOUBLE_SCORE -> GameEngine.PowerupBucket.RISK
    }
}



internal fun GameEngine.recordPowerup(type: PowerUpType) {
    recentPowerups.addLast(type)
    while (recentPowerups.size > recentPowerupLimit) {
        recentPowerups.removeFirst()
    }
    when (powerupBucket(type)) {
        GameEngine.PowerupBucket.OFFENSE -> {
            powerupsSinceOffense = 0
            powerupsSinceDefense += 1
            powerupsSinceControl += 1
        }
        GameEngine.PowerupBucket.DEFENSE -> {
            powerupsSinceDefense = 0
            powerupsSinceOffense += 1
            powerupsSinceControl += 1
        }
        GameEngine.PowerupBucket.CONTROL -> {
            powerupsSinceControl = 0
            powerupsSinceOffense += 1
            powerupsSinceDefense += 1
        }
        GameEngine.PowerupBucket.RISK -> {
            powerupsSinceOffense += 1
            powerupsSinceDefense += 1
            powerupsSinceControl += 1
        }
    }
    powerupsSinceOffense = powerupsSinceOffense.coerceAtMost(12)
    powerupsSinceDefense = powerupsSinceDefense.coerceAtMost(12)
    powerupsSinceControl = powerupsSinceControl.coerceAtMost(12)
}



internal fun GameEngine.spawnPowerupBurst(power: PowerUp) {
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



internal fun GameEngine.powerIntersectsPaddle(power: PowerUp): Boolean {
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



internal fun GameEngine.updatePowerups(dt: Float) {
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

