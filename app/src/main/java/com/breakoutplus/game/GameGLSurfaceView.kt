package com.breakoutplus.game

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper

class GameGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var rendererImpl: GameRenderer? = null
    private val framePacer = FramePacer(this)
    private var targetFps: Float = 0f
    private var surfaceReady = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
                applySurfaceFrameRate()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                surfaceReady = true
                applySurfaceFrameRate()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
            }
        })
    }

    fun start(config: GameConfig, listener: GameEventListener) {
        if (rendererImpl == null) {
            rendererImpl = GameRenderer(context, config, listener)
            setRenderer(rendererImpl)
            renderMode = RENDERMODE_WHEN_DIRTY
            framePacer.start()
        } else {
            rendererImpl?.reset(config)
            framePacer.start()
        }
    }

    fun pauseGame() {
        framePacer.stop()
        rendererImpl?.pause()
        renderMode = RENDERMODE_WHEN_DIRTY
        requestRender()
    }

    fun resumeGame() {
        rendererImpl?.resume()
        renderMode = RENDERMODE_WHEN_DIRTY
        framePacer.start()
    }

    fun restartGame() {
        rendererImpl?.restart()
    }

    fun nextLevel() {
        rendererImpl?.nextLevel()
    }

    fun setTargetFrameRate(fps: Float) {
        targetFps = if (fps.isFinite() && fps > 0f) fps else 0f
        framePacer.setTargetFps(targetFps)
        applySurfaceFrameRate()
    }

    fun applySettings(settings: com.breakoutplus.SettingsManager.Settings) {
        rendererImpl?.updateSettings(settings)
    }

    fun fireLaser() {
        rendererImpl?.fireLaser()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        rendererImpl?.handleTouch(event, width.toFloat(), height.toFloat())
        return true
    }

    override fun onDetachedFromWindow() {
        framePacer.stop()
        rendererImpl?.release()
        super.onDetachedFromWindow()
    }

    private fun applySurfaceFrameRate() {
        if (!surfaceReady || targetFps <= 0f) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val surface: Surface = holder.surface
            if (!surface.isValid) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                surface.setFrameRate(
                    targetFps,
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                    Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                )
            } else {
                surface.setFrameRate(
                    targetFps,
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                )
            }
        }
    }

    private class FramePacer(private val view: GLSurfaceView) : Choreographer.FrameCallback {
        private val handler = Handler(Looper.getMainLooper())
        private var running = false
        private var targetFrameNs = 0L
        private var lastFrameNs = 0L

        fun setTargetFps(fps: Float) {
            targetFrameNs = if (fps > 0f) (1_000_000_000L / fps).toLong() else 0L
        }

        fun start() {
            if (running) return
            running = true
            lastFrameNs = 0L
            handler.post { Choreographer.getInstance().postFrameCallback(this) }
        }

        fun stop() {
            if (!running) return
            running = false
            handler.post { Choreographer.getInstance().removeFrameCallback(this) }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val target = targetFrameNs
            val shouldRender = target == 0L || lastFrameNs == 0L || frameTimeNanos - lastFrameNs >= target - minOf(target / 10L, 2_000_000L)
            if (shouldRender) {
                view.requestRender()
                lastFrameNs = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
}
