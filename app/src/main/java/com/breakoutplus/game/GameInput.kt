package com.breakoutplus.game

enum class PointerAction { DOWN, MOVE, POINTER_DOWN, POINTER_UP, UP, CANCEL }
data class GamePointer(val id: Int, val x: Float, val y: Float, val pressure: Float)
/** Coordinates normalized to the surface; no recycled native event crosses threads. */
data class PointerInput(val actionMasked: PointerAction, val actionIndex: Int,
    val pointers: List<GamePointer>, val eventTime: Long) {
    val pointerCount get() = pointers.size
    fun findPointerIndex(id: Int) = pointers.indexOfFirst { it.id == id }
    fun getPointerId(index: Int) = pointers[index].id
    fun getX(index: Int) = pointers[index].x
    fun getY(index: Int) = pointers[index].y
    fun getPressure(index: Int) = pointers[index].pressure
    companion object { const val INVALID_POINTER_ID = -1 }
}
sealed interface GameInput {
    data class Pointer(val event: PointerInput) : GameInput
    data object FireLaser : GameInput
}
