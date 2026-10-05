package com.breakoutplus.game

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Comprehensive game logging system for opt-in local debugging.
 * Records game events, player actions, performance metrics, and game state snapshots.
 */
class GameLogger(private val context: Context, enabled: Boolean = false) : GameDiagnostics {

    @Volatile
    private var enabled: Boolean = enabled

    private val logBuffer = mutableListOf<GameEvent>()
    private var sessionStartTime = System.currentTimeMillis()
    private var sessionId = UUID.randomUUID().toString().substring(0, 8)

    private val writerDelegate = lazy {
        DiagnosticLogWriter<GameEvent>(File(context.filesDir, "game_logs"), "${sessionStartTime}_${sessionId}", encode = { event ->
            JSONObject().apply {
                put("timestamp", event.timestamp); put("type", event.type.name)
                put("data", JSONObject(event.data))
            }.toString()
        })
    }

    private val writer by writerDelegate

    // Performance tracking
    private var frameCount = 0
    private var lastFpsUpdate = System.currentTimeMillis()
    private var currentFps = 0f

    enum class EventType {
        SESSION_START, SESSION_END, LEVEL_START, LEVEL_COMPLETE, GAME_OVER,
        BRICK_DESTROYED, POWERUP_COLLECTED, BALL_LOST, COMBO_ACHIEVED,
        TOUCH_INPUT, PERFORMANCE_METRIC, STATE_SNAPSHOT
    }

    data class GameEvent(
        val timestamp: Long,
        val type: EventType,
        val data: Map<String, Any>
    )

    // Core logging methods
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun logSessionStart(mode: GameMode, seed: Long) {
        if (!enabled) return
        logEvent(EventType.SESSION_START, mapOf(
            "sessionId" to sessionId,
            "mode" to mode.displayName,
            "seed" to seed,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    fun logSessionEnd(finalScore: Int, levelsCompleted: Int, totalTime: Long) {
        if (!enabled) return
        logEvent(EventType.SESSION_END, mapOf(
            "sessionId" to sessionId,
            "finalScore" to finalScore,
            "levelsCompleted" to levelsCompleted,
            "totalTimeMs" to totalTime,
            "averageFps" to currentFps
        ))
        flushLogs()
    }

    override fun logLevelStart(levelIndex: Int, theme: String) {
        if (!enabled) return
        logEvent(EventType.LEVEL_START, mapOf(
            "levelIndex" to levelIndex,
            "theme" to theme,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    override fun logLevelComplete(levelIndex: Int, score: Int, timeTaken: Float, bricksRemaining: Int) {
        if (!enabled) return
        logEvent(EventType.LEVEL_COMPLETE, mapOf(
            "levelIndex" to levelIndex,
            "score" to score,
            "timeTaken" to timeTaken,
            "bricksRemaining" to bricksRemaining
        ))
    }

    override fun logGameOver(finalScore: Int, levelReached: Int, reason: String) {
        if (!enabled) return
        logEvent(EventType.GAME_OVER, mapOf(
            "finalScore" to finalScore,
            "levelReached" to levelReached,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    override fun logError(message: String, extraData: Map<String, Any>) {
        if (!enabled) return
        logEvent(EventType.STATE_SNAPSHOT, mapOf("error" to message) + extraData)
    }

    override fun logLevelAdvance(newLevelIndex: Int) {
        if (!enabled) return
        logEvent(EventType.LEVEL_START, mapOf(
            "levelIndex" to newLevelIndex,
            "advanceType" to "next_level",
            "timestamp" to System.currentTimeMillis()
        ))
    }

    override fun logBrickDestroyed(brickType: BrickType, position: Pair<Float, Float>, comboCount: Int) {
        if (!enabled) return
        logEvent(EventType.BRICK_DESTROYED, mapOf(
            "brickType" to brickType.name,
            "x" to position.first,
            "y" to position.second,
            "comboCount" to comboCount
        ))
    }

    override fun logPowerupCollected(powerupType: PowerUpType, position: Pair<Float, Float>) {
        if (!enabled) return
        logEvent(EventType.POWERUP_COLLECTED, mapOf(
            "powerupType" to powerupType.name,
            "x" to position.first,
            "y" to position.second
        ))
    }

    override fun logBallLost(ballCount: Int, position: Pair<Float, Float>, livesRemaining: Int) {
        if (!enabled) return
        logEvent(EventType.BALL_LOST, mapOf(
            "ballCount" to ballCount,
            "x" to position.first,
            "y" to position.second,
            "livesRemaining" to livesRemaining
        ))
    }

    override fun logComboAchieved(comboCount: Int, multiplier: Float, scoreGained: Int) {
        if (!enabled) return
        logEvent(EventType.COMBO_ACHIEVED, mapOf(
            "comboCount" to comboCount,
            "multiplier" to multiplier,
            "scoreGained" to scoreGained
        ))
    }

    override fun logTouchInput(action: String, x: Float, y: Float, pressure: Float) {
        if (!enabled) return
        logEvent(EventType.TOUCH_INPUT, mapOf(
            "action" to action,
            "x" to x,
            "y" to y,
            "pressure" to pressure
        ))
    }

    fun logPerformanceMetric(fps: Float, frameTime: Float, objectCount: Int) {
        if (!enabled) return
        currentFps = fps
        logEvent(EventType.PERFORMANCE_METRIC, mapOf(
            "fps" to fps,
            "frameTimeMs" to frameTime,
            "objectCount" to objectCount,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    fun logStateSnapshot(
        balls: List<Ball>,
        bricks: List<Brick>,
        powerups: List<PowerUp>,
        paddleX: Float,
        score: Int,
        lives: Int
    ) {
        if (!enabled) return
        val snapshot = mapOf(
            "balls" to balls.map { mapOf("x" to it.x, "y" to it.y, "vx" to it.vx, "vy" to it.vy) },
            "bricks" to bricks.filter { it.alive }.map {
                mapOf("x" to it.x, "y" to it.y, "type" to it.type.name, "hits" to it.hitPoints)
            },
            "powerups" to powerups.map { mapOf("x" to it.x, "y" to it.y, "type" to it.type.name) },
            "paddleX" to paddleX,
            "score" to score,
            "lives" to lives
        )
        logEvent(EventType.STATE_SNAPSHOT, mapOf("snapshot" to snapshot))
    }

    // Internal logging
    private fun logEvent(type: EventType, data: Map<String, Any>) {
        if (!enabled) return

        synchronized(logBuffer) {
            logBuffer.add(GameEvent(System.currentTimeMillis(), type, data.toMap()))
            if (logBuffer.size >= 256) flushLogs()
        }
    }

    private fun flushLogs() {
        val batch = synchronized(logBuffer) {
            if (logBuffer.isEmpty()) return
            logBuffer.toList().also { logBuffer.clear() }
        }
        writer.append(batch)
    }

    fun close() {
        flushLogs() // enqueue only; Activity/GL teardown does not wait for disk.
        if (writerDelegate.isInitialized()) writer.close()
    }

    // Export is an explicit diagnostic action, never a gameplay callback.
    fun exportLogs(): String? {
        flushLogs()
        if (!writer.awaitIdle()) return null
        return try {
            File(context.filesDir, "game_logs").listFiles()
                ?.filter { it.name.startsWith("session_") }
                ?.maxByOrNull { it.lastModified() }?.readText()
        } catch (e: Exception) { Log.e("GameLogger", "Failed to export logs", e); null }
    }

    // Get performance summary
    fun getPerformanceSummary(): Map<String, Any> {
        return mapOf(
            "sessionId" to sessionId,
            "sessionDurationMs" to (System.currentTimeMillis() - sessionStartTime),
            "averageFps" to currentFps,
            "totalEventsLogged" to logBuffer.size,
            "enabled" to enabled
        )
    }
}
