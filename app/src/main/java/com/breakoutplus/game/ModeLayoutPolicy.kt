package com.breakoutplus.game

object ModeLayoutPolicy {
    fun volleyRowBoost(
        aspectRatio: Float,
        isSlate: Boolean,
        levelIndex: Int
    ): Int {
        if (isSlate) {
            val progressionBoost = (levelIndex / 6).coerceIn(0, 4)
            val slateBase = when {
                aspectRatio <= 1.45f -> 14
                aspectRatio <= 1.7f -> 13
                else -> 12
            }
            return slateBase + progressionBoost
        }
        return when {
            aspectRatio > 2.15f -> 2
            aspectRatio > 1.95f -> 3
            aspectRatio > 1.75f -> 5
            else -> 6
        }
    }

    fun tunnelRowBoost(
        aspectRatio: Float,
        isSlate: Boolean,
        levelIndex: Int
    ): Int {
        if (isSlate) {
            val progressionBoost = (levelIndex / 7).coerceIn(0, 3)
            val slateBase = when {
                aspectRatio <= 1.45f -> 16
                aspectRatio <= 1.7f -> 15
                else -> 14
            }
            return slateBase + progressionBoost
        }

        val densityBoost = (levelIndex / 6).coerceAtMost(2)
        val baseRowBoost = when {
            aspectRatio > 2.05f -> 4
            aspectRatio > 1.85f -> 2
            else -> 0
        }
        return baseRowBoost + 3 + densityBoost
    }
}