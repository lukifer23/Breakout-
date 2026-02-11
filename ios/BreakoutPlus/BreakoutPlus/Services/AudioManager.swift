//
//  AudioManager.swift
//  BreakoutPlus
//
//  Lightweight audio system (SFX + music) for parity with Android.
//  Uses pooled AVAudioPlayer instances to allow overlapping sound effects.
//

import AVFoundation
import Foundation

enum GameSound: String, CaseIterable {
    case bounce = "sfx_bounce"
    case brick = "sfx_brick"
    case explosion = "sfx_explosion"
    case powerup = "sfx_powerup"
    case life = "sfx_life"
    case laser = "sfx_laser"
    case gameOver = "sfx_gameover"
}

final class AudioManager {
    static let shared = AudioManager()

    private var configured = false

    private var sfxPools: [GameSound: [AVAudioPlayer]] = [:]
    private var sfxIndex: [GameSound: Int] = [:]

    private var musicPlayer: AVAudioPlayer?

    // Settings
    private var soundEnabled = true
    private var musicEnabled = true
    private var masterVolume: Float = 1.0
    private var effectsVolume: Float = 0.8
    private var musicVolume: Float = 0.6

    private init() {}

    func configure(
        soundEnabled: Bool,
        musicEnabled: Bool,
        masterVolume: Float,
        effectsVolume: Float,
        musicVolume: Float
    ) {
        self.soundEnabled = soundEnabled
        self.musicEnabled = musicEnabled
        self.masterVolume = clamp01(masterVolume)
        self.effectsVolume = clamp01(effectsVolume)
        self.musicVolume = clamp01(musicVolume)

        ensureConfigured()
        applyMusicVolume()
        if !musicEnabled {
            pauseMusic()
        }
    }

    func startMusicIfEnabled() {
        ensureConfigured()
        guard musicEnabled else {
            stopMusic()
            return
        }
        if musicPlayer?.isPlaying == true { return }
        if musicPlayer == nil {
            musicPlayer = loadPlayer(filename: "music_loop")
            musicPlayer?.numberOfLoops = -1
        }
        applyMusicVolume()
        musicPlayer?.play()
    }

    func pauseMusic() {
        musicPlayer?.pause()
    }

    func stopMusic() {
        musicPlayer?.stop()
        musicPlayer?.currentTime = 0
    }

    func play(_ sound: GameSound, volume: Float = 1.0) {
        ensureConfigured()
        guard soundEnabled else { return }

        let pool = sfxPools[sound] ?? buildPool(for: sound, count: 4)
        sfxPools[sound] = pool
        let idx = sfxIndex[sound, default: 0] % pool.count
        sfxIndex[sound] = (idx + 1) % pool.count

        let player = pool[idx]
        player.currentTime = 0
        player.volume = clamp01(volume) * effectsVolume * masterVolume
        player.play()
    }

    private func ensureConfigured() {
        if configured { return }
        configured = true
        do {
            // Keep game audio polite: mix with other audio (e.g., user's music).
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.ambient, options: [.mixWithOthers])
            try session.setActive(true, options: [])
        } catch {
            // If audio session fails, we can still run the game; just skip audio.
        }
    }

    private func applyMusicVolume() {
        musicPlayer?.volume = musicVolume * masterVolume
    }

    private func buildPool(for sound: GameSound, count: Int) -> [AVAudioPlayer] {
        var players: [AVAudioPlayer] = []
        players.reserveCapacity(count)
        for _ in 0..<count {
            if let player = loadPlayer(filename: sound.rawValue) {
                players.append(player)
            }
        }
        return players
    }

    private func loadPlayer(filename: String) -> AVAudioPlayer? {
        guard let url = Bundle.main.url(forResource: filename, withExtension: "wav") else {
            return nil
        }
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            return player
        } catch {
            return nil
        }
    }

    private func clamp01(_ v: Float) -> Float {
        min(1.0, max(0.0, v))
    }
}
