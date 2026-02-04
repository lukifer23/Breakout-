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
    func onTip(message: String)
    func onLevelComplete(summary: GameSummary)
    func onGameOver(summary: GameSummary)
    func onFeedback(_ event: GameFeedbackEvent)
}

enum GameFeedbackEvent: Equatable {
    case sound(GameSound, volume: Float)
    case haptic(HapticEvent)
}

extension GameEngineDelegate {
    func onFeedback(_ event: GameFeedbackEvent) {}
}

struct GameSummary: Equatable {
    let score: Int
    let level: Int
    let durationSeconds: Int
}

indirect enum GameState: Equatable {
    case ready
    case running
    case paused(previous: GameState)
    case levelComplete
    case gameOver
}

class GameEngine {
    // Coordinate system:
    // World is 100 units wide and `worldHeight` units tall.
    // Y increases upward (matching SpriteKit + Android implementation).
    private(set) var worldWidth: Float = 100
    private(set) var worldHeight: Float = 160

    // Game state
    private(set) var balls: [Ball] = []
    private(set) var bricks: [Brick] = []
    private(set) var powerups: [PowerUp] = []
    private(set) var beams: [Beam] = []
    private(set) var paddle: Paddle
    private(set) var currentTheme: LevelTheme = .neon
    private(set) var currentTip: String = ""

    // Game properties
    private(set) var score = 0
    private(set) var lives = 3
    private(set) var levelIndex = 0
    private(set) var timeRemaining = 0
    private(set) var comboCount = 0
    private(set) var comboTimer: TimeInterval = 0
    private(set) var state: GameState = .ready

    // Settings
    private(set) var gameMode: GameMode
    private(set) var soundEnabled = true
    private(set) var vibrationEnabled = true

    // Active effects
    private var activeEffects: [PowerUpType: TimeInterval] = [:]
    private var speedMultiplier = 1.0
    private var timerAccumulator: TimeInterval = 0
    private var elapsedSeconds: TimeInterval = 0
    private var guardrailActive = false
    private var shieldCharges = 0
    private var laserCooldown: TimeInterval = 0
    private var magnetActive = false
    private var gravityWellActive = false
    private var freezeActive = false
    private var pierceActive = false
    private var feedbackCooldown: TimeInterval = 0

    weak var delegate: GameEngineDelegate?

    init(gameMode: GameMode) {
        self.gameMode = gameMode
        self.paddle = Paddle(x: 50, y: 8, width: 18, height: 2.6)
        self.lives = gameMode.baseLives
        self.timeRemaining = gameMode.timeLimitSeconds

        resetLevel()
    }

    func update(deltaTime: TimeInterval) {
        switch state {
        case .paused, .gameOver, .levelComplete:
            return
        case .ready, .running:
            break
        }

        let dtRaw = max(0.0, min(0.05, deltaTime))
        if laserCooldown > 0 {
            laserCooldown = max(0, laserCooldown - dtRaw)
        }
        if feedbackCooldown > 0 {
            feedbackCooldown = max(0, feedbackCooldown - dtRaw)
        }
        elapsedSeconds += dtRaw

        let dt = dtRaw * speedMultiplier

        // Update timers
        updateTimers(dt)

        // Update game objects
        if state == .ready {
            attachReadyBallsToPaddle()
        }
        updateBalls(dt)
        updateBricks(dt)
        updatePowerups(dt)
        updateBeams(dt)
        updatePaddle(dt)

        // Handle collisions
        handleCollisions()

        // Check win/lose conditions
        checkLevelCompletion()
        checkGameOver()
    }

    private func attachReadyBallsToPaddle() {
        if balls.isEmpty {
            spawnBall()
            return
        }
        for i in 0..<balls.count {
            if balls[i].vy == 0 {
                balls[i].x = paddle.x
                balls[i].y = paddle.y + paddle.height / 2 + balls[i].radius + 0.7
                balls[i].vx = 0
            }
        }
    }

    private func updateTimers(_ deltaTime: TimeInterval) {
        // Update powerup timers
        var nextEffects: [PowerUpType: TimeInterval] = [:]
        nextEffects.reserveCapacity(activeEffects.count)
        for (type, remaining) in activeEffects {
            let newRemaining = remaining - deltaTime
            if newRemaining <= 0 {
                deactivatePowerup(type)
            } else {
                nextEffects[type] = newRemaining
            }
        }
        activeEffects = nextEffects

        // Update combo timer
        if comboTimer > 0 {
            comboTimer -= deltaTime
            if comboTimer <= 0 {
                comboCount = 0
            }
        }

        // Update game timer
        if gameMode.timeLimitSeconds > 0 && timeRemaining > 0 {
            timerAccumulator += deltaTime
            if timerAccumulator >= 1.0 {
                let ticks = Int(timerAccumulator)
                timerAccumulator -= Double(ticks)
                let newRemaining = max(0, timeRemaining - ticks)
                if newRemaining != timeRemaining {
                    timeRemaining = newRemaining
                    delegate?.onTimeUpdated(seconds: timeRemaining)
                }
            }
        }
    }

    private func updateBalls(_ deltaTime: TimeInterval) {
        var toRemove: [Int] = []
        toRemove.reserveCapacity(2)

        for i in 0..<balls.count {
            var ball = balls[i]

            // Update position
            ball.x += ball.vx * Float(deltaTime)
            ball.y += ball.vy * Float(deltaTime)

            if gravityWellActive {
                let cx: Float = worldWidth / 2
                let cy: Float = worldHeight * 0.55
                let dx = cx - ball.x
                let dy = cy - ball.y
                let dist2 = max(40, dx * dx + dy * dy)
                let strength: Float = 650.0
                ball.vx += dx / dist2 * strength * Float(deltaTime)
                ball.vy += dy / dist2 * strength * Float(deltaTime)
            }

            // Left/right walls
            if ball.x - ball.radius <= 0 || ball.x + ball.radius >= worldWidth {
                ball.vx = -ball.vx
                ball.x = max(ball.radius, min(worldWidth - ball.radius, ball.x))
                emit(.sound(.bounce, volume: 0.45))
            }

            // Bottom - lose life
            if ball.y - ball.radius <= 0 {
                if guardrailActive {
                    ball.vy = abs(ball.vy)
                    ball.y = ball.radius + 0.1
                    emit(.sound(.bounce, volume: 0.55))
                    emit(.haptic(.light))
                } else if shieldCharges > 0 {
                    shieldCharges -= 1
                    ball.vy = max(abs(ball.vy), Float(gameMode.launchSpeed * 0.8))
                    ball.y = paddle.y + paddle.height / 2 + ball.radius + 0.5
                    ball.x = paddle.x
                    emit(.sound(.powerup, volume: 0.45))
                    emit(.haptic(.medium))
                } else {
                    toRemove.append(i)
                }
            }

            // Top
            if ball.y + ball.radius >= worldHeight {
                ball.vy = -ball.vy
                ball.y = worldHeight - ball.radius
                emit(.sound(.bounce, volume: 0.35))
            }

            balls[i] = ball
        }

        if !toRemove.isEmpty {
            for idx in toRemove.sorted(by: >) {
                if idx >= 0 && idx < balls.count {
                    balls.remove(at: idx)
                }
            }
        }
    }

    private func updateBricks(_ deltaTime: TimeInterval) {
        for i in 0..<bricks.count where bricks[i].alive {
            var brick = bricks[i]
            if brick.type == .moving {
                // Horizontal movement
                brick.x += brick.vx * Float(deltaTime)

                // Bounce off walls
                if brick.x - brick.width / 2 <= 0 || brick.x + brick.width / 2 >= worldWidth {
                    brick.vx = -brick.vx
                }
            }
            bricks[i] = brick
        }
    }

    private func updatePowerups(_ deltaTime: TimeInterval) {
        if powerups.isEmpty { return }
        for i in stride(from: powerups.count - 1, through: 0, by: -1) {
            var powerup = powerups[i]
            if magnetActive {
                let dx = paddle.x - powerup.x
                powerup.vx += dx * 0.7 * Float(deltaTime)
            }
            powerup.x += powerup.vx * Float(deltaTime)
            powerup.y += powerup.vy * Float(deltaTime)

            // Remove if off screen (below bottom)
            if powerup.y + powerup.height / 2 < 0 {
                powerups.remove(at: i)
                continue
            }
            powerups[i] = powerup
        }
    }

    private func updateBeams(_ deltaTime: TimeInterval) {
        if beams.isEmpty { return }
        for i in stride(from: beams.count - 1, through: 0, by: -1) {
            var beam = beams[i]
            beam.y += beam.vy * Float(deltaTime)
            if beam.y - beam.height / 2 > worldHeight {
                beams.remove(at: i)
                continue
            }
            beams[i] = beam
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

        // Beam-brick collisions
        if !beams.isEmpty && !bricks.isEmpty {
            for beamIndex in stride(from: beams.count - 1, through: 0, by: -1) {
                let beam = beams[beamIndex]
                var consumed = false
                for brickIndex in 0..<bricks.count where bricks[brickIndex].alive {
                    var brick = bricks[brickIndex]
                    if beam.intersects(brick) {
                        handleBeamHitBrick(&brick)
                        bricks[brickIndex] = brick
                        consumed = true
                        break
                    }
                }
                if consumed {
                    beams.remove(at: beamIndex)
                }
            }
        }

        // Powerup-paddle collisions
        if !powerups.isEmpty {
            for i in stride(from: powerups.count - 1, through: 0, by: -1) {
                let powerup = powerups[i]
                if powerup.intersects(paddle) {
                    activatePowerup(powerup.type)
                    powerups.remove(at: i)
                }
            }
        }
    }

    private func handleBrickCollision(_ ball: inout Ball, _ brick: inout Brick) {
        // Resolve collision response first so the ball doesn't tunnel into bricks.
        resolveBallBrickCollision(&ball, brick)

        // Unbreakable bricks ignore normal hits.
        if brick.type == .unbreakable && !ball.isFireball {
            emit(.sound(.bounce, volume: 0.35))
            return
        }

        let wasAlive = brick.alive
        if brick.type == .unbreakable && ball.isFireball {
            brick.hitPoints = 0
        } else {
            brick.hitPoints -= 1
        }

        if brick.hitPoints <= 0 {
            brick.alive = false
            score += brick.scoreValue
            delegate?.onScoreChanged(newScore: score)

            // Combo system
            comboCount += 1
            comboTimer = 2.0 // 2 seconds to maintain combo
            delegate?.onComboAchieved(count: comboCount)

            // Create powerup (10% chance)
            if Double.random(in: 0..<1) < 0.1 {
                spawnPowerup(atX: brick.centerX, y: brick.centerY)
            }

            if brick.type == .explosive || brick.type == .boss {
                emit(.sound(.explosion, volume: 0.85))
                emit(.haptic(.heavy))
            } else {
                emit(.sound(.brick, volume: 0.8))
                emit(.haptic(.medium))
            }
        } else {
            emit(.sound(.brick, volume: 0.55))
            emit(.haptic(.light))
        }

        if wasAlive && !brick.alive {
            applyBrickOnDestroyEffects(brick)
        }
    }

    private func handlePaddleCollision(_ ball: inout Ball) {
        // Calculate bounce angle based on hit position
        let hitPos = (ball.x - paddle.x) / (paddle.width / 2) // -1..1 across paddle
        let angle = hitPos * .pi / 3 // Max 60 degrees

        let speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
        ball.vx = speed * sin(angle)
        ball.vy = abs(speed * cos(angle)) // Always bounce up (positive Y)

        // Prevent the "stuck in paddle" case by placing the ball just above the paddle.
        ball.y = paddle.y + paddle.height / 2 + ball.radius + 0.05
        emit(.sound(.bounce, volume: 0.65))
        emit(.haptic(.light))
    }

    private func resolveBallBrickCollision(_ ball: inout Ball, _ brick: Brick) {
        // Axis-aligned resolution using overlap against expanded brick bounds.
        let bx0 = brick.x - brick.width / 2 - ball.radius
        let bx1 = brick.x + brick.width / 2 + ball.radius
        let by0 = brick.y - brick.height / 2 - ball.radius
        let by1 = brick.y + brick.height / 2 + ball.radius

        if ball.x < bx0 || ball.x > bx1 || ball.y < by0 || ball.y > by1 {
            return
        }

        // Fireball / piercing balls pass through bricks (damage without reflection).
        if ball.isFireball || ball.isPiercing {
            return
        }

        let leftPen = abs(ball.x - bx0)
        let rightPen = abs(bx1 - ball.x)
        let bottomPen = abs(ball.y - by0)
        let topPen = abs(by1 - ball.y)

        let minXPen = min(leftPen, rightPen)
        let minYPen = min(bottomPen, topPen)

        if minXPen < minYPen {
            ball.vx = -ball.vx
        } else {
            ball.vy = -ball.vy
        }
    }

    private func applyBrickOnDestroyEffects(_ destroyed: Brick) {
        switch destroyed.type {
        case .explosive:
            // Damage neighbors in a small radius.
            for i in 0..<bricks.count where bricks[i].alive {
                if bricks[i].id == destroyed.id { continue }
                let dx = abs(bricks[i].x - destroyed.x)
                let dy = abs(bricks[i].y - destroyed.y)
                if dx <= destroyed.width * 1.2 && dy <= destroyed.height * 1.2 {
                    bricks[i].hitPoints -= 1
                    if bricks[i].hitPoints <= 0 {
                        bricks[i].alive = false
                        score += bricks[i].scoreValue
                    }
                }
            }
            delegate?.onScoreChanged(newScore: score)
        case .spawning:
            // Split into two smaller bricks.
            let childW = max(2.5, destroyed.width * 0.48)
            let childH = max(2.5, destroyed.height * 0.85)
            let dx = childW * 0.55
            let left = Brick(x: destroyed.x - dx, y: destroyed.y, width: childW, height: childH, type: .normal, hitPoints: 1)
            let right = Brick(x: destroyed.x + dx, y: destroyed.y, width: childW, height: childH, type: .normal, hitPoints: 1)
            bricks.append(left)
            bricks.append(right)
        case .phase:
            if Double.random(in: 0..<1) < 0.25 {
                spawnPowerup(atX: destroyed.x, y: destroyed.y)
            }
        case .boss:
            spawnPowerup(atX: destroyed.x, y: destroyed.y)
        default:
            break
        }
    }

    private func handleBeamHitBrick(_ brick: inout Brick) {
        emit(.sound(.brick, volume: 0.45))
        if brick.type == .unbreakable {
            brick.hitPoints = max(0, brick.hitPoints - 20)
        } else {
            brick.hitPoints -= 1
        }
        if brick.hitPoints <= 0 {
            brick.alive = false
            score += brick.scoreValue
            delegate?.onScoreChanged(newScore: score)
            if Double.random(in: 0..<1) < 0.15 {
                spawnPowerup(atX: brick.centerX, y: brick.centerY)
            }
            applyBrickOnDestroyEffects(brick)
        }
    }

    private func activatePowerup(_ type: PowerUpType) {
        delegate?.onPowerupActivated(type: type)
        if !type.isInstant {
            activeEffects[type] = type.duration
        }
        emit(.sound(.powerup, volume: 0.8))
        emit(.haptic(.success))

        switch type {
        case .multiBall:
            // Spawn additional balls
            for _ in 0..<2 {
                let angle = Double.random(in: .pi/6...(5 * .pi / 6)) // 30-150 deg
                let speed = Double(gameMode.launchSpeed) * 0.75
                let newBall = Ball(
                    x: paddle.x,
                    y: paddle.y + paddle.height / 2 + 1.0 + 1.0,
                    vx: Float(speed * cos(angle)),
                    vy: Float(abs(speed * sin(angle)))
                )
                balls.append(newBall)
            }
        case .laser:
            // Active effect; fired via `fireLasers()`.
            break
        case .guardrail:
            guardrailActive = true
        case .shield:
            shieldCharges = min(2, shieldCharges + 1)
        case .widePaddle:
            paddle.width = 25
        case .slowMotion:
            speedMultiplier = 0.7
        case .freeze:
            freezeActive = true
            speedMultiplier = 0.12
        case .pierce:
            pierceActive = true
            for i in 0..<balls.count { balls[i].isPiercing = true }
        case .fireball:
            for i in 0..<balls.count { balls[i].isFireball = true }
        case .magnet:
            magnetActive = true
        case .gravityWell:
            gravityWellActive = true
        case .ballSplitter:
            let existing = balls
            for ball in existing {
                for _ in 0..<2 {
                    let angle = Double.random(in: .pi/6...(5 * .pi / 6))
                    let speed = Double(gameMode.launchSpeed) * 0.7
                    var newBall = Ball(
                        x: ball.x,
                        y: ball.y,
                        vx: Float(speed * cos(angle)),
                        vy: Float(abs(speed * sin(angle)))
                    )
                    newBall.isFireball = ball.isFireball
                    newBall.isPiercing = ball.isPiercing
                    balls.append(newBall)
                }
            }
        case .extraLife:
            if !gameMode.godMode {
                lives += 1
                delegate?.onLivesChanged(newLives: lives)
                emit(.sound(.life, volume: 0.9))
                emit(.haptic(.success))
            }
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
        case .guardrail:
            guardrailActive = false
        case .shield:
            shieldCharges = 0
        case .freeze:
            freezeActive = false
            speedMultiplier = 1.0
        case .pierce:
            pierceActive = false
            for i in 0..<balls.count { balls[i].isPiercing = false }
        case .fireball:
            for i in 0..<balls.count { balls[i].isFireball = false }
        case .magnet:
            magnetActive = false
        case .gravityWell:
            gravityWellActive = false
        default:
            break
        }
    }

    private func spawnPowerup(atX x: Float, y: Float) {
        guard let randomType = PowerUpType.allCases.randomElement() else { return }
        let powerup = PowerUp(x: x, y: y, type: randomType)
        powerups.append(powerup)
    }

    private func checkLevelCompletion() {
        let remainingBricks = bricks.filter { $0.alive && $0.type != .unbreakable }.count
        if remainingBricks == 0 {
            state = .levelComplete
            emit(.sound(.powerup, volume: 0.6))
            emit(.haptic(.success))
            delegate?.onLevelComplete(summary: makeSummary())
        }
    }

    private func checkGameOver() {
        if balls.isEmpty && gameMode.godMode {
            spawnBall()
            return
        }

        if balls.isEmpty && lives > 0 {
            lives -= 1
            delegate?.onLivesChanged(newLives: lives)
            emit(.sound(.life, volume: 0.7))
            emit(.haptic(.warning))

            if lives > 0 {
                spawnBall()
            } else {
                state = .gameOver
                emit(.sound(.gameOver, volume: 0.9))
                emit(.haptic(.error))
                delegate?.onGameOver(summary: makeSummary())
            }
        }
    }

    private func spawnBall() {
        let ball = Ball(
            x: paddle.x,
            y: paddle.y + paddle.height / 2 + 1.0 + 1.0,
            vx: 0,
            vy: 0
        )
        balls.append(ball)
    }

    func launchBall() {
        guard let index = balls.firstIndex(where: { $0.vy == 0 }), index < balls.count else { return }

        var ball = balls[index]
        let angle = Double.random(in: .pi/6...(5 * .pi / 6)) // 30-150 degrees
        let speed = Double(gameMode.launchSpeed)
        ball.vx = Float(speed * cos(angle))
        ball.vy = Float(abs(speed * sin(angle))) // Always up
        balls[index] = ball
        if state == .ready {
            state = .running
        }
    }

    func movePaddle(to x: Float) {
        let half = paddle.width / 2
        paddle.x = max(half, min(worldWidth - half, x))
    }

    private func resetLevel() {
        balls.removeAll()
        bricks.removeAll()
        powerups.removeAll()
        beams.removeAll()
        comboCount = 0
        comboTimer = 0
        activeEffects.removeAll()
        speedMultiplier = 1.0
        timerAccumulator = 0
        guardrailActive = false
        shieldCharges = 0
        laserCooldown = 0
        magnetActive = false
        gravityWellActive = false
        freezeActive = false
        pierceActive = false
        state = .ready
        if gameMode.rush {
            timeRemaining = gameMode.timeLimitSeconds
        }

        // Generate level (simplified for now)
        generateLevel()
        if currentTip.isEmpty {
            delegate?.onTip(message: levelIndex == 0 ? "Tip: Release to launch. Hit the paddle edge to steer." : "Tip: Stack powerups for huge combos.")
        } else {
            delegate?.onTip(message: currentTip)
        }

        spawnBall()
    }

    private func generateLevel() {
        let built = LevelFactory.buildLevel(
            index: levelIndex,
            worldWidth: worldWidth,
            worldHeight: worldHeight,
            endless: gameMode.endless
        )
        bricks = built.bricks
        currentTheme = built.theme
        currentTip = built.tip
    }

    func nextLevel() {
        levelIndex += 1
        delegate?.onLevelChanged(newLevel: levelIndex + 1)
        resetLevel()
    }

    func restart() {
        score = 0
        elapsedSeconds = 0
        levelIndex = 0
        lives = gameMode.baseLives
        timeRemaining = gameMode.timeLimitSeconds
        delegate?.onScoreChanged(newScore: score)
        delegate?.onLivesChanged(newLives: lives)
        delegate?.onLevelChanged(newLevel: 1)
        resetLevel()
    }

    func pause() {
        if case .paused = state { return }
        state = .paused(previous: state)
    }

    func resume() {
        if case .paused(let previous) = state {
            state = previous
        }
    }

    func fireLasers() {
        guard activeEffects.keys.contains(.laser) else { return }
        if laserCooldown > 0 { return }
        laserCooldown = 0.18
        let y = paddle.y + paddle.height / 2 + 0.6
        let offset = paddle.width * 0.28
        beams.append(Beam(x: paddle.x - offset, y: y, width: 0.6, height: 8.0, vy: 120))
        beams.append(Beam(x: paddle.x + offset, y: y, width: 0.6, height: 8.0, vy: 120))
        emit(.sound(.laser, volume: 0.75))
        emit(.haptic(.light))
    }

    private func makeSummary() -> GameSummary {
        GameSummary(score: score, level: levelIndex + 1, durationSeconds: Int(elapsedSeconds.rounded(.down)))
    }

    private func emit(_ event: GameFeedbackEvent) {
        // Haptics can be fatiguing; throttle slightly.
        switch event {
        case .haptic:
            if feedbackCooldown > 0 { return }
            feedbackCooldown = 0.03
        case .sound:
            break
        }
        delegate?.onFeedback(event)
    }
}
