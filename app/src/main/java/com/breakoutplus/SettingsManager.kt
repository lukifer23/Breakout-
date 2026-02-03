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

    data class Settings(
        val soundEnabled: Boolean,
        val musicEnabled: Boolean,
        val vibrationEnabled: Boolean,
        val tipsEnabled: Boolean,
        val leftHanded: Boolean,
        val sensitivity: Float
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): Settings {
        val prefs = prefs(context)
        return Settings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            musicEnabled = prefs.getBoolean(KEY_MUSIC, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            tipsEnabled = prefs.getBoolean(KEY_TIPS, true),
            leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 0.7f)
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
            .apply()
    }
}
