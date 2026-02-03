package com.breakoutplus.game

import com.breakoutplus.SettingsManager

data class GameConfig(
    val mode: GameMode,
    val settings: SettingsManager.Settings
)

data class GameSummary(
    val score: Int,
    val level: Int,
    val durationSeconds: Int
)
