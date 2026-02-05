package com.breakoutplus.game

import com.breakoutplus.SettingsManager

data class GameConfig(
    val mode: GameMode,
    val settings: SettingsManager.Settings,
    val dailyChallenges: MutableList<DailyChallenge>? = null
)

data class GameSummary(
    val score: Int,
    val level: Int,
    val durationSeconds: Int
)
