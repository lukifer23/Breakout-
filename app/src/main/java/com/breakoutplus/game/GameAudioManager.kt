package com.breakoutplus.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.breakoutplus.R
import com.breakoutplus.SettingsManager

class GameAudioManager(private val context: Context, private val settings: SettingsManager.Settings) {
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
        soundMap[GameSound.BOUNCE] = soundPool.load(context, R.raw.sfx_bounce, 1)
        soundMap[GameSound.BRICK] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.POWERUP] = soundPool.load(context, R.raw.sfx_powerup, 1)
        soundMap[GameSound.LIFE] = soundPool.load(context, R.raw.sfx_life, 1)
        soundMap[GameSound.EXPLOSION] = soundPool.load(context, R.raw.sfx_explosion, 1)
        soundMap[GameSound.LASER] = soundPool.load(context, R.raw.sfx_laser, 1)
        soundMap[GameSound.GAME_OVER] = soundPool.load(context, R.raw.sfx_gameover, 1)
    }

    fun play(sound: GameSound, volume: Float = 1f) {
        if (!settings.soundEnabled) return
        val id = soundMap[sound] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun startMusic() {
        if (!settings.musicEnabled) return
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.music_loop)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.35f, 0.35f)
        }
        mediaPlayer?.start()
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

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool.release()
    }
}

enum class GameSound {
    BOUNCE,
    BRICK,
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
