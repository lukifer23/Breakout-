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
}