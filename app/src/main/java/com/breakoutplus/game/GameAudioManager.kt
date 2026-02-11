package com.breakoutplus.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.breakoutplus.R
import com.breakoutplus.SettingsManager

class GameAudioManager(private val context: Context, private var settings: SettingsManager.Settings) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<GameSound, Int>()
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(6)
            .build()
        // Load all sound effects (using existing files as fallbacks for new ones)
        soundMap[GameSound.BOUNCE] = soundPool.load(context, R.raw.sfx_bounce, 1)
        soundMap[GameSound.BRICK_NORMAL] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.BRICK_REINFORCED] = soundPool.load(context, R.raw.sfx_brick, 1) // Uses standard brick impact
        soundMap[GameSound.BRICK_ARMORED] = soundPool.load(context, R.raw.sfx_brick, 1) // Uses standard brick impact
        soundMap[GameSound.BRICK_EXPLOSIVE] = soundPool.load(context, R.raw.sfx_explosion, 1)
        soundMap[GameSound.BRICK_UNBREAKABLE] = soundPool.load(context, R.raw.sfx_bounce, 1) // Metallic bounce
        soundMap[GameSound.BRICK_MOVING] = soundPool.load(context, R.raw.sfx_brick, 1) // Uses standard brick impact
        soundMap[GameSound.BRICK_SPAWNING] = soundPool.load(context, R.raw.sfx_powerup, 1)
        soundMap[GameSound.BRICK_PHASE] = soundPool.load(context, R.raw.sfx_brick, 1) // Uses standard brick impact
        soundMap[GameSound.BRICK_BOSS] = soundPool.load(context, R.raw.sfx_explosion, 1)
        soundMap[GameSound.POWERUP] = soundPool.load(context, R.raw.sfx_powerup, 1)
        soundMap[GameSound.LIFE] = soundPool.load(context, R.raw.sfx_life, 1)
        soundMap[GameSound.EXPLOSION] = soundPool.load(context, R.raw.sfx_explosion, 1)
        soundMap[GameSound.LASER] = soundPool.load(context, R.raw.sfx_laser, 1)
        soundMap[GameSound.GAME_OVER] = soundPool.load(context, R.raw.sfx_gameover, 1)
    }

    fun play(sound: GameSound, volume: Float = 1f, rate: Float = 1f) {
        if (!settings.soundEnabled) return
        val id = soundMap[sound] ?: return

        // Apply volume settings based on sound type
        val finalVolume = when {
            sound == GameSound.BOUNCE -> volume * settings.effectsVolume * settings.masterVolume
            sound.name.startsWith("BRICK_") -> volume * settings.effectsVolume * settings.masterVolume
            sound == GameSound.POWERUP || sound == GameSound.LIFE ||
            sound == GameSound.EXPLOSION || sound == GameSound.LASER -> volume * settings.effectsVolume * settings.masterVolume
            sound == GameSound.GAME_OVER -> volume * settings.effectsVolume * settings.masterVolume
            else -> volume * settings.effectsVolume * settings.masterVolume
        }

        val finalRate = rate.coerceIn(0.7f, 1.3f)
        soundPool.play(id, finalVolume, finalVolume, 1, 0, finalRate)
    }

    fun startMusic() {
        if (!settings.musicEnabled) return
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.music_loop)
            mediaPlayer?.isLooping = true
        }
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            val musicVol = settings.musicVolume * settings.masterVolume
            mediaPlayer?.setVolume(musicVol, musicVol)
            mediaPlayer?.start()
        }
    }

    fun haptic(type: GameHaptic) {
        if (!settings.vibrationEnabled) return
        val effect = when (type) {
            GameHaptic.LIGHT -> VibrationEffect.createOneShot(18, 80)
            GameHaptic.MEDIUM -> VibrationEffect.createOneShot(28, 120)
            GameHaptic.HEAVY -> VibrationEffect.createOneShot(45, 180)
        }
        vibrator?.vibrate(effect)
    }

    fun stopMusic() {
        mediaPlayer?.pause()
    }

    fun isMusicPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool.release()
    }

    fun updateSettings(newSettings: SettingsManager.Settings) {
        settings = newSettings
        if (!settings.musicEnabled) {
            stopMusic()
        } else if (mediaPlayer != null) {
            val musicVol = settings.musicVolume * settings.masterVolume
            mediaPlayer?.setVolume(musicVol, musicVol)
        }
    }
}

enum class GameSound {
    BOUNCE,
    BRICK_NORMAL,
    BRICK_REINFORCED,
    BRICK_ARMORED,
    BRICK_EXPLOSIVE,
    BRICK_UNBREAKABLE,
    BRICK_MOVING,
    BRICK_SPAWNING,
    BRICK_PHASE,
    BRICK_BOSS,
    POWERUP,
    LIFE,
    EXPLOSION,
    LASER,
    GAME_OVER
}

enum class GameHaptic {
    LIGHT,
    MEDIUM,
    HEAVY
}
