package com.breakoutplus.game


data class GameConfig(
    val mode: GameMode,
    val settings: com.breakoutplus.game.GameSettings,
    val dailyChallenges: MutableList<DailyChallenge>? = null,
    val unlocks: com.breakoutplus.game.GameUnlocks = com.breakoutplus.game.GameUnlocks(emptySet(), 0),
    val seed: Long = java.security.SecureRandom().nextLong(),
    val runId: String = java.util.UUID.randomUUID().toString(),
    val challengeDate: java.time.LocalDate = java.time.LocalDate.now(),
    val rewardBonuses: RewardBonuses = RewardBonuses(),
    val initialState: RunSnapshot? = null,
    val restorePaused: Boolean = false,
    val persistRun: Boolean = true,
    val debugPerformanceCapture: Boolean = false,
    val debugStressScenario: String? = null
)

data class GameSummary(
    val score: Int,
    val level: Int,
    val durationSeconds: Int,
    val bricksBroken: Int = 0,
    val livesLost: Int = 0
)
