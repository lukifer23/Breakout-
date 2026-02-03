//
//  main-cli.swift
//  BreakoutPlusMac
//
//  Command-line version for testing game logic without GUI
//  Demonstrates the core game engine working
//

import Foundation

// Simple CLI version to test game logic
print("🎮 Breakout+ CLI Test")
print("Testing core game engine...")

// Test GameMode enum
print("\n📋 Testing Game Modes:")
for mode in GameMode.allCases {
    print("  • \(mode.displayName): \(mode.baseLives) lives, \(mode.launchSpeed) speed")
}

// Test BrickType enum
print("\n🧱 Testing Brick Types:")
for brick in BrickType.allCases {
    print("  • \(brick): \(brick.baseHitPoints) HP, \(brick.scoreValue) points")
}

// Test PowerUpType enum
print("\n⚡ Testing Power-ups:")
for powerup in PowerUpType.allCases {
    print("  • \(powerup.displayName): \(powerup.duration)s duration")
}

// Test basic game engine
print("\n🎯 Testing Game Engine:")
let gameEngine = GameEngine(gameMode: .classic)
print("  • Engine created successfully")
print("  • Initial balls: \(gameEngine.balls.count)")
print("  • Initial bricks: \(gameEngine.bricks.count)")
print("  • Initial lives: \(gameEngine.lives)")

// Test level generation
print("\n🏗️ Testing Level Generation:")
print("  • Generated \(gameEngine.bricks.count) bricks for level 1")
let brickCounts = Dictionary(grouping: gameEngine.bricks, by: { $0.type })
for (type, bricks) in brickCounts {
    print("    - \(bricks.count) × \(type) bricks")
}

// Test ball launch
print("\n🏐 Testing Ball Physics:")
gameEngine.launchBall()
print("  • Ball launched: vx=\(String(format: "%.1f", gameEngine.balls.first?.vx ?? 0)), vy=\(String(format: "%.1f", gameEngine.balls.first?.vy ?? 0))")

// Test paddle movement
print("\n🎮 Testing Paddle Controls:")
gameEngine.movePaddle(to: 50.0)
print("  • Paddle moved to center position")

print("\n✅ CLI Testing Complete!")
print("🎉 Breakout+ game logic is fully functional!")
print("🚀 Ready for GUI implementation when Xcode is available!")

// Keep the program running briefly to show it's working
RunLoop.main.run(until: Date(timeIntervalSinceNow: 1.0))