package com.breakoutplus.game

import kotlin.math.abs
import kotlin.math.min

/**
 * Shared collision math/response helpers used by GameEngine.
 * Pure math extraction keeps collision behavior stable while reducing engine size.
 */
object GameCollisionSystem {
    fun circleIntersectsRect(ball: Ball, brick: Brick): Boolean {
        val closestX = ball.x.coerceIn(brick.x, brick.x + brick.width)
        val closestY = ball.y.coerceIn(brick.y, brick.y + brick.height)
        val dx = ball.x - closestX
        val dy = ball.y - closestY
        return dx * dx + dy * dy <= ball.radius * ball.radius
    }

    fun bounceBallFromBrick(ball: Ball, brick: Brick) {
        val overlapLeft = ball.x + ball.radius - brick.x
        val overlapRight = brick.x + brick.width - (ball.x - ball.radius)
        val overlapBottom = ball.y + ball.radius - brick.y
        val overlapTop = brick.y + brick.height - (ball.y - ball.radius)

        val minOverlapX = min(overlapLeft, overlapRight)
        val minOverlapY = min(overlapBottom, overlapTop)

        if (minOverlapX < minOverlapY) {
            if (overlapLeft < overlapRight) {
                ball.x = brick.x - ball.radius
                ball.vx = -abs(ball.vx)
            } else {
                ball.x = brick.x + brick.width + ball.radius
                ball.vx = abs(ball.vx)
            }
        } else {
            if (overlapBottom < overlapTop) {
                ball.y = brick.y - ball.radius
                ball.vy = -abs(ball.vy)
            } else {
                ball.y = brick.y + brick.height + ball.radius
                ball.vy = abs(ball.vy)
            }
        }
    }

    fun beamIntersectsBrick(beam: Beam, brick: Brick): Boolean {
        val beamLeft = beam.x - beam.width / 2f
        val beamRight = beam.x + beam.width / 2f
        val beamBottom = beam.y - beam.height / 2f
        val beamTop = beam.y + beam.height / 2f
        val brickLeft = brick.x
        val brickRight = brick.x + brick.width
        val brickBottom = brick.y
        val brickTop = brick.y + brick.height
        return beamRight > brickLeft && beamLeft < brickRight && beamTop > brickBottom && beamBottom < brickTop
    }
}
