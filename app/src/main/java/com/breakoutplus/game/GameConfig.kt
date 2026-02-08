package com.breakoutplus.game

import com.breakoutplus.SettingsManager
import com.breakoutplus.UnlockManager

data class GameConfig(
    val mode: GameMode,
    val settings: SettingsManager.Settings,
    val dailyChallenges: MutableList<DailyChallenge>? = null,
    val unlocks: UnlockManager.UnlockState = UnlockManager.UnlockState(emptySet(), 0)
)

data class GameSummary(
    val score: Int,
    val level: Int,
    val durationSeconds: Int
)
