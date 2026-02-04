//
//  LevelTheme.swift
//  BreakoutPlus
//
//  Level theme with color palettes - simplified version for initial implementation
//

import Foundation

struct LevelTheme {
    let name: String
    let background: (red: Float, green: Float, blue: Float)
    let paddle: (red: Float, green: Float, blue: Float)
    let accent: (red: Float, green: Float, blue: Float)
    let brickPalette: [BrickType: (red: Float, green: Float, blue: Float)]

    static let neon = LevelTheme(
        name: "Neon",
        background: (0.03, 0.06, 0.12),
        paddle: (0.97, 0.97, 1.0),
        accent: (0.19, 0.88, 0.97),
        brickPalette: [
            .normal: (0.2, 0.9, 1.0),      // Bright cyan
            .reinforced: (1.0, 0.2, 0.8),  // Magenta
            .armored: (0.3, 1.0, 0.3),     // Bright green
            .explosive: (1.0, 0.5, 0.0),   // Orange
            .unbreakable: (0.8, 0.8, 0.8), // Light gray
            .moving: (0.4, 1.0, 0.6),      // Lime green
            .spawning: (1.0, 0.4, 1.0),    // Pink
            .phase: (1.0, 0.8, 0.2),       // Gold
            .boss: (1.0, 0.1, 0.1)         // Red
        ]
    )

    static let sunset = LevelTheme(
        name: "Sunset",
        background: (0.12, 0.06, 0.10),
        paddle: (0.98, 0.90, 0.80),
        accent: (1.00, 0.48, 0.35),
        brickPalette: [
            .normal: (1.0, 0.6, 0.2),
            .reinforced: (1.0, 0.3, 0.5),
            .armored: (0.8, 0.4, 0.8),
            .explosive: (1.0, 0.2, 0.0),
            .unbreakable: (0.7, 0.7, 0.5),
            .moving: (1.0, 0.7, 0.3),
            .spawning: (0.9, 0.5, 0.7),
            .phase: (1.0, 0.9, 0.4),
            .boss: (0.9, 0.1, 0.2)
        ]
    )

    static let cobalt = LevelTheme(
        name: "Cobalt",
        background: (0.05, 0.09, 0.20),
        paddle: (0.70, 0.80, 1.00),
        accent: (0.32, 0.73, 1.00),
        brickPalette: [
            .normal: (0.3, 0.7, 1.0),
            .reinforced: (0.5, 0.5, 1.0),
            .armored: (0.2, 0.9, 0.8),
            .explosive: (1.0, 0.4, 0.6),
            .unbreakable: (0.6, 0.6, 0.8),
            .moving: (0.4, 0.8, 1.0),
            .spawning: (0.7, 0.5, 1.0),
            .phase: (0.8, 0.9, 1.0),
            .boss: (0.1, 0.3, 1.0)
        ]
    )

    static let aurora = LevelTheme(
        name: "Aurora",
        background: (0.06, 0.10, 0.17),
        paddle: (0.50, 1.00, 0.70),
        accent: (0.38, 0.88, 0.66),
        brickPalette: [
            .normal: (0.3, 1.0, 0.5),
            .reinforced: (0.5, 0.8, 1.0),
            .armored: (0.8, 1.0, 0.4),
            .explosive: (1.0, 0.6, 0.3),
            .unbreakable: (0.7, 0.8, 0.6),
            .moving: (0.4, 1.0, 0.8),
            .spawning: (0.9, 0.7, 1.0),
            .phase: (0.9, 1.0, 0.6),
            .boss: (0.2, 0.8, 0.4)
        ]
    )

    static let forest = LevelTheme(
        name: "Forest",
        background: (0.05, 0.11, 0.09),
        paddle: (0.80, 0.90, 0.60),
        accent: (0.35, 0.85, 0.51),
        brickPalette: [
            .normal: (0.4, 0.8, 0.3),
            .reinforced: (0.6, 0.5, 0.8),
            .armored: (0.8, 0.7, 0.2),
            .explosive: (0.9, 0.4, 0.2),
            .unbreakable: (0.6, 0.5, 0.4),
            .moving: (0.5, 0.9, 0.4),
            .spawning: (0.7, 0.6, 0.9),
            .phase: (0.9, 0.8, 0.3),
            .boss: (0.3, 0.6, 0.2)
        ]
    )

    static let lava = LevelTheme(
        name: "Lava",
        background: (0.11, 0.05, 0.05),
        paddle: (1.00, 0.60, 0.30),
        accent: (1.00, 0.43, 0.20),
        brickPalette: [
            .normal: (1.0, 0.4, 0.0),
            .reinforced: (0.8, 0.2, 0.6),
            .armored: (0.9, 0.6, 0.0),
            .explosive: (1.0, 0.1, 0.0),
            .unbreakable: (0.5, 0.3, 0.2),
            .moving: (1.0, 0.5, 0.1),
            .spawning: (0.9, 0.3, 0.7),
            .phase: (1.0, 0.7, 0.1),
            .boss: (0.8, 0.0, 0.0)
        ]
    )
}
