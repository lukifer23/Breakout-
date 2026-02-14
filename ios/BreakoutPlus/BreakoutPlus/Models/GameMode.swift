//
//  GameMode.swift
//  BreakoutPlus
//
//  Game mode configurations controlling lives, timers, and special rules.
//  Direct port from Android GameMode.kt
//

import Foundation

enum GameMode: String, CaseIterable, Identifiable, Codable {
    case classic, timed, endless, god, rush, volley, tunnel, survival, invaders, zen

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .classic: return "Classic"
        case .timed: return "Timed Challenge"
        case .endless: return "Endless"
        case .god: return "God Mode"
        case .rush: return "Level Rush"
        case .volley: return "Volley"
        case .tunnel: return "Tunnel Siege"
        case .survival: return "Survival"
        case .invaders: return "Invaders"
        case .zen: return "Zen Mode"
        }
    }

    var description: String {
        switch self {
        case .classic: return "Standard breakout with escalating levels and powerups."
        case .timed: return "Race the clock. Clear as many bricks as possible."
        case .endless: return "Infinite levels with scaling speed and brick density."
        case .god: return "Practice mode. No life loss, perfect for experimentation."
        case .rush: return "Beat each stage before the timer expires."
        case .volley: return "Aim once, launch a chain of balls, then brace for descending rows."
        case .tunnel: return "Break through a fortified ring and route shots through a narrow entry tunnel."
        case .survival: return "One life. Speed ramps faster as you climb."
        case .invaders: return "Breakout meets space invaders. Bounce shots to clear ships while dodging fire."
        case .zen: return "Relaxing brick-breaking without pressure. No scores or lives to worry about."
        }
    }

    var meta: String {
        switch self {
        case .classic: return "Lives 3 • No timer • Balanced"
        case .timed: return "Lives 2 • 2:30 timer • Fast"
        case .endless: return "Lives 3 • No timer • Scaling"
        case .god: return "Infinite lives • Endless levels"
        case .rush: return "Lives 1 • 0:55 per level • Hardcore"
        case .volley: return "Lives 1 • Turn-based • Chain shots"
        case .tunnel: return "Lives 2 • Precision routing • Fortress"
        case .survival: return "Lives 1 • No timer • High speed"
        case .invaders: return "Shielded paddle • Enemy fire • No timer"
        case .zen: return "No scores • No lives • Relaxed"
        }
    }

    var baseLives: Int {
        switch self {
        case .classic, .endless, .invaders: return 3
        case .timed: return 2
        case .god, .zen: return 99
        case .rush, .volley, .survival: return 1
        case .tunnel: return 2
        }
    }

    var timeLimitSeconds: Int {
        switch self {
        case .timed: return 150  // 2:30
        case .rush: return 55    // 0:55 per level
        case .volley: return 0   // No timer
        default: return 0        // No timer
        }
    }

    var endless: Bool {
        switch self {
        case .endless, .survival, .god: return true
        default: return false
        }
    }

    var godMode: Bool {
        switch self {
        case .god, .zen: return true
        default: return false
        }
    }

    var rush: Bool {
        switch self {
        case .rush: return true
        default: return false
        }
    }

    var invaders: Bool {
        switch self {
        case .invaders: return true
        default: return false
        }
    }

    var launchSpeed: Float {
        switch self {
        case .classic: return 76.5
        case .timed: return 91.8
        case .endless: return 85.0
        case .god: return 72.25
        case .rush: return 90.0
        case .volley: return 82.0
        case .tunnel: return 79.5
        case .survival: return 92.5
        case .invaders: return 78.2
        case .zen: return 72.25
        }
    }
}
