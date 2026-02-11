package com.breakoutplus

import android.content.Context

object ProgressionManager {
    private const val PREFS_NAME = "breakout_plus_progress"
    private const val KEY_XP = "xp_total"
    private const val KEY_BEST_LEVEL = "best_level"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadXp(context: Context): Int =
        prefs(context).getInt(KEY_XP, 0)

    fun addXp(context: Context, amount: Int): Int {
        val safeAmount = amount.coerceAtLeast(0)
        val total = loadXp(context) + safeAmount
        prefs(context).edit().putInt(KEY_XP, total).apply()

        // Check for theme unlocks based on XP thresholds
        checkXpUnlocks(context, total)

        return total
    }

    private fun checkXpUnlocks(context: Context, totalXp: Int) {
        val unlocks = UnlockManager.load(context)

        // Theme unlock thresholds (cumulative XP required)
        val themeThresholds = mapOf(
            "Sunset" to 500,
            "Cobalt" to 1000,
            "Aurora" to 2000,
            "Neon" to 3500,
            "Circuit" to 5000,
            "Vapor" to 7500
        )

        for ((themeName, threshold) in themeThresholds) {
            if (totalXp >= threshold && !unlocks.unlockedThemes.contains(themeName)) {
                UnlockManager.unlockTheme(context, themeName)
            }
        }

        // Cosmetic tier unlock thresholds
        val cosmeticThresholds = mapOf(
            1 to 2500,  // Tier 1 at 2500 XP
            2 to 5000,  // Tier 2 at 5000 XP
            3 to 10000  // Tier 3 at 10000 XP
        )

        for ((tier, threshold) in cosmeticThresholds) {
            if (totalXp >= threshold && unlocks.cosmeticTier < tier) {
                UnlockManager.setCosmeticTier(context, tier)
            }
        }
    }

    fun loadBestLevel(context: Context): Int =
        prefs(context).getInt(KEY_BEST_LEVEL, 1).coerceAtLeast(1)

    fun updateBestLevel(context: Context, level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        val current = loadBestLevel(context)
        if (safeLevel > current) {
            prefs(context).edit().putInt(KEY_BEST_LEVEL, safeLevel).apply()
            return safeLevel
        }
        return current
    }

    fun xpForLevel(level: Int): Int {
        val base = 10
        val bonus = (level.coerceAtLeast(1) - 1) * 2
        return base + bonus
    }

    fun chapterForLevel(level: Int): Int =
        ((level.coerceAtLeast(1) - 1) / 10) + 1

    fun stageForLevel(level: Int): Int =
        ((level.coerceAtLeast(1) - 1) % 10) + 1
}
