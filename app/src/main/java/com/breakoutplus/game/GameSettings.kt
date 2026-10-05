package com.breakoutplus.game

data class GameSettings(
        val soundEnabled: Boolean,
        val musicEnabled: Boolean,
        val vibrationEnabled: Boolean,
        val tipsEnabled: Boolean,
        val leftHanded: Boolean,
        val sensitivity: Float,
        val masterVolume: Float = 1.0f,
        val effectsVolume: Float = 0.8f,
        val musicVolume: Float = 0.6f,
        val loggingEnabled: Boolean = false,
        val darkMode: Boolean = false,
        val showFpsCounter: Boolean = false,
        val highRefreshRate: Boolean = true
    )
