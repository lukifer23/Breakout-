package com.breakoutplus.game

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.opengl.GLSurfaceView

class GameGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var rendererImpl: GameRenderer? = null

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
    }

    fun start(config: GameConfig, listener: GameEventListener) {
        if (rendererImpl == null) {
            rendererImpl = GameRenderer(context, config, listener)
            setRenderer(rendererImpl)
            renderMode = RENDERMODE_CONTINUOUSLY
        } else {
            rendererImpl?.reset(config)
        }
    }

    fun pauseGame() {
        rendererImpl?.pause()
        renderMode = RENDERMODE_WHEN_DIRTY
        requestRender()
    }

    fun resumeGame() {
        rendererImpl?.resume()
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun restartGame() {
        rendererImpl?.restart()
    }

    fun nextLevel() {
        rendererImpl?.nextLevel()
    }

    fun applySettings(settings: com.breakoutplus.SettingsManager.Settings) {
        rendererImpl?.updateSettings(settings)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        rendererImpl?.handleTouch(event, width.toFloat(), height.toFloat())
        return true
    }

    override fun onDetachedFromWindow() {
        rendererImpl?.release()
        super.onDetachedFromWindow()
    }
}
