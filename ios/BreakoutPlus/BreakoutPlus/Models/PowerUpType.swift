//
//  PowerUpType.swift
//  BreakoutPlus
//
//  Powerup type definitions with effects and durations.
//  Direct port from Android PowerUpType.kt
//

import Foundation

enum PowerUpType: String, CaseIterable {
    case multiBall, laser, guardrail, shield, extraLife, widePaddle, shrink
    case slowMotion, overdrive, fireball, magnet, gravityWell, ballSplitter, freeze, pierce

    var displayName: String {
        switch self {
        case .multiBall: return "Multi-Ball"
        case .laser: return "Laser"
        case .guardrail: return "Guardrail"
        case .shield: return "Shield"
        case .extraLife: return "Extra Life"
        case .widePaddle: return "Wide Paddle"
        case .shrink: return "Shrink"
        case .slowMotion: return "Slow Motion"
        case .overdrive: return "Overdrive"
        case .fireball: return "Fireball"
        case .magnet: return "Magnet"
        case .gravityWell: return "Gravity Well"
        case .ballSplitter: return "Ball Splitter"
        case .freeze: return "Freeze"
        case .pierce: return "Pierce"
        }
    }

    var description: String {
        switch self {
        case .multiBall: return "Creates multiple balls for increased chaos"
        case .laser: return "Shoot lasers to destroy bricks from distance"
        case .guardrail: return "Creates barriers to prevent ball loss"
        case .shield: return "Protects paddle from ball damage"
        case .extraLife: return "Grants an additional life"
        case .widePaddle: return "Makes paddle wider for easier catches"
        case .shrink: return "Shrinks the paddle temporarily"
        case .slowMotion: return "Slows down time for precision play"
        case .overdrive: return "Speeds up the action"
        case .fireball: return "Ball passes through bricks and destroys unbreakable ones"
        case .magnet: return "Attracts powerups to the paddle"
        case .gravityWell: return "Creates attractive force for nearby powerups"
        case .ballSplitter: return "Splits ball into multiple projectiles"
        case .freeze: return "Freezes time temporarily"
        case .pierce: return "Ball passes through multiple bricks"
        }
    }

    var duration: TimeInterval {
        switch self {
        case .multiBall: return 0  // Instant effect
        case .laser: return 10.0
        case .guardrail: return 15.0
        case .shield: return 12.0
        case .extraLife: return 0  // Instant effect
        case .widePaddle: return 8.0
        case .shrink: return 8.0
        case .slowMotion: return 6.0
        case .overdrive: return 6.0
        case .fireball: return 10.0
        case .magnet: return 12.0
        case .gravityWell: return 8.0
        case .ballSplitter: return 0  // Instant effect
        case .freeze: return 5.0
        case .pierce: return 12.0
        }
    }

    var color: (red: Float, green: Float, blue: Float) {
        switch self {
        case .multiBall: return (0.2, 0.9, 1.0)     // Cyan
        case .laser: return (1.0, 0.2, 0.8)          // Magenta
        case .guardrail: return (0.3, 1.0, 0.3)      // Green
        case .shield: return (1.0, 0.8, 0.2)         // Gold
        case .extraLife: return (1.0, 0.4, 0.4)      // Red
        case .widePaddle: return (0.5, 0.5, 1.0)     // Blue
        case .shrink: return (1.0, 0.45, 0.35)       // Warm red
        case .slowMotion: return (0.8, 0.4, 1.0)     // Purple
        case .overdrive: return (1.0, 0.6, 0.2)      // Orange
        case .fireball: return (1.0, 0.3, 0.0)       // Orange
        case .magnet: return (1.0, 0.6, 1.0)         // Pink
        case .gravityWell: return (0.4, 0.8, 1.0)    // Light blue
        case .ballSplitter: return (1.0, 1.0, 0.3)   // Yellow
        case .freeze: return (0.7, 0.9, 1.0)         // Ice blue
        case .pierce: return (0.9, 0.9, 0.9)         // Silver
        }
    }

    var isInstant: Bool {
        return duration == 0
    }

    var affectsPhysics: Bool {
        return self == .slowMotion || self == .freeze || self == .overdrive
    }

    var affectsPaddle: Bool {
        return self == .widePaddle || self == .shrink || self == .shield
    }

    var affectsBall: Bool {
        return self == .fireball || self == .pierce
    }
}
