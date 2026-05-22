package com.breakoutplus

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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

    const val ACTIVITY_SLIDE_DURATION = 300L
    const val ACTIVITY_FADE_IN_DURATION = 340L
    const val ACTIVITY_FADE_OUT_DURATION = 180L
    const val ACTIVITY_FADE_ENTER_SCALE = 0.965f
    const val ACTIVITY_FADE_EXIT_SCALE = 0.985f

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
    const val HUD_DISABLED_ALPHA = 0.6f
    const val LABEL_FLASH_DURATION = 260L
    const val HUD_TICK_INTERVAL = 60L

    const val TITLE_ENTRY_OFFSET_Y = -20f
    const val BUTTON_ENTRY_OFFSET_Y = 20f
    const val HEADER_ENTRY_OFFSET_Y = 16f
    const val LIST_ITEM_OFFSET_Y = 14f
    const val CARD_ENTRY_OFFSET_Y = 30f
    const val LIST_ENTER_SCALE = 0.98f
    const val ROW_ENTER_SCALE = 0.985f

    const val STAGGER_STEP_HEADER = 60L
    const val STAGGER_STEP_LIST = 54L
    const val STAGGER_STEP_CARD = 70L
    const val STAGGER_BASE_CARD = 90L
    const val STAGGER_STEP_SCREEN = 80L
    const val STAGGER_STEP_SCORE_ROW = 42L
    const val STAGGER_STEP_CHALLENGE_ROW = 52L

    const val OVERLAY_CHILD_OFFSET_Y = 12f
    const val OVERLAY_CHILD_STAGGER = 48L
    const val EXPAND_COLLAPSE_OFFSET_Y = 6f

    const val MAIN_BUTTON_STAGGER_BASE = 180L
    const val MAIN_BUTTON_STAGGER_STEP = 78L
    const val MAIN_TITLE_STAGGER_BASE = 90L

    const val SCORE_COUNT_MIN_DURATION = 420L
    const val SCORE_COUNT_MAX_DURATION = 980L

    fun stagger(index: Int, base: Long = 0L, step: Long = 70L): Long = base + index * step

    fun scoreCountDuration(score: Int): Long {
        val normalized = (score.coerceAtLeast(0).toFloat() / 12_000f).coerceIn(0f, 1f)
        return (SCORE_COUNT_MIN_DURATION + (SCORE_COUNT_MAX_DURATION - SCORE_COUNT_MIN_DURATION) * normalized).toLong()
    }

    fun animateFadeUp(
        view: View,
        index: Int = 0,
        offsetY: Float = HEADER_ENTRY_OFFSET_Y,
        step: Long = STAGGER_STEP_HEADER,
        base: Long = 0L,
        duration: Long = ENTRY_DURATION,
        interpolator: Interpolator = EMPHASIS_OUT
    ) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = offsetY
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(stagger(index, base = base, step = step))
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()
    }

    fun animateListItem(
        view: View,
        index: Int,
        offsetY: Float = LIST_ITEM_OFFSET_Y,
        enterScale: Float = LIST_ENTER_SCALE,
        step: Long = STAGGER_STEP_LIST,
        base: Long = 0L,
        duration: Long = LIST_ITEM_DURATION
    ) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = offsetY
        view.scaleX = enterScale
        view.scaleY = enterScale
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setStartDelay(stagger(index, base = base, step = step))
            .setInterpolator(EMPHASIS_OUT)
            .start()
    }

    fun animateOverlayIn(
        view: View,
        token: Int? = null,
        isCurrent: ((Int) -> Boolean)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        view.animate().cancel()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = OVERLAY_ENTER_SCALE
        view.scaleY = OVERLAY_ENTER_SCALE
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(OVERLAY_IN_DURATION)
            .setInterpolator(EMPHASIS_OUT)
            .withEndAction {
                if (token != null && isCurrent != null && !isCurrent(token)) return@withEndAction
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
                onEnd?.invoke()
            }
            .start()
    }

    fun animateOverlayOut(
        view: View,
        token: Int? = null,
        isCurrent: ((Int) -> Boolean)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        if (view.visibility != View.VISIBLE) return
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .scaleX(OVERLAY_EXIT_SCALE)
            .scaleY(OVERLAY_EXIT_SCALE)
            .setDuration(OVERLAY_OUT_DURATION)
            .setInterpolator(EMPHASIS_OUT)
            .withEndAction {
                if (token != null && isCurrent != null && !isCurrent(token)) return@withEndAction
                view.visibility = View.GONE
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
                onEnd?.invoke()
            }
            .start()
    }

    fun staggerOverlayChildren(container: ViewGroup, skipFirst: Int = 0) {
        val childCount = container.childCount
        for (i in skipFirst until childCount) {
            val child = container.getChildAt(i)
            child.animate().cancel()
            child.alpha = 0f
            child.translationY = OVERLAY_CHILD_OFFSET_Y
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(stagger(i - skipFirst, step = OVERLAY_CHILD_STAGGER))
                .setDuration(LIST_ITEM_DURATION)
                .setInterpolator(EMPHASIS_OUT)
                .start()
        }
    }

    fun animateScreenSections(
        views: List<View>,
        offsetY: Float = HEADER_ENTRY_OFFSET_Y,
        step: Long = STAGGER_STEP_SCREEN
    ) {
        views.forEachIndexed { index, view ->
            animateFadeUp(view, index = index, offsetY = offsetY, step = step, base = 0L)
        }
    }

    fun animateStaggerChildren(
        container: ViewGroup,
        offsetY: Float = LIST_ITEM_OFFSET_Y,
        enterScale: Float = ROW_ENTER_SCALE,
        step: Long = STAGGER_STEP_LIST
    ) {
        for (i in 0 until container.childCount) {
            animateListItem(container.getChildAt(i), i, offsetY = offsetY, enterScale = enterScale, step = step)
        }
    }

    fun animateExpandableSection(content: View, expand: Boolean, onEnd: (() -> Unit)? = null) {
        content.animate().cancel()
        if (expand) {
            content.visibility = View.VISIBLE
            content.alpha = 0f
            content.translationY = -EXPAND_COLLAPSE_OFFSET_Y
            content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(OVERLAY_IN_DURATION)
                .setInterpolator(EMPHASIS_OUT)
                .withEndAction { onEnd?.invoke() }
                .start()
        } else {
            content.animate()
                .alpha(0f)
                .translationY(-EXPAND_COLLAPSE_OFFSET_Y)
                .setDuration(OVERLAY_OUT_DURATION)
                .setInterpolator(EMPHASIS_OUT)
                .withEndAction {
                    content.visibility = View.GONE
                    content.alpha = 1f
                    content.translationY = 0f
                    onEnd?.invoke()
                }
                .start()
        }
    }

    const val PRESS_SCALE = 0.97f
    const val PRESS_IN_DURATION = 80L
    const val PRESS_OUT_DURATION = 120L

    fun attachPressScale(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate()
                        .scaleX(PRESS_SCALE)
                        .scaleY(PRESS_SCALE)
                        .setDuration(PRESS_IN_DURATION)
                        .start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(PRESS_OUT_DURATION)
                        .setInterpolator(EMPHASIS_OUT)
                        .start()
            }
            false
        }
    }

    fun pulseView(view: View, scale: Float, token: Int, isCurrent: (Int) -> Boolean) {
        view.animate().cancel()
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(PULSE_IN_DURATION)
            .setInterpolator(EMPHASIS_OUT)
            .withEndAction {
                if (!isCurrent(token)) return@withEndAction
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(PULSE_OUT_DURATION)
                    .setInterpolator(EMPHASIS_OUT)
                    .withEndAction {
                        if (!isCurrent(token)) return@withEndAction
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                    .start()
            }
            .start()
    }
}
