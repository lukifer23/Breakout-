package com.breakoutplus.game

import java.util.ArrayDeque

sealed interface GameFeedback {
    data class Sound(val sound: GameSound, val volume: Float, val rate: Float) : GameFeedback
    data class Haptic(val type: GameHaptic) : GameFeedback
    data object StopMusic : GameFeedback
    data object StartMusic : GameFeedback
}

/** Core emits commands; the Android adapter drains them on each rendered frame. */
class GameFeedbackQueue {
    private val events = ArrayDeque<GameFeedback>()
    fun play(sound: GameSound, volume: Float = 1f, rate: Float = 1f) {
        events.add(GameFeedback.Sound(sound, volume, rate))
    }
    fun haptic(type: GameHaptic) { events.add(GameFeedback.Haptic(type)) }
    fun stopMusic() { events.add(GameFeedback.StopMusic) }
    fun startMusic() { events.add(GameFeedback.StartMusic) }
    fun drain(consumer: (GameFeedback) -> Unit) {
        while (events.isNotEmpty()) consumer(events.removeFirst())
    }
}

interface GameVisualFeedback {
    fun triggerScreenShake(intensity: Float = 3f, duration: Float = 0.2f)
    fun triggerComboFlash()
    fun triggerLevelClearFlash()
    fun triggerImpactFlash(intensity: Float)
    fun setVolleyDanger(danger: Float)
}
