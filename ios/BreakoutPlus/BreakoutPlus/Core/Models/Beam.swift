//
//  Beam.swift
//  BreakoutPlus
//
//  Laser beam projectile fired by the paddle.
//

import Foundation
import CoreGraphics

struct Beam: Identifiable, Equatable {
    let id = UUID()
    var x: Float
    var y: Float
    var width: Float
    var height: Float
    var vy: Float

    var bounds: CGRect {
        CGRect(
            x: CGFloat(x - width / 2),
            y: CGFloat(y - height / 2),
            width: CGFloat(width),
            height: CGFloat(height)
        )
    }

    func intersects(_ brick: Brick) -> Bool {
        bounds.intersects(brick.bounds)
    }
}

