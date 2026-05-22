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

    // Batched rect drawing to reduce draw-call overhead for solid-color quads.
    private val rectBatch = ArrayList<RectDraw>(512)
    private var rectBatchCount = 0

    private class RectDraw {
        var x: Float = 0f
        var y: Float = 0f
        var width: Float = 0f
        var height: Float = 0f
        val color: FloatArray = FloatArray(4)
    }

    fun drawRectBatch(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        if (rectBatchCount >= rectBatch.size) {
            rectBatch.add(RectDraw())
        }
        val rect = rectBatch[rectBatchCount]
        rect.x = x
        rect.y = y
        rect.width = width
        rect.height = height
        rect.color[0] = color.getOrElse(0) { 1f }
        rect.color[1] = color.getOrElse(1) { 1f }
        rect.color[2] = color.getOrElse(2) { 1f }
        rect.color[3] = color.getOrElse(3) { 1f }
        rectBatchCount += 1
    }

    fun flushRectBatch() {
        if (rectBatchCount == 0) return
        ensureShader()
        for (i in 0 until rectBatchCount) {
            val rect = rectBatch[i]
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, rect.x + offsetX, rect.y + offsetY, 0f)
            Matrix.scaleM(modelMatrix, 0, rect.width, rect.height, 1f)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
            shader.setUniformMatrix("u_MVPMatrix", mvpMatrix)
            shader.setUniformColor("u_Color", rect.color)
            rectMesh.draw(shader)
        }
        rectBatchCount = 0
    }

    // Batched circle drawing to reduce shader switches and matrix calculations.
    // Reuses slots to avoid per-frame object churn from transient particle effects.
    private val circleBatch = ArrayList<CircleDraw>(300)
    private var circleBatchCount = 0

    private class CircleDraw {
        var x: Float = 0f
        var y: Float = 0f
        var radius: Float = 0f
        val color: FloatArray = FloatArray(4)
    }

    fun drawCircleBatch(x: Float, y: Float, radius: Float, color: FloatArray) {
        if (circleBatchCount >= circleBatch.size) {
            circleBatch.add(CircleDraw())
        }
        val circle = circleBatch[circleBatchCount]
        circle.x = x
        circle.y = y
        circle.radius = radius
        circle.color[0] = color.getOrElse(0) { 1f }
        circle.color[1] = color.getOrElse(1) { 1f }
        circle.color[2] = color.getOrElse(2) { 1f }
        circle.color[3] = color.getOrElse(3) { 1f }
        circleBatchCount += 1
    }

    fun flushCircleBatch() {
        if (circleBatchCount == 0) return

        ensureShader()
        for (i in 0 until circleBatchCount) {
            val circle = circleBatch[i]
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, circle.x + offsetX, circle.y + offsetY, 0f)
            Matrix.scaleM(modelMatrix, 0, circle.radius, circle.radius, 1f)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
            shader.setUniformMatrix("u_MVPMatrix", mvpMatrix)
            shader.setUniformColor("u_Color", circle.color)
            circleMesh.draw(shader)
        }
        circleBatchCount = 0
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
