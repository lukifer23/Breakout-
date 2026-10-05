package com.breakoutplus.game

import android.view.MotionEvent

object AndroidInputAdapter {
    fun translate(event: MotionEvent, width: Int, height: Int): GameInput? {
        if (width <= 0 || height <= 0 || event.pointerCount == 0) return null
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> PointerAction.DOWN
            MotionEvent.ACTION_MOVE -> PointerAction.MOVE
            MotionEvent.ACTION_POINTER_DOWN -> PointerAction.POINTER_DOWN
            MotionEvent.ACTION_POINTER_UP -> PointerAction.POINTER_UP
            MotionEvent.ACTION_UP -> PointerAction.UP
            MotionEvent.ACTION_CANCEL -> PointerAction.CANCEL
            else -> return null
        }
        val pointers = (0 until event.pointerCount).map { i ->
            GamePointer(event.getPointerId(i), event.getX(i) / width, event.getY(i) / height, event.getPressure(i))
        }
        return GameInput.Pointer(PointerInput(action, event.actionIndex, pointers, event.eventTime))
    }
}
