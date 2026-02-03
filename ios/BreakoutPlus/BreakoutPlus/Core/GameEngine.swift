//
//  GameEngine.swift
//  BreakoutPlus
//
//  Core game engine handling physics, collisions, powerups, and level progression.
//  Direct port from Android GameEngine.kt
//

import Foundation
import CoreGraphics

protocol GameEngineDelegate: AnyObject {
    func onScoreChanged(newScore: Int)
    func onLivesChanged(newLives: Int)
    func onLevelChanged(newLevel: Int)
    func onPowerupActivated(type: PowerUpType)
    func onComboAchieved(count: Int)
    func onTimeUpdated(seconds: Int)
    func onLevelComplete()
    func onGameOver(finalScore: Int)
}

class GameEngine {
    // Game state
    private(set) var balls: [Ball] = []
    private(set) var bricks: [Brick] = []
    private(set) var powerups: [PowerUp] = []
    private(set) var paddle: Paddle

    // Game properties
    private(set) var score = 0
    private(set) var lives = 3
    private(set) var levelIndex = 0
    private(set) var timeRemaining = 0
    private(set) var comboCount = 0
    private(set) var comboTimer: TimeInterval = 0

    // Settings
    private(set) var gameMode: GameMode
    private(set) var soundEnabled = true
    private(set) var vibrationEnabled = true

    // Active effects
    private var activeEffects: [PowerUpType: TimeInterval] = [:]
    private var speedMultiplier = 1.0
    private var lastUpdateTime: TimeInterval = 0

    weak var delegate: GameEngineDelegate?

    init(gameMode: GameMode) {
        self.gameMode = gameMode
        self.paddle = Paddle(x: 50, y: 10, width: 18, height: 2.6)
        self.lives = gameMode.baseLives
        self.timeRemaining = gameMode.timeLimitSeconds

        resetLevel()
    }

    func update(currentTime: TimeInterval) {
        let deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        let dt = deltaTime * speedMultiplier

        // Update timers
        updateTimers(dt)

        // Update game objects
        updateBalls(dt)
        updateBricks(dt)
        updatePowerups(dt)
        updatePaddle(dt)

        // Handle collisions
        handleCollisions()

        // Check win/lose conditions
        checkLevelCompletion()
        checkGameOver()
    }

    private func updateTimers(_ deltaTime: TimeInterval) {
        // Update powerup timers
        activeEffects = activeEffects.filter { (type, remaining) in
            let newRemaining = remaining - deltaTime
            if newRemaining <= 0 {
                deactivatePowerup(type)
                return false
            }
            activeEffects[type] = newRemaining
            return true
        }

        // Update combo timer
        if comboTimer > 0 {
            comboTimer -= deltaTime
            if comboTimer <= 0 {
                comboCount = 0
            }
        }

        // Update game timer
        if gameMode.timeLimitSeconds > 0 && timeRemaining > 0 {
            timeRemaining = max(0, timeRemaining - 1) // Convert to seconds
            delegate?.onTimeUpdated(seconds: timeRemaining)
        }
    }

    private func updateBalls(_ deltaTime: TimeInterval) {
        for i in 0..<balls.count {
            var ball = balls[i]
            // Update position
            ball.x += ball.vx * Float(deltaTime)
            ball.y += ball.vy * Float(deltaTime)

            // Boundary collisions
            if ball.x - ball.radius <= 0 || ball.x + ball.radius >= 100 {
                ball.vx = -ball.vx
                ball.x = max(ball.radius, min(100 - ball.radius, ball.x))
            }

            if ball.y + ball.radius >= 160 { // Bottom - lose life
                removeBall(ball)
            }

            if ball.y - ball.radius <= 0 { // Top
                ball.vy = -ball.vy
                ball.y = ball.radius
            }
            balls[i] = ball
        }
    }

    private func updateBricks(_ deltaTime: TimeInterval) {
        for i in 0..<bricks.count where bricks[i].alive {
            var brick = bricks[i]
            if brick.type == .moving {
                // Horizontal movement
                brick.x += brick.vx * Float(deltaTime)

                // Bounce off walls
                if brick.x <= 0 || brick.x + brick.width >= 100 {
                    brick.vx = -brick.vx
                }
            }
            bricks[i] = brick
        }
    }

    private func updatePowerups(_ deltaTime: TimeInterval) {
        for i in 0..<powerups.count {
            var powerup = powerups[i]
            powerup.y += powerup.vy * Float(deltaTime)

            // Remove if off screen
            if powerup.y > 160 {
                powerups.remove(at: i)
                break
            }
            powerups[i] = powerup
        }
    }

    private func updatePaddle(_ deltaTime: TimeInterval) {
        // Paddle physics/constraints can be added here
    }

    private func handleCollisions() {
        // Ball-brick collisions
        for ballIndex in 0..<balls.count {
            var ball = balls[ballIndex]
            for brickIndex in 0..<bricks.count where bricks[brickIndex].alive {
                var brick = bricks[brickIndex]
                if ball.intersects(brick) {
                    handleBrickCollision(&ball, &brick)
                    balls[ballIndex] = ball
                    bricks[brickIndex] = brick
                }
            }
        }

        // Ball-paddle collisions
        for ballIndex in 0..<balls.count {
            var ball = balls[ballIndex]
            if ball.intersects(paddle) {
                handlePaddleCollision(&ball)
                balls[ballIndex] = ball
            }
        }

        // Powerup-paddle collisions
        for powerup in powerups {
            if powerup.intersects(paddle) {
                collectPowerup(powerup)
            }
        }
    }

    private func handleBrickCollision(_ ball: inout Ball, _ brick: inout Brick) {
        // Damage brick
        brick.hitPoints -= 1

        if brick.hitPoints <= 0 {
            brick.alive = false
            score += brick.scoreValue

            // Combo system
            comboCount += 1
            comboTimer = 2.0 // 2 seconds to maintain combo
            delegate?.onComboAchieved(count: comboCount)

            // Create powerup (10% chance)
            if Double.random(in: 0..<1) < 0.1 {
                spawnPowerup(atX: brick.centerX, y: brick.centerY)
            }
        }

        // Bounce ball
        ball.vy = -ball.vy
    }

    private func handlePaddleCollision(_ ball: inout Ball) {
        // Calculate bounce angle based on hit position
        let hitPos = (ball.x - paddle.x) / paddle.width
        let angle = hitPos * .pi / 3 // Max 60 degrees

        let speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
        ball.vx = speed * sin(angle)
        ball.vy = -abs(speed * cos(angle)) // Always bounce up
    }

    private func collectPowerup(_ powerup: PowerUp) {
        activatePowerup(powerup.type)

        if let index = powerups.firstIndex(of: powerup) {
            powerups.remove(at: index)
        }
    }

    private func activatePowerup(_ type: PowerUpType) {
        delegate?.onPowerupActivated(type: type)
        activeEffects[type] = type.duration

        switch type {
        case .multiBall:
            // Spawn additional balls
            for _ in 0..<2 {
                let newBall = Ball(x: paddle.x, y: paddle.y + paddle.height / 2 + 1.0 + 0.5,
                                  vx: Float.random(in: -2...2), vy: Float.random(in: -3 ... -7))
                balls.append(newBall)
            }
        case .widePaddle:
            paddle.width = 25
        case .slowMotion:
            speedMultiplier = 0.7
        case .extraLife:
            lives += 1
            delegate?.onLivesChanged(newLives: lives)
        default:
            break
        }
    }

    private func deactivatePowerup(_ type: PowerUpType) {
        switch type {
        case .widePaddle:
            paddle.width = 18
        case .slowMotion:
            speedMultiplier = 1.0
        default:
            break
        }
    }

    private func spawnPowerup(atX x: Float, y: Float) {
        let randomType = PowerUpType.allCases.randomElement()!
        let powerup = PowerUp(x: x, y: y, type: randomType)
        powerups.append(powerup)
    }

    private func checkLevelCompletion() {
        let remainingBricks = bricks.filter { $0.alive && $0.type != .unbreakable }.count
        if remainingBricks == 0 {
            delegate?.onLevelComplete()
        }
    }

    private func checkGameOver() {
        if balls.isEmpty && lives > 0 {
            lives -= 1
            delegate?.onLivesChanged(newLives: lives)

            if lives > 0 {
                spawnBall()
            } else {
                delegate?.onGameOver(finalScore: score)
            }
        }
    }

    private func spawnBall() {
        let ball = Ball(x: paddle.x, y: paddle.y + paddle.height / 2 + 1.0 + 0.5, vx: 0, vy: 0)
        balls.append(ball)
    }

    private func removeBall(_ ball: Ball) {
        if let index = balls.firstIndex(where: { $0.id == ball.id }) {
            balls.remove(at: index)
        }
    }

    func launchBall() {
        guard let index = balls.firstIndex(where: { $0.vy == 0 }), index < balls.count else { return }

        var ball = balls[index]
        let angle = Double.random(in: .pi/6...(5 * .pi / 6)) // 30-150 degrees
        let speed = Double(gameMode.launchSpeed)
        ball.vx = Float(speed * cos(angle))
        ball.vy = Float(speed * -sin(angle)) // Negative for up
        balls[index] = ball
    }

    func movePaddle(to x: Float) {
        paddle.x = max(0, min(100 - paddle.width, x))
    }

    private func resetLevel() {
        balls.removeAll()
        bricks.removeAll()
        powerups.removeAll()
        comboCount = 0
        comboTimer = 0

        // Generate level (simplified for now)
        generateLevel()

        spawnBall()
    }

    private func generateLevel() {
        // Simple level generation - 8x10 grid
        let rows = 8
        let cols = 10
        let brickWidth: Float = 100.0 / Float(cols)
        let brickHeight: Float = 6.0

        for row in 0..<rows {
            for col in 0..<cols {
                let brickType: BrickType
                switch row {
                case 0: brickType = .normal
                case 1: brickType = .reinforced
                case 2: brickType = .armored
                case 3: brickType = .explosive
                case 4: brickType = .moving
                case 5: brickType = .spawning
                case 6: brickType = .phase
                case 7: brickType = .boss
                default: brickType = .normal
                }

                let x = Float(col) * brickWidth + brickWidth/2
                let y = Float(row) * brickHeight + 20

                let brick = Brick(x: x, y: y, width: brickWidth - 1, height: brickHeight - 1,
                                type: brickType, hitPoints: brickType.baseHitPoints)
                bricks.append(brick)
            }
        }
    }

    func nextLevel() {
        levelIndex += 1
        delegate?.onLevelChanged(newLevel: levelIndex + 1)
        resetLevel()
    }
}