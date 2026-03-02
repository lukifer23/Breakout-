package com.breakoutplus.game

object ModeLayoutPolicy {
    fun volleyRowBoost(
        aspectRatio: Float,
        isSlate: Boolean,
        levelIndex: Int
    ): Int {
        if (isSlate) {
            val progressionBoost = (levelIndex / 7).coerceIn(0, 3)
            val slateBase = when {
                aspectRatio <= 1.45f -> 10
                aspectRatio <= 1.7f -> 9
                else -> 8
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
}