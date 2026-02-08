package com.breakoutplus

import android.content.Context
import com.breakoutplus.game.LevelTheme
import com.breakoutplus.game.LevelThemes

object UnlockManager {
    private const val PREFS_NAME = "breakout_plus_unlocks"
    private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
    private const val KEY_COSMETIC_TIER = "cosmetic_tier"
    private const val MAX_COSMETIC_TIER = 3

    data class UnlockState(
        val unlockedThemes: Set<String>,
        val cosmeticTier: Int
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): UnlockState {
        val prefs = prefs(context)
        val rawThemes = prefs.getString(KEY_UNLOCKED_THEMES, "") ?: ""
        val unlockedThemes = rawThemes.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val cosmeticTier = prefs.getInt(KEY_COSMETIC_TIER, 0).coerceIn(0, MAX_COSMETIC_TIER)
        return UnlockState(unlockedThemes, cosmeticTier)
    }

    fun save(context: Context, state: UnlockState) {
        val serialized = state.unlockedThemes.joinToString(",")
        prefs(context).edit()
            .putString(KEY_UNLOCKED_THEMES, serialized)
            .putInt(KEY_COSMETIC_TIER, state.cosmeticTier.coerceIn(0, MAX_COSMETIC_TIER))
            .apply()
    }

    fun unlockCosmetic(context: Context): UnlockState {
        val current = load(context)
        val nextTier = (current.cosmeticTier + 1).coerceAtMost(MAX_COSMETIC_TIER)
        val updated = current.copy(cosmeticTier = nextTier)
        save(context, updated)
        return updated
    }

    fun setCosmeticTier(context: Context, tier: Int): UnlockState {
        val current = load(context)
        val updated = current.copy(cosmeticTier = tier.coerceIn(0, MAX_COSMETIC_TIER))
        save(context, updated)
        return updated
    }

    fun unlockTheme(context: Context, themeName: String): UnlockState {
        val current = load(context)
        val updatedThemes = current.unlockedThemes + themeName
        val updated = current.copy(unlockedThemes = updatedThemes)
        save(context, updated)
        return updated
    }

    fun unlockRandomTheme(context: Context, exclude: Set<String> = emptySet()): LevelTheme? {
        val current = load(context)
        val locked = LevelThemes.bonusThemes()
            .filter { it.name !in current.unlockedThemes && it.name !in exclude }
        if (locked.isEmpty()) return null
        val chosen = locked.random()
        unlockTheme(context, chosen.name)
        return chosen
    }

    fun resolveThemePool(state: UnlockState): List<LevelTheme> {
        val base = LevelThemes.baseThemes()
        val extras = LevelThemes.bonusThemes().filter { it.name in state.unlockedThemes }
        return base + extras
    }
}
