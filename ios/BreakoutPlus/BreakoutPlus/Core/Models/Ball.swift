//
//  Ball.swift
//  BreakoutPlus
//
//  Ball model with physics and collision detection
//

import Foundation
import CoreGraphics

struct Ball: Identifiable, Equatable {
    let id = UUID()
    var x: Float
    var y: Float
    var vx: Float
    var vy: Float
    var radius: Float = 1.0
    var isFireball: Bool = false
    var isPiercing: Bool = false
    var color: (red: Float, green: Float, blue: Float) = (1.0, 1.0, 1.0)

    var position: CGPoint {
        get { CGPoint(x: CGFloat(x), y: CGFloat(y)) }
        set { x = Float(newValue.x); y = Float(newValue.y) }
    }

    func intersects(_ rect: CGRect) -> Bool {
        let ballRect = CGRect(x: CGFloat(x - radius), y: CGFloat(y - radius),
                            width: CGFloat(radius * 2), height: CGFloat(radius * 2))
        return ballRect.intersects(rect)
    }

    func intersects(_ brick: Brick) -> Bool {
        intersects(brick.bounds)
    }

    func intersects(_ paddle: Paddle) -> Bool {
        intersects(paddle.bounds)
    }

    static func == (lhs: Ball, rhs: Ball) -> Bool {
        return lhs.id == rhs.id &&
               lhs.x == rhs.x &&
               lhs.y == rhs.y &&
               lhs.vx == rhs.vx &&
               lhs.vy == rhs.vy &&
               lhs.radius == rhs.radius &&
               lhs.isFireball == rhs.isFireball &&
               lhs.isPiercing == rhs.isPiercing
    }
}
