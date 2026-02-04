//
//  Paddle.swift
//  BreakoutPlus
//
//  Paddle model with collision detection
//

import Foundation
import CoreGraphics

struct Paddle: Equatable {
    var x: Float
    var y: Float
    var width: Float
    var height: Float

    var position: CGPoint {
        get { CGPoint(x: CGFloat(x), y: CGFloat(y)) }
        set { x = Float(newValue.x); y = Float(newValue.y) }
    }

    var bounds: CGRect {
        CGRect(x: CGFloat(x - width/2), y: CGFloat(y - height/2),
               width: CGFloat(width), height: CGFloat(height))
    }

    var left: Float {
        x - width/2
    }

    var right: Float {
        x + width/2
    }

    var top: Float {
        y + height/2
    }

    var bottom: Float {
        y - height/2
    }

    static func == (lhs: Paddle, rhs: Paddle) -> Bool {
        return lhs.x == rhs.x &&
               lhs.y == rhs.y &&
               lhs.width == rhs.width &&
               lhs.height == rhs.height
    }
}
