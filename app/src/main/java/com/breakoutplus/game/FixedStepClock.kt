package com.breakoutplus.game

/** Display cadence changes rendering frequency, never the simulation tick. */
class FixedStepClock {
    private var accumulator = 0.0
    var droppedSeconds = 0.0
        private set
    fun advance(frameSeconds: Float): Int {
        if (!frameSeconds.isFinite() || frameSeconds <= 0f) return 0
        val accepted = frameSeconds.toDouble().coerceAtMost(MAX_FRAME_SECONDS)
        droppedSeconds += frameSeconds.toDouble() - accepted
        accumulator += accepted
        val ticks = ((accumulator + 1e-7) / STEP_SECONDS).toInt().coerceAtMost(MAX_TICKS)
        accumulator = (accumulator - ticks * STEP_SECONDS).coerceAtLeast(0.0)
        return ticks
    }
    fun reset() { accumulator = 0.0 }
    companion object {
        const val STEP_SECONDS = 1.0 / 120.0
        const val STEP = 1f / 120f
        const val MAX_FRAME_SECONDS = 0.1
        const val MAX_TICKS = 12
    }
}
