package com.breakoutplus.game

object BrickCollisionFeedback {

    fun comboMultiplier(combo: Int): Float {
        return when {
            combo >= 10 -> 5f
            combo >= 7 -> 3f
            combo >= 4 -> 2f
            combo >= 2 -> 1.5f
            else -> 1f
        }
    }

    fun dynamicBrickSoundRate(baseRate: Float, combo: Int, randomUnit: Float): Float {
        val comboPitchBoost = (combo.coerceAtMost(10) * 0.02f).coerceAtMost(0.2f)
        val randomVariation = (randomUnit - 0.5f) * 0.3f
        return (baseRate + comboPitchBoost + randomVariation).coerceIn(0.7f, 1.3f)
    }

    fun shouldTriggerComboFlash(multiplier: Float): Boolean = multiplier >= 2f
}
