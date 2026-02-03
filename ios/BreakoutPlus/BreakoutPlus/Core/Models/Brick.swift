//
//  Brick.swift
//  BreakoutPlus
//
//  Brick model with destruction logic and visual properties
//

import Foundation
import CoreGraphics

struct Brick: Identifiable, Equatable {
    let id = UUID()
    var x: Float
    var y: Float
    var width: Float
    var height: Float
    var type: BrickType
    var hitPoints: Int
    var maxHitPoints: Int
    var alive = true
    var vx: Float = 0  // For moving bricks
    var vy: Float = 0

    init(x: Float, y: Float, width: Float, height: Float, type: BrickType, hitPoints: Int) {
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.type = type
        self.hitPoints = hitPoints
        self.maxHitPoints = hitPoints

        // Initialize moving bricks with velocity
        if type == .moving {
            self.vx = Float.random(in: -1...1) * 2
        }
    }

    var position: CGPoint {
        get { CGPoint(x: CGFloat(x), y: CGFloat(y)) }
        set { x = Float(newValue.x); y = Float(newValue.y) }
    }

    var bounds: CGRect {
        CGRect(x: CGFloat(x - width/2), y: CGFloat(y - height/2),
               width: CGFloat(width), height: CGFloat(height))
    }

    var centerX: Float {
        x
    }

    var centerY: Float {
        y
    }

    var scoreValue: Int {
        type.scoreValue
    }

    mutating func takeDamage(_ damage: Int = 1) {
        hitPoints = max(0, hitPoints - damage)
        if hitPoints <= 0 {
            alive = false
        }
    }

    func currentColor(theme: LevelTheme) -> (red: Float, green: Float, blue: Float) {
        // Simplified color calculation - full implementation would match Android version
        let baseColor = theme.brickPalette[type] ?? (0.5, 0.5, 0.5)

        let ratio = type == .unbreakable ? 1.0 : Float(hitPoints) / Float(maxHitPoints)
        let r = baseColor.0 * ratio
        let g = baseColor.1 * ratio
        let b = baseColor.2 * ratio

        return (r, g, b)
    }
}