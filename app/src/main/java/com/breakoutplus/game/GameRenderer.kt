package com.breakoutplus.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.breakoutplus.SettingsManager
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class GameRenderer(
    private val context: Context,
    private var config: GameConfig,
    private val listener: GameEventListener
) : GLSurfaceView.Renderer {

    private val renderer2D = Renderer2D()
    private val audioManager = GameAudioManager(context, config.settings)
    private val logger = if (config.settings.loggingEnabled) GameLogger(context, true) else null
    private var engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
    private var lastTimeNs: Long = 0L
    private var paused = false
    private var worldWidth = 100f
    private var worldHeight = 160f

    // Enhanced visual effects
    private var screenShake = 0f
    private var screenShakeDuration = 0f
    private var shakeIntensity = 0f
    private var shakePhase = 0f
    private var comboFlash = 0f
    private var levelClearFlash = 0f
    private var musicWasPlaying = false
    private var fixedStepSeconds = 1f / 120f
    private var simulationAccumulator = 0f
    private var debugAutoPlayEnabled = false
    private val shakeAmplitudeScale = 0.34f
    private val maxShakeAmplitude = 1.15f
    private val comboFlashDuration = 0.28f
    private val levelClearFlashDuration = 0.72f

    fun triggerScreenShake(intensity: Float = 3f, duration: Float = 0.2f) {
        val clampedIntensity = intensity.coerceIn(0f, 2.4f)
        val clampedDuration = duration.coerceIn(0.03f, 0.24f)
        shakeIntensity = max(shakeIntensity * 0.82f, clampedIntensity)
        screenShakeDuration = max(screenShakeDuration, clampedDuration)
        screenShake = max(screenShake, clampedDuration)
    }

    fun triggerComboFlash() {
        comboFlash = max(comboFlash, comboFlashDuration)
    }

    fun triggerLevelClearFlash() {
        levelClearFlash = max(levelClearFlash, levelClearFlashDuration)
    }

    fun setTargetFrameRate(fps: Float) {
        val normalized = if (fps.isFinite() && fps > 0f) fps.coerceIn(45f, 240f) else 120f
        fixedStepSeconds = 1f / normalized
    }

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.015f, 0.02f, 0.035f, 1f)
        renderer2D.init()
        // Music now starts when gameplay begins (ball launch) - not on activity load
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        GLES20.glViewport(0, 0, width, height)
        renderer2D.setViewport(width, height)
        worldWidth = 100f
        worldHeight = worldWidth * (height.toFloat() / width.toFloat())
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
        if (delta > 0.1f) delta = 0.1f

        // Update visual effects
        if (screenShake > 0f) {
            screenShake = (screenShake - delta).coerceAtLeast(0f)
            shakePhase += delta * (24f + shakeIntensity * 10f)
        }
        if (comboFlash > 0f) {
            comboFlash = (comboFlash - delta).coerceAtLeast(0f)
        }
        if (levelClearFlash > 0f) {
            levelClearFlash = (levelClearFlash - delta).coerceAtLeast(0f)
        }

        if (!paused) {
            val step = fixedStepSeconds.coerceIn(1f / 240f, 1f / 45f)
            simulationAccumulator = (simulationAccumulator + delta).coerceAtMost(step * 6f)
            var updates = 0
            while (simulationAccumulator >= step && updates < 6) {
                engine.update(step)
                simulationAccumulator -= step
                updates += 1
            }
            if (updates == 0 && delta > 0f) {
                // Keep controls responsive when frame pacing temporarily outruns simulation step.
                engine.update(delta.coerceAtMost(step))
                simulationAccumulator = 0f
            }
        }

        // Apply screen shake to renderer
        if (screenShake > 0f) {
            val decay = if (screenShakeDuration > 0f) {
                (screenShake / screenShakeDuration).coerceIn(0f, 1f)
            } else {
                0f
            }
            val amplitude = (shakeIntensity * smoothStep(decay) * shakeAmplitudeScale).coerceAtMost(maxShakeAmplitude)
            val shakeX = sin(shakePhase) * amplitude
            val shakeY = cos(shakePhase * 1.37f) * amplitude * 0.82f
            renderer2D.setOffset(shakeX, shakeY)
        } else {
            shakeIntensity = 0f
            screenShakeDuration = 0f
            renderer2D.setOffset(0f, 0f)
        }

        engine.render(renderer2D)

        if (comboFlash > 0f) {
            val t = (comboFlash / comboFlashDuration).coerceIn(0f, 1f)
            val alpha = (smoothStep(t) * 0.33f).coerceIn(0f, 0.33f)
            renderer2D.drawRect(0f, 0f, worldWidth, worldHeight, floatArrayOf(0.9f, 0.98f, 1f, alpha))
        }

        if (levelClearFlash > 0f) {
            val t = (levelClearFlash / levelClearFlashDuration).coerceIn(0f, 1f)
            val alpha = (smoothStep(t) * 0.42f).coerceIn(0f, 0.42f)
            renderer2D.drawRect(0f, 0f, worldWidth, worldHeight, floatArrayOf(1f, 0.85f, 0.35f, alpha))
        }

        // Performance logging
        if (!paused) {
            val frameTime = (System.nanoTime() - frameStart) / 1_000_000f // Convert to milliseconds
            val fps = if (delta > 0f) (1f / delta).toInt() else 0
            logger?.logPerformanceMetric(fps.toFloat(), frameTime, engine.getObjectCount())
            if (config.settings.showFpsCounter) {
                listener.onFpsUpdate(fps)
            }
        }
    }

    fun handleTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float) {
        engine.handleTouch(event, viewWidth, viewHeight)
    }

    fun fireLaser() {
        engine.triggerLaserFromUi()
    }

    fun debugSpawnPowerup(type: PowerUpType) {
        engine.debugSpawnPowerup(type)
    }

    fun setDebugAutoPlay(enabled: Boolean) {
        debugAutoPlayEnabled = enabled
        engine.setDebugAutoPlay(enabled)
    }

    fun isGameRunning(): Boolean {
        return !paused && engine.isGameRunning()
    }

    fun pause() {
        paused = true
        engine.pause()
        musicWasPlaying = audioManager.isMusicPlaying()
        audioManager.stopMusic()
    }

    fun resume() {
        paused = false
        engine.resume()
        if (musicWasPlaying) {
            audioManager.startMusic()
        }
        lastTimeNs = 0L
        simulationAccumulator = 0f
    }

    fun restart() {
        engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
        engine.setDebugAutoPlay(debugAutoPlayEnabled)
        lastTimeNs = 0L
        simulationAccumulator = 0f
    }

    fun nextLevel() {
        engine.nextLevel()
    }

    fun reset(newConfig: GameConfig) {
        config = newConfig
        audioManager.updateSettings(newConfig.settings)
        engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
        engine.setDebugAutoPlay(debugAutoPlayEnabled)
        simulationAccumulator = 0f
    }

    fun updateSettings(settings: SettingsManager.Settings) {
        config = config.copy(settings = settings)
        engine.updateSettings(settings)
        audioManager.updateSettings(settings)
    }

    fun updateUnlocks(unlocks: com.breakoutplus.UnlockManager.UnlockState) {
        config = config.copy(unlocks = unlocks)
        engine.updateUnlocks(unlocks)
    }

    fun release() {
        audioManager.release()
    }

    private fun smoothStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
