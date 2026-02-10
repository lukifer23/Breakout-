package com.breakoutplus

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator

object UiMotion {
    val EMPHASIS_OUT: Interpolator = DecelerateInterpolator(1.28f)
    val EMPHASIS_IN_OUT: Interpolator = AccelerateDecelerateInterpolator()

    const val TITLE_DURATION = 460L
    const val SUBTITLE_DURATION = 420L
    const val ENTRY_DURATION = 340L
    const val LIST_ITEM_DURATION = 280L

    const val OVERLAY_IN_DURATION = 210L
    const val OVERLAY_OUT_DURATION = 180L

    const val BANNER_IN_DURATION = 180L
    const val BANNER_OUT_DURATION = 260L
    const val BANNER_HOLD_DURATION = 820L

    const val PULSE_IN_DURATION = 140L
    const val PULSE_OUT_DURATION = 170L

    fun stagger(index: Int, base: Long = 0L, step: Long = 70L): Long = base + index * step
}
