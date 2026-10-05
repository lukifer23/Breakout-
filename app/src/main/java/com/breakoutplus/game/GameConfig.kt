package com.breakoutplus.game


data class GameConfig(
    val mode: GameMode,
    val settings: GameSettings,
    val dailyChallenges: MutableList<DailyChallenge>? = null,
    val unlocks: GameUnlocks = GameUnlocks(emptySet(), 0),
    val seed: Long = java.security.SecureRandom().nextLong()
)

data class GameSummary(
    val score: Int,
    val level: Int,
    val durationSeconds: Int,
    val bricksBroken: Int = 0,
    val livesLost: Int = 0
)
