package com.breakoutplus.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.breakoutplus.R
import com.breakoutplus.SettingsManager

class GameAudioManager(private val context: Context, private var settings: SettingsManager.Settings) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<GameSound, Int>()
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private var musicPausedByFocusLoss = false
    private val hapticEffects = mapOf(
        GameHaptic.LIGHT to VibrationEffect.createOneShot(18, 80),
        GameHaptic.MEDIUM to VibrationEffect.createOneShot(28, 120),
        GameHaptic.HEAVY to VibrationEffect.createOneShot(45, 180)
    )
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isMusicPlaying()) {
                    musicPausedByFocusLoss = true
                    stopMusic()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (musicPausedByFocusLoss && settings.musicEnabled) {
                    musicPausedByFocusLoss = false
                    startMusic()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val musicVol = (settings.musicVolume * settings.masterVolume * 0.35f)
                mediaPlayer?.setVolume(musicVol, musicVol)
            }
        }
    }
    private val audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
    } else {
        null
    }

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
        soundMap[GameSound.BRICK_NORMAL] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.BRICK_REINFORCED] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.BRICK_ARMORED] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.BRICK_EXPLOSIVE] = soundPool.load(context, R.raw.sfx_explosion, 1)
        soundMap[GameSound.BRICK_UNBREAKABLE] = soundPool.load(context, R.raw.sfx_bounce, 1)
        soundMap[GameSound.BRICK_MOVING] = soundPool.load(context, R.raw.sfx_brick, 1)
        soundMap[GameSound.BRICK_SPAWNING] = soundPool.load(context, R.raw.sfx_powerup, 1)
        soundMap[GameSound.BRICK_PHASE] = soundPool.load(context, R.raw.sfx_brick, 1)
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
        val finalVolume = volume * settings.effectsVolume * settings.masterVolume
        val finalRate = rate.coerceIn(0.7f, 1.3f)
        soundPool.play(id, finalVolume, finalVolume, 1, 0, finalRate)
    }

    fun startMusic() {
        if (!settings.musicEnabled) return
        requestAudioFocus()
        val player = mediaPlayer ?: MediaPlayer.create(context, R.raw.music_loop)?.also {
            it.isLooping = true
            mediaPlayer = it
        } ?: return
        if (!player.isPlaying) {
            val musicVol = settings.musicVolume * settings.masterVolume
            player.setVolume(musicVol, musicVol)
            player.start()
        }
    }

    fun haptic(type: GameHaptic) {
        if (!settings.vibrationEnabled) return
        val effect = hapticEffects[type] ?: return
        vibrator?.vibrate(effect)
    }

    fun stopMusic() {
        mediaPlayer?.pause()
    }

    fun isMusicPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun release() {
        abandonAudioFocus()
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

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusListener)
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
