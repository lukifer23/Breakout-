package com.breakoutplus.game

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Renderer2D {
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val rectMesh = RectMesh()
    private val circleMesh = CircleMesh(28)

    private var offsetX = 0f
    private var offsetY = 0f
    private var worldWidth = 100f
    private var worldHeight = 160f
    private var shaderBound = false

    fun init() {
        shader.build()
        rectMesh.build()
        circleMesh.build()
        shaderBound = false
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun setViewport(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    fun setWorldSize(width: Float, height: Float) {
        worldWidth = width
        worldHeight = height
        Matrix.orthoM(projectionMatrix, 0, 0f, width, 0f, height, -1f, 1f)
    }

    fun setOffset(x: Float, y: Float) {
        offsetX = x
        offsetY = y
    }

    // Simple frustum culling - check if a circle is visible on screen
    fun isCircleVisible(x: Float, y: Float, radius: Float): Boolean {
        val left = x - radius
        val right = x + radius
        val bottom = y - radius
        val top = y + radius
        return right >= 0f && left <= worldWidth && top >= 0f && bottom <= worldHeight
    }

    // Simple frustum culling - check if a rect is visible on screen
    fun isRectVisible(x: Float, y: Float, width: Float, height: Float): Boolean {
        val right = x + width
        val top = y + height
        return right >= 0f && x <= worldWidth && top >= 0f && y <= worldHeight
    }

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        ensureShader()
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x + offsetX, y + offsetY, 0f)
        Matrix.scaleM(modelMatrix, 0, width, height, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
        shader.setUniformMatrix("u_MVPMatrix", mvpMatrix)
        shader.setUniformColor("u_Color", color)
        rectMesh.draw(shader)
    }

    fun drawCircle(x: Float, y: Float, radius: Float, color: FloatArray) {
        ensureShader()
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x + offsetX, y + offsetY, 0f)
        Matrix.scaleM(modelMatrix, 0, radius, radius, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
        shader.setUniformMatrix("u_MVPMatrix", mvpMatrix)
        shader.setUniformColor("u_Color", color)
        circleMesh.draw(shader)
    }

    // Batched circle drawing to reduce shader switches and matrix calculations
    private val circleBatch = ArrayList<CircleDraw>(300) // Pre-allocated for up to 300 circles
    private data class CircleDraw(val x: Float, val y: Float, val radius: Float, val color: FloatArray)

    fun drawCircleBatch(x: Float, y: Float, radius: Float, color: FloatArray) {
        circleBatch.add(CircleDraw(x, y, radius, color))
    }

    fun flushCircleBatch() {
        if (circleBatch.isEmpty()) return

        ensureShader()
        circleBatch.forEach { circle ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, circle.x + offsetX, circle.y + offsetY, 0f)
            Matrix.scaleM(modelMatrix, 0, circle.radius, circle.radius, 1f)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
            shader.setUniformMatrix("u_MVPMatrix", mvpMatrix)
            shader.setUniformColor("u_Color", circle.color)
            circleMesh.draw(shader)
        }
        circleBatch.clear()
    }

    private fun ensureShader() {
        if (shaderBound) return
        shader.use()
        shaderBound = true
    }

    fun drawBeam(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        drawRect(x - width / 2f, y - height / 2f, width, height, color)
    }

    private class RectMesh {
        private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(floatArrayOf(
                    0f, 0f,
                    1f, 0f,
                    0f, 1f,
                    1f, 1f
                ))
                position(0)
            }

        fun build() = Unit

        fun draw(shader: ShaderProgram) {
            val handle = shader.getAttributeLocation("a_Position")
            GLES20.glEnableVertexAttribArray(handle)
            GLES20.glVertexAttribPointer(handle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(handle)
        }
    }

    private class CircleMesh(private val segments: Int) {
        private lateinit var vertexBuffer: FloatBuffer
        private var vertexCount = 0

        fun build() {
            val vertices = ArrayList<Float>()
            vertices.add(0f)
            vertices.add(0f)
            for (i in 0..segments) {
                val angle = (Math.PI * 2.0 * i / segments).toFloat()
                vertices.add(kotlin.math.cos(angle))
                vertices.add(kotlin.math.sin(angle))
            }
            vertexCount = vertices.size / 2
            vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertices.toFloatArray())
            vertexBuffer.position(0)
        }

        fun draw(shader: ShaderProgram) {
            val handle = shader.getAttributeLocation("a_Position")
            GLES20.glEnableVertexAttribArray(handle)
            GLES20.glVertexAttribPointer(handle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(handle)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVPMatrix;
            attribute vec2 a_Position;
            void main() {
                gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """
    }
}

class ShaderProgram(private val vertexSrc: String, private val fragmentSrc: String) {
    private var programId = 0
    private val attributeCache = mutableMapOf<String, Int>()
    private val uniformCache = mutableMapOf<String, Int>()

    fun build() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    fun getAttributeLocation(name: String): Int = attributeCache.getOrPut(name) {
        GLES20.glGetAttribLocation(programId, name)
    }

    fun setUniformMatrix(name: String, matrix: FloatArray) {
        val handle = uniformCache.getOrPut(name) { GLES20.glGetUniformLocation(programId, name) }
        GLES20.glUniformMatrix4fv(handle, 1, false, matrix, 0)
    }

    fun setUniformColor(name: String, color: FloatArray) {
        val handle = uniformCache.getOrPut(name) { GLES20.glGetUniformLocation(programId, name) }
        GLES20.glUniform4fv(handle, 1, color, 0)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
