package com.breakoutplus.game

/**
 * Shared powerup drop-rate math extracted from GameEngine.
 * Centralizes base chance and clamped scaling rules.
 */
object PowerupDropModel {
    fun baseChance(type: BrickType): Float {
        return when (type) {
            BrickType.EXPLOSIVE -> 0.30f
            BrickType.REINFORCED, BrickType.ARMORED -> 0.2f
            BrickType.BOSS, BrickType.PHASE -> 0.27f
            BrickType.SPAWNING -> 0.18f
            BrickType.MOVING -> 0.15f
            BrickType.INVADER -> 0.16f
            else -> 0.12f
        }
    }

    fun levelBoost(levelIndex: Int): Float {
        return (levelIndex * 0.0035f).coerceAtMost(0.06f)
    }

    fun dropChance(
        baseChance: Float,
        levelIndex: Int,
        modeBoost: Float,
        dynamicBoost: Float
    ): Float {
        return (baseChance + levelBoost(levelIndex) + modeBoost + dynamicBoost)
            .coerceIn(0.08f, 0.48f)
    }
}
