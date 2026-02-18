package com.breakoutplus.game

import java.util.Locale

/**
 * Centralized status text formatting used by mode-specific HUD summaries.
 * Keeping these strings out of GameEngine reduces orchestration complexity.
 */
object ModeStatusText {
    data class EffectStatus(
        val type: PowerUpType,
        val remainingSeconds: Int,
        val charges: Int = 0
    )

    fun volley(
        volleyBallCount: Int,
        turnNumber: Int,
        depthRows: Int,
        aliveBreakables: Int,
        laneClearance: Float,
        locale: Locale = Locale.getDefault()
    ): String {
        return "Volley balls: $volleyBallCount • Turn $turnNumber"
            .plus(" • Depth $depthRows")
            .plus(" • Bricks $aliveBreakables • Lane ${String.format(locale, "%.1f", laneClearance)}")
    }

    fun tunnel(
        shotsFired: Int,
        gateIntegrityPercent: Int,
        breachPercent: Int,
        combo: Int
    ): String {
        val segments = mutableListOf<String>()
        segments.add("Shots: $shotsFired")
        segments.add("Gate ${gateIntegrityPercent.coerceIn(0, 100)}%")
        segments.add("Breach ${breachPercent.coerceIn(0, 100)}%")
        if (combo >= 2) {
            segments.add("Combo x$combo")
        }
        return segments.joinToString(" • ")
    }

    fun survival(speedMultiplier: Float, combo: Int): String {
        val segments = mutableListOf<String>()
        segments.add("Speed: ${String.format(Locale.US, "%.1f", speedMultiplier)}x")
        if (combo >= 2) {
            segments.add("Combo x$combo")
        }
        return segments.joinToString(" • ")
    }

    fun powerups(effects: List<EffectStatus>): String {
        if (effects.isEmpty()) return "Powerups: none"
        return effects.joinToString(" • ", prefix = "Powerups: ") { effect ->
            if (effect.type == PowerUpType.SHIELD) {
                "${effect.type.displayName} x${effect.charges} ${effect.remainingSeconds}s"
            } else {
                "${effect.type.displayName} ${effect.remainingSeconds}s"
            }
        }
    }
}
