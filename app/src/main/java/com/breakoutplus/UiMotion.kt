package com.breakoutplus

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

object UiMotion {
    val EMPHASIS_OUT: Interpolator = DecelerateInterpolator(1.28f)
    val EMPHASIS_IN_OUT: Interpolator = AccelerateDecelerateInterpolator()
    val LINEAR: Interpolator = LinearInterpolator()

    const val TITLE_DURATION = 460L
    const val SUBTITLE_DURATION = 420L
    const val ENTRY_DURATION = 340L
    const val LIST_ITEM_DURATION = 280L

    const val OVERLAY_IN_DURATION = 210L
    const val OVERLAY_OUT_DURATION = 180L
    const val OVERLAY_ENTER_SCALE = 0.965f
    const val OVERLAY_EXIT_SCALE = 0.985f

    const val BANNER_IN_DURATION = 180L
    const val BANNER_OUT_DURATION = 260L
    const val BANNER_HOLD_DURATION = 820L
    const val BANNER_ENTER_SCALE = 0.92f
    const val BANNER_EXIT_SCALE = 1.04f

    const val PULSE_IN_DURATION = 140L
    const val PULSE_OUT_DURATION = 170L
    const val HUD_PULSE_SCALE = 1.05f
    const val SHIELD_PULSE_SCALE = 1.08f

    const val SCORE_COUNT_MIN_DURATION = 420L
    const val SCORE_COUNT_MAX_DURATION = 980L

    fun stagger(index: Int, base: Long = 0L, step: Long = 70L): Long = base + index * step

    fun scoreCountDuration(score: Int): Long {
        val normalized = (score.coerceAtLeast(0).toFloat() / 12_000f).coerceIn(0f, 1f)
        return (SCORE_COUNT_MIN_DURATION + (SCORE_COUNT_MAX_DURATION - SCORE_COUNT_MIN_DURATION) * normalized).toLong()
    }
}
