//
//  GameViewModel.swift
//  BreakoutPlus
//
//  Central state management and navigation for Breakout+ iOS
//  Port of Android GameViewModel pattern
//

import SwiftUI
import Combine

enum Screen {
    case splash, menu, game, settings, scoreboard, howTo, privacy, dailyChallenges
}

class GameViewModel: ObservableObject {
    @Published var currentScreen: Screen = .splash
    @Published var score = 0
    @Published var lives = 3
    @Published var level = 1
    @Published var activePowerup: PowerUpType?
    @Published var laserActive = false
    @Published var comboCount = 0
    @Published var timeRemaining = 0
    @Published var isPaused = false
    @Published var lastSummary: GameSummary?
    @Published var showLevelComplete = false
    @Published var showGameOver = false
    @Published var tipMessage: String?
    @Published var powerupStatuses: [PowerupStatus] = []

    // Game state
    var selectedGameMode: GameMode = .classic

    // Settings
    @AppStorage("soundEnabled") var soundEnabled = true
    @AppStorage("musicEnabled") var musicEnabled = true
    @AppStorage("vibrationEnabled") var vibrationEnabled = true
    @AppStorage("tipsEnabled") var tipsEnabled = true
    @AppStorage("leftHanded") var leftHanded = false
    @AppStorage("sensitivity") var sensitivity: Double = 0.7
    @AppStorage("masterVolume") var masterVolume: Double = 1.0
    @AppStorage("effectsVolume") var effectsVolume: Double = 0.8
    @AppStorage("musicVolume") var musicVolume: Double = 0.28
    @AppStorage("playerName") var playerName: String = "Player"

    private var cancellables = Set<AnyCancellable>()

    init() {
        // Auto-transition from splash to menu after 2 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            withAnimation(.easeInOut(duration: 0.5)) {
                self.currentScreen = .menu
            }
        }
    }

    // Navigation methods
    func navigateToMenu() {
        currentScreen = .menu
    }

    func startGame(mode: GameMode) {
        selectedGameMode = mode
        resetGameState()
        currentScreen = .game
    }

    func navigateToSettings() {
        currentScreen = .settings
    }

    func navigateToScoreboard() {
        currentScreen = .scoreboard
    }

    func navigateToHowTo() {
        currentScreen = .howTo
    }

    func navigateToPrivacy() {
        currentScreen = .privacy
    }

    func navigateToDailyChallenges() {
        currentScreen = .dailyChallenges
    }

    func exitToMenu() {
        currentScreen = .menu
        isPaused = false
        showLevelComplete = false
        showGameOver = false
        laserActive = false
    }

    private func resetGameState() {
        score = 0
        lives = selectedGameMode.baseLives
        level = 1
        activePowerup = nil
        laserActive = false
        comboCount = 0
        timeRemaining = selectedGameMode.timeLimitSeconds
        isPaused = false
        showLevelComplete = false
        showGameOver = false
        lastSummary = nil
        tipMessage = nil
    }

    // Game event handlers (called by GameEngine)
    func onScoreChanged(newScore: Int) {
        score = newScore
    }

    func onLivesChanged(newLives: Int) {
        lives = newLives
    }

    func onLevelChanged(newLevel: Int) {
        level = newLevel
    }

    func onPowerupActivated(type: PowerUpType) {
        activePowerup = type
        // Auto-clear after duration
        DispatchQueue.main.asyncAfter(deadline: .now() + 8.0) {
            if self.activePowerup == type {
                self.activePowerup = nil
            }
        }
    }

    func onComboAchieved(count: Int) {
        comboCount = count
    }

    func onTimeUpdated(seconds: Int) {
        timeRemaining = seconds
    }
}
