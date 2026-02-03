//
//  BrickType.swift
//  BreakoutPlus
//
//  Brick type definitions with behaviors and properties.
//  Direct port from Android BrickType.kt
//

import Foundation

enum BrickType: String, CaseIterable {
    case normal, reinforced, armored, explosive, unbreakable
    case moving, spawning, phase, boss

    var baseHitPoints: Int {
        switch self {
        case .normal: return 1
        case .reinforced: return 2
        case .armored: return 3
        case .explosive: return 1
        case .unbreakable: return 999
        case .moving: return 2
        case .spawning: return 2
        case .phase: return 3
        case .boss: return 6
        }
    }

    var scoreValue: Int {
        switch self {
        case .normal: return 10
        case .reinforced: return 20
        case .armored: return 30
        case .explosive: return 15
        case .unbreakable: return 50  // Bonus for destroying with special powerup
        case .moving: return 25
        case .spawning: return 35
        case .phase: return 40
        case .boss: return 100
        }
    }

    var isDestructible: Bool {
        return self != .unbreakable
    }

    var canBeHitByNormalBall: Bool {
        return self != .unbreakable
    }

    var explodesOnDestruction: Bool {
        return self == .explosive
    }

    var hasSpecialBehavior: Bool {
        return self == .moving || self == .spawning || self == .phase || self == .boss
    }
}