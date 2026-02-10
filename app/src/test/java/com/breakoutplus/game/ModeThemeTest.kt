package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ModeThemeTest {
    @Test
    fun invadersModeAlwaysUsesInvadersTheme() {
        val chosen = ModeTheme.themeFor(
            mode = GameMode.INVADERS,
            levelIndex = 4,
            availableThemeNames = setOf("Neon", "Sunset", "Invaders")
        )
        assertEquals("Invaders", chosen.name)
    }

    @Test
    fun rushModeFallsBackWhenPreferredThemeLocked() {
        val chosen = ModeTheme.themeFor(
            mode = GameMode.RUSH,
            levelIndex = 1,
            availableThemeNames = setOf("Neon", "Cobalt")
        )
        assertEquals("Neon", chosen.name)
    }

    @Test
    fun classicModeUsesRotationWhenAvailable() {
        val chosen = ModeTheme.themeFor(
            mode = GameMode.CLASSIC,
            levelIndex = 2,
            availableThemeNames = setOf("Neon", "Cobalt", "Aurora")
        )
        assertEquals("Aurora", chosen.name)
    }
}
