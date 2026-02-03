//
//  PowerUp.swift
//  BreakoutPlus
//
//  Powerup model with falling physics
//

import Foundation
import CoreGraphics

struct PowerUp: Identifiable, Equatable {
    let id = UUID()
    var x: Float
    var y: Float
    var type: PowerUpType
    var vx: Float = 0
    var vy: Float = 3  // Falling speed
    var width: Float = 4
    var height: Float = 4

    var position: CGPoint {
        get { CGPoint(x: CGFloat(x), y: CGFloat(y)) }
        set { x = Float(newValue.x); y = Float(newValue.y) }
    }

    var bounds: CGRect {
        CGRect(x: CGFloat(x - width/2), y: CGFloat(y - height/2),
               width: CGFloat(width), height: CGFloat(height))
    }

    func intersects(_ paddle: Paddle) -> Bool {
        bounds.intersects(paddle.bounds)
    }
}