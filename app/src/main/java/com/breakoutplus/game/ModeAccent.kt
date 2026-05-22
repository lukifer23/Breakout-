package com.breakoutplus.game

import com.breakoutplus.R

/**
 * Canonical mode accent colors for UI surfaces (mode select, scoreboard, etc.).
 * Keep in sync with [Docs/PARITY.md] and [Docs/DESIGN.md].
 */
object ModeAccent {
    fun colorRes(mode: GameMode): Int = when (mode) {
        GameMode.CLASSIC -> R.color.bp_cyan
        GameMode.TIMED -> R.color.bp_gold
        GameMode.ENDLESS -> R.color.bp_green
        GameMode.GOD -> R.color.bp_magenta
        GameMode.RUSH -> R.color.bp_red
        GameMode.VOLLEY -> R.color.bp_azure
        GameMode.TUNNEL -> R.color.bp_orange
        GameMode.SURVIVAL -> R.color.bp_flame
        GameMode.INVADERS -> R.color.bp_violet
        GameMode.ZEN -> R.color.bp_gray
    }
}
