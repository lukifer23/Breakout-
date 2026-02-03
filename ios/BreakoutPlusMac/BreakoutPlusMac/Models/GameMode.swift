//
//  GameMode.swift
//  BreakoutPlus
//
//  Game mode configurations controlling lives, timers, and special rules.
//  Direct port from Android GameMode.kt
//

import Foundation

enum GameMode: String, CaseIterable, Identifiable {
    case classic, timed, endless, god, rush

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .classic: return "Classic"
        case .timed: return "Timed Challenge"
        case .endless: return "Endless"
        case .god: return "God Mode"
        case .rush: return "Level Rush"
        }
    }

    var description: String {
        switch self {
        case .classic: return "Standard breakout with escalating levels and powerups."
        case .timed: return "Race the clock. Clear as many bricks as possible."
        case .endless: return "Infinite levels with scaling speed and brick density."
        case .god: return "Practice mode. No life loss, perfect for experimentation."
        case .rush: return "Beat each stage before the timer expires."
        }
    }

    var meta: String {
        switch self {
        case .classic: return "Lives 3 • No timer • Balanced"
        case .timed: return "Lives 2 • 2:30 timer • Fast"
        case .endless: return "Lives 3 • No timer • Scaling"
        case .god: return "Infinite lives • No timer"
        case .rush: return "Lives 1 • 0:45 per level • Hardcore"
        }
    }

    var baseLives: Int {
        switch self {
        case .classic, .endless: return 3
        case .timed: return 2
        case .god: return 99
        case .rush: return 1
        }
    }

    var timeLimitSeconds: Int {
        switch self {
        case .timed: return 150  // 2:30
        case .rush: return 45    // 0:45 per level
        default: return 0        // No timer
        }
    }

    var endless: Bool {
        switch self {
        case .endless: return true
        default: return false
        }
    }

    var godMode: Bool {
        switch self {
        case .god: return true
        default: return false
        }
    }

    var rush: Bool {
        switch self {
        case .rush: return true
        default: return false
        }
    }

    var launchSpeed: Float {
        switch self {
        case .classic: return 37.0
        case .timed: return 42.0
        case .endless: return 39.0
        case .god: return 33.0
        case .rush: return 44.0
        }
    }
}