package com.breakoutplus.game

/**
 * Provides curated theme identity per mode while still allowing light variety.
 */
object ModeTheme {
    private val classicRotation = listOf("Neon", "Cobalt", "Aurora")
    private val timedRotation = listOf("Sunset", "Ember", "Lava")
    private val endlessRotation = listOf("Neon", "Sunset", "Cobalt", "Aurora", "Forest", "Lava", "Circuit", "Vapor", "Ember")
    private val godRotation = listOf("Aurora", "Forest", "Cobalt")
    private val rushRotation = listOf("Lava", "Ember", "Sunset")
    private val volleyRotation = listOf("Circuit", "Lava", "Cobalt", "Ember", "Vapor")
    private val tunnelRotation = listOf("Cobalt", "Circuit", "Forest", "Ember")
    private val survivalRotation = listOf("Forest", "Circuit", "Cobalt")

    private val fallbackByMode = mapOf(
        GameMode.CLASSIC to "Neon",
        GameMode.TIMED to "Sunset",
        GameMode.ENDLESS to "Neon",
        GameMode.GOD to "Aurora",
        GameMode.RUSH to "Lava",
        GameMode.VOLLEY to "Circuit",
        GameMode.TUNNEL to "Cobalt",
        GameMode.SURVIVAL to "Forest",
        GameMode.INVADERS to "Invaders"
    )

    fun themeFor(mode: GameMode, levelIndex: Int, availableThemeNames: Set<String>): LevelTheme {
        if (mode == GameMode.INVADERS) {
            return LevelThemes.INVADERS
        }

        val unlocked = availableThemeNames.toSet()
        val orderedUnlocked = (LevelThemes.baseThemes() + LevelThemes.bonusThemes() + listOf(LevelThemes.DEFAULT))
            .map { it.name }
            .filter { it in unlocked }
        val rotation = when (mode) {
            GameMode.CLASSIC -> classicRotation
            GameMode.TIMED -> timedRotation
            GameMode.ENDLESS -> endlessRotation
            GameMode.GOD -> godRotation
            GameMode.RUSH -> rushRotation
            GameMode.VOLLEY -> volleyRotation
            GameMode.TUNNEL -> tunnelRotation
            GameMode.SURVIVAL -> survivalRotation
            GameMode.INVADERS -> listOf("Invaders")
        }
        val filtered = rotation.filter { it in unlocked }
        val chosenName = if (filtered.isNotEmpty()) {
            filtered[positiveMod(levelIndex, filtered.size)]
        } else {
            val fallback = fallbackByMode[mode] ?: "Neon"
            if (fallback in unlocked) {
                fallback
            } else {
                orderedUnlocked.firstOrNull() ?: fallback
            }
        }
        return LevelThemes.themeByName(chosenName) ?: LevelThemes.DEFAULT
    }

    private fun positiveMod(value: Int, size: Int): Int {
        val mod = value % size
        return if (mod < 0) mod + size else mod
    }
}
