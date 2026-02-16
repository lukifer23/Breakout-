package com.breakoutplus.game

/**
 * Tracks latest valid viewport dimensions so renderer lifecycle actions
 * (engine restart/reset) can reapply the same world sizing consistently.
 */
internal class ViewportState {
    private var widthPx: Int = 0
    private var heightPx: Int = 0

    fun update(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        widthPx = width
        heightPx = height
    }

    fun hasViewport(): Boolean = widthPx > 0 && heightPx > 0

    fun reapply(onResize: (width: Int, height: Int) -> Unit) {
        if (!hasViewport()) return
        onResize(widthPx, heightPx)
    }
}

