package com.breakoutplus.game

/**
 * Game mode configurations controlling lives, timers, and special rules.
 */
enum class GameMode(
    val displayName: String,
    val description: String,
    val meta: String,
    val baseLives: Int,
    val timeLimitSeconds: Int,
    val endless: Boolean,
    val godMode: Boolean,
    val rush: Boolean,
    val launchSpeed: Float
) {
    CLASSIC(
        displayName = "Classic",
        description = "Standard breakout with escalating levels and powerups.",
        meta = "Lives 3 • No timer • Balanced",
        baseLives = 3,
        timeLimitSeconds = 0,
        endless = false,
        godMode = false,
        rush = false,
        launchSpeed = 37f
    ),
    TIMED(
        displayName = "Timed Challenge",
        description = "Race the clock. Clear as many bricks as possible.",
        meta = "Lives 2 • 2:30 timer • Fast",
        baseLives = 2,
        timeLimitSeconds = 150,
        endless = false,
        godMode = false,
        rush = false,
        launchSpeed = 42f
    ),
    ENDLESS(
        displayName = "Endless",
        description = "Infinite levels with scaling speed and brick density.",
        meta = "Lives 3 • No timer • Scaling",
        baseLives = 3,
        timeLimitSeconds = 0,
        endless = true,
        godMode = false,
        rush = false,
        launchSpeed = 39f
    ),
    GOD(
        displayName = "God Mode",
        description = "Practice mode. No life loss, perfect for experimentation.",
        meta = "Infinite lives • No timer",
        baseLives = 99,
        timeLimitSeconds = 0,
        endless = false,
        godMode = true,
        rush = false,
        launchSpeed = 33f
    ),
    RUSH(
        displayName = "Level Rush",
        description = "Beat each stage before the timer expires.",
        meta = "Lives 1 • 0:45 per level • Hardcore",
        baseLives = 1,
        timeLimitSeconds = 45,
        endless = false,
        godMode = false,
        rush = true,
        launchSpeed = 44f
    );
}
