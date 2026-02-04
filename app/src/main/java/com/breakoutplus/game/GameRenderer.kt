package com.breakoutplus.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class GameRenderer(
    private val context: Context,
    private var config: GameConfig,
    private val listener: GameEventListener
) : GLSurfaceView.Renderer {

    private val renderer2D = Renderer2D()
    private val audioManager = GameAudioManager(context, config.settings)
    private val logger = if (config.settings.loggingEnabled) GameLogger(context, true) else null
    private var engine = GameEngine(config, listener, audioManager, logger, null, this)
    private var lastTimeNs: Long = 0L
    private var paused = false
    private val random = java.util.Random()

    // Enhanced visual effects
    private var screenShake = 0f
    private var shakeIntensity = 0f
    private var comboFlash = 0f

    fun triggerScreenShake(intensity: Float = 3f, duration: Float = 0.2f) {
        shakeIntensity = intensity
        screenShake = duration
    }

    fun triggerComboFlash() {
        comboFlash = 0.5f
    }

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.07f, 0.13f, 1f)
        renderer2D.init()
        audioManager.startMusic()
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        renderer2D.setViewport(width, height)
        engine.onResize(width, height)
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val frameStart = System.nanoTime()
        val now = frameStart
        if (lastTimeNs == 0L) {
            lastTimeNs = now
        }
        var delta = (now - lastTimeNs) / 1_000_000_000f
        lastTimeNs = now
        if (delta > 0.05f) delta = 0.05f

        // Update visual effects
        if (screenShake > 0f) {
            screenShake -= delta
            if (screenShake < 0f) screenShake = 0f
        }
        if (comboFlash > 0f) {
            comboFlash -= delta * 2f
            if (comboFlash < 0f) comboFlash = 0f
        }

        if (!paused) {
            engine.update(delta)
        }

        // Apply screen shake to renderer
        if (screenShake > 0f) {
            val shakeX = (random.nextFloat() - 0.5f) * shakeIntensity * (screenShake / 0.2f)
            val shakeY = (random.nextFloat() - 0.5f) * shakeIntensity * (screenShake / 0.2f)
            renderer2D.setOffset(shakeX, shakeY)
        } else {
            renderer2D.setOffset(0f, 0f)
        }

        engine.render(renderer2D)

        // Performance logging
        if (!paused) {
            val frameTime = (System.nanoTime() - frameStart) / 1_000_000f // Convert to milliseconds
            val fps = if (delta > 0f) (1f / delta).toInt() else 0
            logger?.logPerformanceMetric(fps.toFloat(), frameTime, engine.getObjectCount())
        }
    }

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        engine.handleTouch(event, viewWidth, viewHeight)
    }

    fun pause() {
        paused = true
        engine.pause()
        audioManager.stopMusic()
    }

    fun resume() {
        paused = false
        engine.resume()
        audioManager.startMusic()
        lastTimeNs = 0L
    }

    fun restart() {
        engine = GameEngine(config, listener, audioManager, logger, null, this)
        lastTimeNs = 0L
    }

    fun nextLevel() {
        engine.nextLevel()
    }

    fun reset(newConfig: GameConfig) {
        config = newConfig
        engine = GameEngine(config, listener, audioManager, logger, null, this)
    }

    fun release() {
        audioManager.release()
    }
}
