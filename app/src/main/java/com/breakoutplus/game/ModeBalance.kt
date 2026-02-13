package com.breakoutplus.game

/**
 * Centralized per-mode pacing and difficulty values so gameplay tuning is
 * consistent across launch speed scaling, clamp ranges, drop rates, and
 * invader wave cadence.
 */
object ModeBalance {
    data class Pacing(
        val speedBoostSlope: Float,
        val speedBoostCap: Float,
        val minSpeedFactor: Float,
        val maxSpeedFactor: Float,
        val difficultyBase: Float,
        val difficultySlope: Float,
        val dropChanceModeBoost: Float
    )

    data class InvaderPacing(
        val baseSpeed: Float,
        val speedPerLevel: Float,
        val speedCap: Float,
        val baseShotCooldown: Float,
        val shotCooldownPerLevel: Float,
        val shotCooldownMin: Float,
        val shieldBase: Float,
        val shieldPerLevel: Float,
        val shieldCap: Float
    )

    private val pacingByMode = mapOf(
        GameMode.CLASSIC to Pacing(
            speedBoostSlope = 0.014f,
            speedBoostCap = 1.33f,
            minSpeedFactor = 0.58f,
            maxSpeedFactor = 1.58f,
            difficultyBase = 1.0f,
            difficultySlope = 0.072f,
            dropChanceModeBoost = 0.0f
        ),
        GameMode.TIMED to Pacing(
            speedBoostSlope = 0.019f,
            speedBoostCap = 1.43f,
            minSpeedFactor = 0.67f,
            maxSpeedFactor = 1.74f,
            difficultyBase = 1.06f,
            difficultySlope = 0.092f,
            dropChanceModeBoost = 0.025f
        ),
        GameMode.ENDLESS to Pacing(
            speedBoostSlope = 0.018f,
            speedBoostCap = 1.46f,
            minSpeedFactor = 0.64f,
            maxSpeedFactor = 1.76f,
            difficultyBase = 1.03f,
            difficultySlope = 0.094f,
            dropChanceModeBoost = 0.02f
        ),
        GameMode.GOD to Pacing(
            speedBoostSlope = 0.008f,
            speedBoostCap = 1.18f,
            minSpeedFactor = 0.48f,
            maxSpeedFactor = 1.36f,
            difficultyBase = 0.86f,
            difficultySlope = 0.04f,
            dropChanceModeBoost = -0.03f
        ),
        GameMode.ZEN to Pacing(
            speedBoostSlope = 0.006f,
            speedBoostCap = 1.14f,
            minSpeedFactor = 0.44f,
            maxSpeedFactor = 1.28f,
            difficultyBase = 0.8f,
            difficultySlope = 0.03f,
            dropChanceModeBoost = -0.04f
        ),
        GameMode.RUSH to Pacing(
            speedBoostSlope = 0.02f,
            speedBoostCap = 1.48f,
            minSpeedFactor = 0.7f,
            maxSpeedFactor = 1.78f,
            difficultyBase = 1.06f,
            difficultySlope = 0.09f,
            dropChanceModeBoost = 0.045f
        ),
        GameMode.VOLLEY to Pacing(
            speedBoostSlope = 0.012f,
            speedBoostCap = 1.34f,
            minSpeedFactor = 0.58f,
            maxSpeedFactor = 1.52f,
            difficultyBase = 1.0f,
            difficultySlope = 0.062f,
            dropChanceModeBoost = -0.08f
        ),
        GameMode.TUNNEL to Pacing(
            speedBoostSlope = 0.015f,
            speedBoostCap = 1.38f,
            minSpeedFactor = 0.6f,
            maxSpeedFactor = 1.6f,
            difficultyBase = 1.04f,
            difficultySlope = 0.078f,
            dropChanceModeBoost = 0.015f
        ),
        GameMode.SURVIVAL to Pacing(
            speedBoostSlope = 0.029f,
            speedBoostCap = 1.64f,
            minSpeedFactor = 0.76f,
            maxSpeedFactor = 1.92f,
            difficultyBase = 1.2f,
            difficultySlope = 0.125f,
            dropChanceModeBoost = 0.025f
        ),
        GameMode.INVADERS to Pacing(
            speedBoostSlope = 0.017f,
            speedBoostCap = 1.44f,
            minSpeedFactor = 0.62f,
            maxSpeedFactor = 1.66f,
            difficultyBase = 1.08f,
            difficultySlope = 0.085f,
            dropChanceModeBoost = 0.035f
        )
    )

    private val invaderPacing = InvaderPacing(
        baseSpeed = 8.2f,
        speedPerLevel = 0.62f,
        speedCap = 14.5f,
        baseShotCooldown = 1.5f,
        shotCooldownPerLevel = 0.055f,
        shotCooldownMin = 0.52f,
        shieldBase = 102f,
        shieldPerLevel = 4.4f,
        shieldCap = 138f
    )

    fun pacingFor(mode: GameMode): Pacing {
        return pacingByMode[mode] ?: pacingByMode.getValue(GameMode.CLASSIC)
    }

    fun invaderPacing(): InvaderPacing = invaderPacing
}
