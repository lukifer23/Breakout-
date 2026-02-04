package com.breakoutplus

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "breakout_plus_settings"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_MUSIC = "music_enabled"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_TIPS = "tips_enabled"
    private const val KEY_LEFT_HANDED = "left_handed"
    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_MASTER_VOLUME = "master_volume"
    private const val KEY_EFFECTS_VOLUME = "effects_volume"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_SHOW_FPS_COUNTER = "show_fps_counter"

    data class Settings(
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
        val showFpsCounter: Boolean = false
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): Settings {
        val prefs = prefs(context)
        return Settings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            musicEnabled = prefs.getBoolean(KEY_MUSIC, false), // Disable music by default to prevent background hum
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            tipsEnabled = prefs.getBoolean(KEY_TIPS, true),
            leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 0.7f),
            masterVolume = prefs.getFloat(KEY_MASTER_VOLUME, 1.0f),
            effectsVolume = prefs.getFloat(KEY_EFFECTS_VOLUME, 0.8f),
            musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.6f),
            loggingEnabled = prefs.getBoolean(KEY_LOGGING_ENABLED, false),
            darkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            showFpsCounter = prefs.getBoolean(KEY_SHOW_FPS_COUNTER, false)
        )
    }

    fun save(context: Context, settings: Settings) {
        prefs(context).edit()
            .putBoolean(KEY_SOUND, settings.soundEnabled)
            .putBoolean(KEY_MUSIC, settings.musicEnabled)
            .putBoolean(KEY_VIBRATION, settings.vibrationEnabled)
            .putBoolean(KEY_TIPS, settings.tipsEnabled)
            .putBoolean(KEY_LEFT_HANDED, settings.leftHanded)
            .putFloat(KEY_SENSITIVITY, settings.sensitivity)
            .putFloat(KEY_MASTER_VOLUME, settings.masterVolume)
            .putFloat(KEY_EFFECTS_VOLUME, settings.effectsVolume)
            .putFloat(KEY_MUSIC_VOLUME, settings.musicVolume)
            .putBoolean(KEY_LOGGING_ENABLED, settings.loggingEnabled)
            .putBoolean(KEY_DARK_MODE, settings.darkMode)
            .putBoolean(KEY_SHOW_FPS_COUNTER, settings.showFpsCounter)
            .apply()
    }
}
