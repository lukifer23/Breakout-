package com.breakoutplus.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import com.breakoutplus.SettingsManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

class GameRenderer(
    private val context: Context,
    private var config: GameConfig,
    private val listener: GameEventListener
) : GLSurfaceView.Renderer {

    private val renderer2D = Renderer2D()
    private val audioManager = GameAudioManager(context, config.settings)
    private val logger = GameLogger(context, config.settings.loggingEnabled)
    private var engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
    private var lastTimeNs: Long = 0L
    private var paused = false
    private var worldWidth = 100f
    private var worldHeight = 160f
    private val viewportState = ViewportState()

    // Enhanced visual effects
    private var screenShake = 0f
    private var screenShakeDuration = 0f
    private var shakeIntensity = 0f
    private var shakePhase = 0f
    private var comboFlash = 0f
    private var levelClearFlash = 0f
    private var impactFlash = 0f
    private var volleyDanger = 0f
    private var volleyDangerTarget = 0f
    private var visualTimeSeconds = 0f
    private var musicWasPlaying = false
    private var fixedStepSeconds = 1f / 120f
    private var simulationAccumulator = 0f
    private var debugAutoPlayEnabled = false
    private var debugProgressionProbeEnabled = false
    private var recoveryAttempts = 0
    private val maxRecoveryAttempts = 2
    private var perfLogSampleTimer = 0f
    private var fpsUiSampleTimer = 0f
    private var lastReportedFps = 0
    private val performanceSampleInterval = 0.25f
    private val fpsUiSampleInterval = 0.16f
    private val shakeAmplitudeScale = 0.34f
    private val maxShakeAmplitude = 1.15f
    private val comboFlashDuration = 0.28f
    private val levelClearFlashDuration = 0.72f
    private val comboFlashColor = floatArrayOf(0.9f, 0.98f, 1f, 0f)
    private val levelClearFlashColor = floatArrayOf(1f, 0.85f, 0.35f, 0f)
    private val impactFlashColor = floatArrayOf(1f, 1f, 1f, 0f)
    private val volleyDangerColor = floatArrayOf(1f, 0f, 0f, 0f)

    fun triggerScreenShake(intensity: Float = 3f, duration: Float = 0.2f) {
        val clampedIntensity = intensity.coerceIn(0f, 2.4f)
        val clampedDuration = duration.coerceIn(0.03f, 0.24f)
        shakeIntensity = max(shakeIntensity, clampedIntensity)
        screenShakeDuration = max(screenShakeDuration, clampedDuration)
        screenShake = max(screenShake, clampedDuration)
    }

    fun triggerComboFlash() {
        comboFlash = max(comboFlash, comboFlashDuration)
    }

    fun triggerLevelClearFlash() {
        levelClearFlash = max(levelClearFlash, levelClearFlashDuration)
    }

    fun triggerImpactFlash(intensity: Float) {
        impactFlash = (impactFlash + intensity.coerceIn(0f, 1f) * 0.62f).coerceIn(0f, 1f)
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
        viewportState.update(width, height)
        worldWidth = 100f
        worldHeight = worldWidth * (height.toFloat() / width.toFloat())
        resetVisualEffects()
        engine.onResize(width, height)
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        try {
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
            if (impactFlash > 0f) {
                impactFlash = (impactFlash - delta * 2.0f).coerceAtLeast(0f)
            }
            visualTimeSeconds += delta
            if (volleyDanger != volleyDangerTarget) {
                val response = if (delta > 0f) 1f - exp(-8f * delta) else 1f
                volleyDanger += (volleyDangerTarget - volleyDanger) * response
                if (abs(volleyDanger - volleyDangerTarget) < 0.002f) {
                    volleyDanger = volleyDangerTarget
                }
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
                comboFlashColor[3] = alpha
                renderer2D.drawRect(0f, 0f, worldWidth, worldHeight, comboFlashColor)
            }

            if (levelClearFlash > 0f) {
                val t = (levelClearFlash / levelClearFlashDuration).coerceIn(0f, 1f)
                val alpha = (smoothStep(t) * 0.42f).coerceIn(0f, 0.42f)
                levelClearFlashColor[3] = alpha
                renderer2D.drawRect(0f, 0f, worldWidth, worldHeight, levelClearFlashColor)
            }

            if (impactFlash > 0f) {
                val t = (impactFlash).coerceIn(0f, 1f)
                val alpha = (smoothStep(t) * 0.6f).coerceIn(0f, 0.6f)
                impactFlashColor[3] = alpha
                renderer2D.drawRect(0f, 0f, worldWidth, worldHeight, impactFlashColor)
            }

            // Volley Danger Zone Overlay
            if (volleyDanger > 0f) {
                val pulse = ((sin(visualTimeSeconds * 5f) + 1.0) * 0.5).toFloat()
                val alpha = (volleyDanger * (0.3f + 0.2f * pulse)).coerceIn(0f, 0.6f)
                // Draw a gradient or semi-transparent red rect at the bottom
                // In Ortho with y=0 at bottom, this needs to be at y=0.
                val dangerHeight = worldHeight * 0.25f
                volleyDangerColor[3] = alpha
                renderer2D.drawRect(0f, 0f, worldWidth, dangerHeight, volleyDangerColor)
            }

            // Performance logging
            if (!paused) {
                val frameTime = (System.nanoTime() - frameStart) / 1_000_000f // Convert to milliseconds
                val fps = if (delta > 0f) (1f / delta).toInt() else 0
                perfLogSampleTimer += delta
                fpsUiSampleTimer += delta
                if (perfLogSampleTimer >= performanceSampleInterval) {
                    logger.logPerformanceMetric(fps.toFloat(), frameTime, engine.getObjectCount())
                    perfLogSampleTimer = 0f
                }
                if (config.settings.showFpsCounter &&
                    (fpsUiSampleTimer >= fpsUiSampleInterval || lastReportedFps == 0)
                ) {
                    listener.onFpsUpdate(fps)
                    fpsUiSampleTimer = 0f
                    lastReportedFps = fps
                }
            }
            if (recoveryAttempts > 0) {
                recoveryAttempts = 0
            }
        } catch (t: Throwable) {
            handleFatalRenderError(t, "onDrawFrame")
        }
    }

    fun setVolleyDanger(danger: Float) {
        volleyDangerTarget = danger.coerceIn(0f, 1f)
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

    fun setDebugProgressionProbe(enabled: Boolean) {
        debugProgressionProbeEnabled = enabled
        engine.setDebugProgressionProbe(enabled)
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
        perfLogSampleTimer = 0f
        fpsUiSampleTimer = 0f
        lastReportedFps = 0
    }

    fun restart() {
        engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
        engine.setDebugAutoPlay(debugAutoPlayEnabled)
        engine.setDebugProgressionProbe(debugProgressionProbeEnabled)
        reapplyViewportToEngine()
        resetVisualEffects()
        lastTimeNs = 0L
        simulationAccumulator = 0f
        perfLogSampleTimer = 0f
        fpsUiSampleTimer = 0f
        lastReportedFps = 0
        recoveryAttempts = 0
    }

    fun nextLevel() {
        engine.nextLevel()
    }

    fun reset(newConfig: GameConfig) {
        config = newConfig
        audioManager.updateSettings(newConfig.settings)
        logger.setEnabled(newConfig.settings.loggingEnabled)
        engine = GameEngine(config, listener, audioManager, logger, config.dailyChallenges, this)
        engine.setDebugAutoPlay(debugAutoPlayEnabled)
        engine.setDebugProgressionProbe(debugProgressionProbeEnabled)
        reapplyViewportToEngine()
        resetVisualEffects()
        simulationAccumulator = 0f
        perfLogSampleTimer = 0f
        fpsUiSampleTimer = 0f
        lastReportedFps = 0
        recoveryAttempts = 0
    }

    fun updateSettings(settings: SettingsManager.Settings) {
        config = config.copy(settings = settings)
        engine.updateSettings(settings)
        audioManager.updateSettings(settings)
        logger.setEnabled(settings.loggingEnabled)
    }

    fun updateUnlocks(unlocks: com.breakoutplus.UnlockManager.UnlockState) {
        config = config.copy(unlocks = unlocks)
        engine.updateUnlocks(unlocks)
    }

    fun snapshotSummary(): GameSummary {
        return engine.currentSummary()
    }

    fun reportExternalError(source: String, t: Throwable) {
        handleFatalRenderError(t, source)
    }

    fun release() {
        audioManager.release()
    }

    private fun smoothStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun resetVisualEffects() {
        screenShake = 0f
        screenShakeDuration = 0f
        shakeIntensity = 0f
        shakePhase = 0f
        comboFlash = 0f
        levelClearFlash = 0f
        impactFlash = 0f
        volleyDanger = 0f
        volleyDangerTarget = 0f
        visualTimeSeconds = 0f
        renderer2D.setOffset(0f, 0f)
    }

    private fun reapplyViewportToEngine() {
        viewportState.reapply { width, height -> engine.onResize(width, height) }
    }

    private fun handleFatalRenderError(t: Throwable, source: String) {
        Log.e("GameRenderer", "Fatal render/update error", t)
        logger.logError(
            "render_crash",
            mapOf(
                "source" to source,
                "type" to (t::class.java.simpleName ?: "Throwable"),
                "message" to (t.message ?: "unknown")
            )
        )
        if (t is VirtualMachineError || t is LinkageError) {
            resetVisualEffects()
            paused = true
            engine.pause()
            return
        }
        if (recoveryAttempts >= maxRecoveryAttempts) {
            resetVisualEffects()
            paused = true
            engine.pause()
            return
        }
        recoveryAttempts += 1
        // Preserve in-flight run state: drop the bad frame and continue.
        simulationAccumulator = 0f
        lastTimeNs = 0L
        resetVisualEffects()
    }
}
