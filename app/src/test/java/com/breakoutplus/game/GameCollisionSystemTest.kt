package com.breakoutplus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCollisionSystemTest {
    @Test
    fun circleIntersectsRectDetectsOverlap() {
        val ball = Ball(x = 10f, y = 10f, radius = 1f, vx = 0f, vy = 0f)
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 9.5f,
            y = 9.5f,
            width = 3f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )
        assertTrue(GameCollisionSystem.circleIntersectsRect(ball, brick))
    }

    @Test
    fun circleIntersectsRectDetectsNoOverlap() {
        val ball = Ball(x = 2f, y = 2f, radius = 0.8f, vx = 0f, vy = 0f)
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = 10f,
            width = 3f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )
        assertFalse(GameCollisionSystem.circleIntersectsRect(ball, brick))
    }

    @Test
    fun bounceBallFromBrickHandlesHorizontalResponse() {
        val ball = Ball(x = 9.5f, y = 11f, radius = 1f, vx = 5f, vy = 0f)
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = 10f,
            width = 4f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )

        GameCollisionSystem.bounceBallFromBrick(ball, brick)

        assertEquals(9f, ball.x, 0.0001f)
        assertEquals(-5f, ball.vx, 0.0001f)
    }

    @Test
    fun bounceBallFromBrickHandlesVerticalResponse() {
        val ball = Ball(x = 12f, y = 9.5f, radius = 1f, vx = 0f, vy = 4f)
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = 10f,
            width = 4f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )

        GameCollisionSystem.bounceBallFromBrick(ball, brick)

        assertEquals(9f, ball.y, 0.0001f)
        assertEquals(-4f, ball.vy, 0.0001f)
    }

    @Test
    fun beamIntersectsBrickDetectsOverlap() {
        val beam = Beam(x = 12f, y = 11f, width = 1f, height = 6f, speed = 40f, color = floatArrayOf(1f, 1f, 1f, 1f))
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = 10f,
            width = 4f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )
        assertTrue(GameCollisionSystem.beamIntersectsBrick(beam, brick))
    }

    @Test
    fun beamIntersectsBrickDetectsNoOverlap() {
        val beam = Beam(x = 2f, y = 2f, width = 1f, height = 2f, speed = 40f, color = floatArrayOf(1f, 1f, 1f, 1f))
        val brick = Brick(
            gridX = 0,
            gridY = 0,
            x = 10f,
            y = 10f,
            width = 4f,
            height = 2f,
            hitPoints = 1,
            maxHitPoints = 1,
            type = BrickType.NORMAL
        )
        assertFalse(GameCollisionSystem.beamIntersectsBrick(beam, brick))
    }
}
