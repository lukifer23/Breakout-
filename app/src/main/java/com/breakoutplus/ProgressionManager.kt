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
        return total
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
