package com.breakoutplus.game

import android.opengl.GLES20
import android.opengl.Matrix

class Renderer2D {
    private val projectionMatrix = FloatArray(16)
    private val shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val vertices = PrimitiveVertices()
    private var offsetX = 0f
    private var offsetY = 0f
    private var worldWidth = 100f
    private var worldHeight = 160f
    private var projectionReady = false
    private val vertexBufferId = IntArray(1)
    var drawCalls = 0
        private set
    var primitiveCount = 0
        private set
    var vertexCount = 0
        private set

    fun beginFrame() {
        check(vertices.count == 0) { "Previous frame was not flushed" }
        drawCalls = 0; primitiveCount = 0; vertexCount = 0
    }
    fun endFrame() = flush()
    fun init() {
        // onSurfaceCreated establishes a new context; cached locations rebuild with it.
        shader.build(); vertices.clear(); projectionReady = false
        if (vertexBufferId[0] != 0 && GLES20.glIsBuffer(vertexBufferId[0])) GLES20.glDeleteBuffers(1, vertexBufferId, 0)
        GLES20.glGenBuffers(1, vertexBufferId, 0)
        check(vertexBufferId[0] != 0) { "GLES vertex buffer allocation failed" }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.capacity * PrimitiveVertices.STRIDE_BYTES, null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
    fun setViewport(width: Int, height: Int) { GLES20.glViewport(0, 0, width, height) }
    fun setWorldSize(width: Float, height: Float) {
        if (projectionReady && worldWidth == width && worldHeight == height) return
        flush()
        worldWidth = width; worldHeight = height
        Matrix.orthoM(projectionMatrix, 0, 0f, width, 0f, height, -1f, 1f)
        projectionReady = true
    }
    fun setOffset(x: Float, y: Float) { offsetX = x; offsetY = y }
    fun isCircleVisible(x: Float, y: Float, radius: Float): Boolean =
        x + radius >= 0f && x - radius <= worldWidth && y + radius >= 0f && y - radius <= worldHeight
    fun isRectVisible(x: Float, y: Float, width: Float, height: Float): Boolean =
        x + width >= 0f && x <= worldWidth && y + height >= 0f && y <= worldHeight

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        if (!vertices.hasRoom(6)) flush()
        vertices.rect(x + offsetX, y + offsetY, width, height, color)
        primitiveCount++; vertexCount += 6
    }
    fun drawCircle(x: Float, y: Float, radius: Float, color: FloatArray) {
        if (!vertices.hasRoom(PrimitiveVertices.CIRCLE_VERTICES)) flush()
        vertices.circle(x + offsetX, y + offsetY, radius, color)
        primitiveCount++; vertexCount += PrimitiveVertices.CIRCLE_VERTICES
    }
    fun drawRectBatch(x: Float, y: Float, width: Float, height: Float, color: FloatArray) =
        drawRect(x, y, width, height, color)
    fun drawCircleBatch(x: Float, y: Float, radius: Float, color: FloatArray) = drawCircle(x, y, radius, color)
    fun flushRectBatch() = flush()
    fun flushCircleBatch() = flush()
    fun drawBeam(x: Float, y: Float, width: Float, height: Float, color: FloatArray) =
        drawRect(x - width / 2f, y - height / 2f, width, height, color)

    private fun flush() {
        if (vertices.count == 0) return
        shader.use()
        shader.setUniformMatrix("u_MVPMatrix", projectionMatrix)
        val position = shader.getAttributeLocation("a_Position")
        val color = shader.getAttributeLocation("a_Color")
        GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(color)
        vertices.prepare()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId[0])
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertices.count * PrimitiveVertices.STRIDE_BYTES, vertices.buffer)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, PrimitiveVertices.STRIDE_BYTES, 0)
        GLES20.glVertexAttribPointer(color, 4, GLES20.GL_FLOAT, false, PrimitiveVertices.STRIDE_BYTES, 8)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.count)
        GLES20.glDisableVertexAttribArray(position); GLES20.glDisableVertexAttribArray(color)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        drawCalls++
        vertices.clear()
    }
    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVPMatrix;
            attribute vec2 a_Position;
            attribute vec4 a_Color;
            varying mediump vec4 v_Color;
            void main() {
                gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);
                v_Color = a_Color;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying mediump vec4 v_Color;
            void main() { gl_FragColor = v_Color; }
        """
    }
}

class ShaderProgram(private val vertexSrc: String, private val fragmentSrc: String) {
    private var programId = 0
    private val attributeCache = mutableMapOf<String, Int>()
    private val uniformCache = mutableMapOf<String, Int>()

    fun build() {
        attributeCache.clear(); uniformCache.clear()
        if (programId != 0 && GLES20.glIsProgram(programId)) GLES20.glDeleteProgram(programId)
        programId = 0
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        var fragmentShader = 0
        var candidate = 0
        try {
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
            candidate = GLES20.glCreateProgram()
            check(candidate != 0) { "GLES program allocation failed" }
            GLES20.glAttachShader(candidate, vertexShader)
            GLES20.glAttachShader(candidate, fragmentShader)
            GLES20.glLinkProgram(candidate)
            val status = IntArray(1)
            GLES20.glGetProgramiv(candidate, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] != 0) { "GLES program link failed: ${GLES20.glGetProgramInfoLog(candidate)}" }
            programId = candidate
        } finally {
            GLES20.glDeleteShader(vertexShader)
            if (fragmentShader != 0) GLES20.glDeleteShader(fragmentShader)
            if (candidate != 0 && candidate != programId) GLES20.glDeleteProgram(candidate)
        }
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
        check(shader != 0) { "GLES shader allocation failed ($type)" }
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("GLES shader compile failed ($type): $log")
        }
        return shader
    }
}
