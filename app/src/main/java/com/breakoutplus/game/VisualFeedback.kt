package com.breakoutplus.game

import com.breakoutplus.UiMotion
import kotlin.math.max

class VisualFlashEffect(
    private val duration: Float,
    private val maxAlpha: Float,
    private val interpolator: android.view.animation.Interpolator = UiMotion.EMPHASIS_OUT
) {
    var remainingTime: Float = 0f
        private set

    fun trigger(intensity: Float = 1f) {
        val clamped = intensity.coerceIn(0f, 1f)
        remainingTime = max(remainingTime, duration * clamped)
    }

    fun update(delta: Float) {
        if (remainingTime > 0f) {
            remainingTime = (remainingTime - delta).coerceAtLeast(0f)
        }
    }

    fun getAlpha(): Float {
        if (remainingTime <= 0f) return 0f
        val rawProgress = 1f - (remainingTime / duration)
        val easedProgress = interpolator.getInterpolation(rawProgress)
        return (maxAlpha * (1f - easedProgress)).coerceIn(0f, maxAlpha)
    }
}
