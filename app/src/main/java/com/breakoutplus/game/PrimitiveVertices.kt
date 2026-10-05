package com.breakoutplus.game

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/** Reused position/color stream. Triangle topology matches the previous strip/fan. */
class PrimitiveVertices(val capacity: Int = 16380) {
    val buffer: FloatBuffer = ByteBuffer.allocateDirect(capacity * STRIDE_BYTES)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val data = FloatArray(capacity * 6)
    var count = 0
        private set
    private val rim = FloatArray((SEGMENTS + 1) * 2).apply {
        for (i in 0..SEGMENTS) {
            val angle = (Math.PI * 2.0 * i / SEGMENTS).toFloat()
            this[i * 2] = cos(angle); this[i * 2 + 1] = sin(angle)
        }
    }
    fun hasRoom(vertices: Int) = count + vertices <= capacity
    fun clear() { count = 0; buffer.clear() }
    fun prepare() { buffer.clear(); buffer.put(data, 0, count * 6); buffer.position(0) }
    private fun vertex(x: Float, y: Float, color: FloatArray) {
        val base = count * 6
        data[base] = x; data[base + 1] = y
        for (i in 0..3) data[base + 2 + i] = if (i < color.size) color[i] else 1f
        count++
    }
    fun rect(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        require(hasRoom(6))
        vertex(x, y, color); vertex(x + width, y, color); vertex(x, y + height, color)
        vertex(x, y + height, color); vertex(x + width, y, color); vertex(x + width, y + height, color)
    }
    fun circle(x: Float, y: Float, radius: Float, color: FloatArray) {
        require(hasRoom(CIRCLE_VERTICES))
        for (i in 0 until SEGMENTS) {
            vertex(x, y, color)
            vertex(x + rim[i * 2] * radius, y + rim[i * 2 + 1] * radius, color)
            vertex(x + rim[(i + 1) * 2] * radius, y + rim[(i + 1) * 2 + 1] * radius, color)
        }
    }
    companion object {
        const val STRIDE_BYTES = 6 * 4
        const val SEGMENTS = 28
        const val CIRCLE_VERTICES = SEGMENTS * 3
    }
}
