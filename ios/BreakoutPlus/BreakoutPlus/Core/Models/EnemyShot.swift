//
//  EnemyShot.swift
//  BreakoutPlus
//
//  Enemy shot model for Invaders mode
//

import Foundation
import CoreGraphics

struct EnemyShot: Identifiable, Equatable {
    let id = UUID()
    var x: Float
    var y: Float
    var vx: Float
    var vy: Float
    var radius: Float = 0.8
    var color: (red: Float, green: Float, blue: Float) = (1.0, 0.3, 0.0) // Orange-red

    var position: CGPoint {
        get { CGPoint(x: CGFloat(x), y: CGFloat(y)) }
        set { x = Float(newValue.x); y = Float(newValue.y) }
    }

    func intersects(_ rect: CGRect) -> Bool {
        let shotRect = CGRect(x: CGFloat(x - radius), y: CGFloat(y - radius),
                             width: CGFloat(radius * 2), height: CGFloat(radius * 2))
        return shotRect.intersects(rect)
    }

    func intersects(_ paddle: Paddle) -> Bool {
        intersects(paddle.bounds)
    }

    static func == (lhs: EnemyShot, rhs: EnemyShot) -> Bool {
        return lhs.id == rhs.id &&
               lhs.x == rhs.x &&
               lhs.y == rhs.y &&
               lhs.vx == rhs.vx &&
               lhs.vy == rhs.vy &&
               lhs.radius == rhs.radius
    }
}