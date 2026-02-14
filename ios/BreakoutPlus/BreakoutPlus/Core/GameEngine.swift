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
    func onPowerupStatusUpdated(status: [PowerupStatus])
    func onFeedback(_ event: GameFeedbackEvent)
}

enum GameFeedbackEvent: Equatable {
    case sound(GameSound, volume: Float)
    case haptic(HapticEvent)
}

extension GameEngineDelegate {
    func onPowerupStatusUpdated(status: [PowerupStatus]) {}
    func onFeedback(_ event: GameFeedbackEvent) {}
}

struct GameSummary: Equatable {
    let score: Int
    let level: Int
    let durationSeconds: Int
    let bricksBroken: Int
    let livesLost: Int
}

struct PowerupStatus: Equatable {
    let type: PowerUpType
    let remainingSeconds: Int
    let charges: Int

    static func == (lhs: PowerupStatus, rhs: PowerupStatus) -> Bool {
        return lhs.type == rhs.type && lhs.remainingSeconds == rhs.remainingSeconds && lhs.charges == rhs.charges
    }
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
    private(set) var enemyShots: [EnemyShot] = []
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
    private var runBricksBroken = 0
    private var runLivesLost = 0

    // Settings
    private(set) var gameMode: GameMode
    private(set) var soundEnabled = true
    private(set) var vibrationEnabled = true

    // Active effects
    private var activeEffects: [PowerUpType: TimeInterval] = [:]
    private var speedMultiplier = 1.0
    private var timeWarpMultiplier = 1.0
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
    private var paddleVelocity: Float = 0
    private var lastPaddleX: Float? = nil
    private let basePaddleWidth: Float = 20
    private let widePaddleScale: Float = 25.0 / 18.0
    private let shrinkPaddleScale: Float = 0.7
    private var sensitivity: Float = 0.7
    private var paddleTargetX: Float = 50
    private var dailyChallengesEnabled = false
    private var aimNormalized: Float = 0
    private var aimNormalizedTarget: Float = 0
    private var aimAngle: Float = .pi * 0.5
    private var aimHasInput = false
    private var isDragging = false
    private let aimSmoothingRate: Float = 18
    private let aimCenterDeadZone: Float = 0.018
    private let aimMinAngle: Float = 0.30

    // Invaders mode state
    private var invaderDirection: Float = 1.0
    private var invaderSpeed: Float = 6.0
    private var invaderShotTimer: TimeInterval = 0
    private var invaderShotCooldown: TimeInterval = 1.6
    private var shieldMaxCharges: Int = 3
    private var invaderBricks: [Brick] = []
    private var invaderFormationOffset: Float = 0
    private var invaderRowPhase: Float = 0
    private var invaderRowDrift: Float = 0.75
    private var invaderRowPhaseOffset: Float = 0.5

    // Volley mode state
    private var volleyTurnActive = false
    private var volleyBallCount = 5
    private var volleyQueuedBalls = 0
    private var volleyLaunchTimer: TimeInterval = 0
    private var volleyLaunchX: Float = 0
    private var volleyReturnAnchorX: Float = Float.nan
    private var volleyReturnSumX: Float = 0
    private var volleyReturnCount = 0
    private var volleyTurnCount = 0
    private var volleyAdvanceRows = 0

    weak var delegate: GameEngineDelegate?

    var aimGuideAngle: Float {
        aimAngle
    }

    var shouldShowAimGuide: Bool {
        if case .ready = state {
            return balls.contains { abs($0.vx) < 0.001 && abs($0.vy) < 0.001 }
        }
        return false
    }

    init(gameMode: GameMode, sensitivity: Float = 0.7) {
        self.gameMode = gameMode
        self.sensitivity = sensitivity
        self.paddle = Paddle(x: 50, y: 8, width: basePaddleWidth, height: 2.6)
        self.paddleTargetX = paddle.x
        self.lives = gameMode.baseLives
        self.timeRemaining = gameMode.timeLimitSeconds

        resetAim()
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

        let worldDt = dtRaw * speedMultiplier * timeWarpMultiplier
        let ballDt = worldDt / max(0.01, timeWarpMultiplier)

        // Update timers
        updateTimers(worldDt)

        // Update game objects
        if state == .ready {
            attachReadyBallsToPaddle()
            updateAimFromPaddle()
        }
        updateBalls(ballDt)
        updateBricks(worldDt)
        updatePowerups(worldDt)
        updateBeams(worldDt)
        updatePaddle(worldDt)
        updateAim(Float(dtRaw))

        // Volley logic
        if gameMode == .volley {
            updateVolleyLaunchQueue(ballDt)
        }

        // Invaders logic
        if gameMode.invaders {
            updateInvaders(worldDt)
        }

        // Handle collisions
        handleCollisions()

        // Check win/lose conditions
        checkLevelCompletion()

        // Volley turn resolution
        if gameMode == .volley && state == .running {
            resolveVolleyTurnIfReady()
        }
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
                delegate?.onComboAchieved(count: 0)
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
        } else if gameMode.timeLimitSeconds == 0 {
            let elapsed = Int(elapsedSeconds)
            if elapsed != timeRemaining {
                timeRemaining = elapsed
                delegate?.onTimeUpdated(seconds: timeRemaining)
            }
        }
    }

    private func updateBalls(_ deltaTime: TimeInterval) {
        var toRemove: [Int] = []
        toRemove.reserveCapacity(2)

        for i in 0..<balls.count {
            var ball = balls[i]
            var removedCurrent = false

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
                if let remaining = ball.ricochetBounces {
                    let next = remaining - 1
                    ball.ricochetBounces = next > 0 ? next : nil
                }
            }

            // Bottom - lose life
            if ball.y - ball.radius <= 0 {
                if gameMode == .volley {
                    let clampedReturnX = ball.x.clamped(to: paddle.width / 2...worldWidth - paddle.width / 2)
                    volleyReturnSumX += clampedReturnX
                    volleyReturnCount += 1
                    if !volleyReturnAnchorX.isFinite {
                        volleyReturnAnchorX = clampedReturnX
                    } else {
                        volleyReturnAnchorX += (clampedReturnX - volleyReturnAnchorX) * 0.35
                    }
                    toRemove.append(i)
                    removedCurrent = true
                } else if gameMode.godMode {
                    ball.vy = abs(ball.vy)
                    ball.y = ball.radius + 0.1
                    emit(.sound(.bounce, volume: 0.55))
                    emit(.haptic(.light))
                } else if guardrailActive {
                    ball.vy = abs(ball.vy)
                    ball.y = ball.radius + 0.1
                    emit(.sound(.bounce, volume: 0.55))
                    emit(.haptic(.light))
                } else if shieldCharges > 0 {
                    shieldCharges -= 1
                    ball.vy = max(abs(ball.vy), Float(gameMode.launchSpeed * 0.8))
                    ball.y = paddle.y + paddle.height / 2 + ball.radius + 0.5
                    ball.x = paddle.x
                    if shieldCharges == 0 {
                        activeEffects.removeValue(forKey: .shield)
                    }
                    emit(.sound(.powerup, volume: 0.45))
                    emit(.haptic(.medium))
                } else {
                    toRemove.append(i)
                    removedCurrent = true
                }
            }

            // Top
            if ball.y + ball.radius >= worldHeight {
                ball.vy = -ball.vy
                ball.y = worldHeight - ball.radius
                emit(.sound(.bounce, volume: 0.35))
            }

            if !removedCurrent {
                clampBallSpeed(&ball)
                balls[i] = ball
            }
        }

        if !toRemove.isEmpty {
            for idx in toRemove.sorted(by: >) {
                if idx >= 0 && idx < balls.count {
                    balls.remove(at: idx)
                }
            }
        }
    }

    private func clampBallSpeed(_ ball: inout Ball) {
        let speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
        if speed <= 0 { return }
        let minSpeed = Float(gameMode.launchSpeed) * minSpeedFactor()
        let maxSpeed = Float(gameMode.launchSpeed) * maxSpeedFactor()
        let target: Float
        if speed < minSpeed {
            target = minSpeed
        } else if speed > maxSpeed {
            target = maxSpeed
        } else {
            target = speed
        }
        if target != speed {
            let scale = target / speed
            ball.vx *= scale
            ball.vy *= scale
        }

        // Keep trajectories readable by preventing near-horizontal stalls.
        let minVerticalRatio: Float = 0.22
        let minVy = target * minVerticalRatio
        if abs(ball.vy) < minVy {
            let signVy: Float = ball.vy == 0 ? 1 : (ball.vy > 0 ? 1 : -1)
            let newVy = signVy * minVy
            let signVx: Float = ball.vx == 0 ? 1 : (ball.vx > 0 ? 1 : -1)
            let newVx = sqrt(max(0, target * target - newVy * newVy)) * signVx
            ball.vy = newVy
            ball.vx = newVx
        }
    }

    private func speedBoostSlope() -> Float {
        switch gameMode {
        case .classic: return 0.014
        case .timed: return 0.019
        case .endless: return 0.018
        case .god: return 0.008
        case .rush: return 0.020
        case .volley: return 0.012
        case .tunnel: return 0.015
        case .survival: return 0.029
        case .invaders: return 0.017
        case .zen: return 0.006
        }
    }

    private func speedBoostCap() -> Float {
        switch gameMode {
        case .classic: return 1.33
        case .timed: return 1.43
        case .endless: return 1.46
        case .god: return 1.18
        case .rush: return 1.48
        case .volley: return 1.34
        case .tunnel: return 1.38
        case .survival: return 1.64
        case .invaders: return 1.44
        case .zen: return 1.14
        }
    }

    private func minSpeedFactor() -> Float {
        switch gameMode {
        case .classic: return 0.58
        case .timed: return 0.67
        case .endless: return 0.64
        case .god: return 0.48
        case .rush: return 0.70
        case .volley: return 0.58
        case .tunnel: return 0.60
        case .survival: return 0.76
        case .invaders: return 0.62
        case .zen: return 0.44
        }
    }

    private func maxSpeedFactor() -> Float {
        switch gameMode {
        case .classic: return 1.58
        case .timed: return 1.74
        case .endless: return 1.76
        case .god: return 1.36
        case .rush: return 1.78
        case .volley: return 1.52
        case .tunnel: return 1.60
        case .survival: return 1.92
        case .invaders: return 1.66
        case .zen: return 1.28
        }
    }

    private func difficultyForMode() -> Float {
        switch gameMode {
        case .classic:
            return min(3.0, 1.0 + Float(levelIndex) * 0.072)
        case .timed:
            return min(3.0, 1.06 + Float(levelIndex) * 0.092)
        case .endless:
            return min(3.0, 1.03 + Float(levelIndex) * 0.094)
        case .god:
            return min(3.0, 0.86 + Float(levelIndex) * 0.04)
        case .rush:
            return min(3.0, 1.06 + Float(levelIndex) * 0.09)
        case .volley:
            return min(3.0, 1.0 + Float(levelIndex) * 0.062)
        case .tunnel:
            return min(3.0, 1.04 + Float(levelIndex) * 0.078)
        case .survival:
            return min(3.0, 1.2 + Float(levelIndex) * 0.125)
        case .invaders:
            return min(3.0, 1.08 + Float(levelIndex) * 0.085)
        case .zen:
            return min(3.0, 0.8 + Float(levelIndex) * 0.03)
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
        let previousX = paddle.x
        let target = paddleTargetX
        let speed: Float = 90 + sensitivity * 180
        let delta = target - paddle.x
        let maxMove = speed * Float(deltaTime)
        if abs(delta) > 0.05 {
            paddle.x += delta > 0 ? min(delta, maxMove) : max(delta, -maxMove)
        }
        paddle.x = max(paddle.width / 2, min(worldWidth - paddle.width / 2, paddle.x))
        paddleVelocity = deltaTime > 0 ? (paddle.x - previousX) / Float(deltaTime) : 0
        lastPaddleX = paddle.x
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
        if gameMode != .volley {
            for ballIndex in 0..<balls.count {
                var ball = balls[ballIndex]
                if ball.intersects(paddle) {
                    handlePaddleCollision(&ball)
                    balls[ballIndex] = ball
                }
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

        // Enemy shot-paddle collisions (Invaders mode)
        if gameMode.invaders && !enemyShots.isEmpty {
            for i in stride(from: enemyShots.count - 1, through: 0, by: -1) {
                let shot = enemyShots[i]
                if shot.intersects(paddle) {
                    handleEnemyShotHit()
                    enemyShots.remove(at: i)
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
            addScore(brick.scoreValue)
            runBricksBroken += 1
            updateDailyChallenge(.bricksDestroyed)

            // Combo system
            comboCount += 1
            comboTimer = 2.0 // 2 seconds to maintain combo
            delegate?.onComboAchieved(count: comboCount)
            updateDailyChallenge(.comboMultiplier, value: comboCount)

            let dropChance: Double
            switch brick.type {
            case .explosive:
                dropChance = 0.25
            case .reinforced, .armored:
                dropChance = 0.15
            case .boss, .phase:
                dropChance = 0.20
            case .spawning:
                dropChance = 0.12
            case .moving:
                dropChance = 0.10
            default:
                dropChance = 0.08
            }
            if Double.random(in: 0..<1) < dropChance {
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
        if ball.vy > 0 { return }
        // Calculate bounce angle based on hit position
        let hitPos = (ball.x - paddle.x) / (paddle.width / 2) // -1..1 across paddle
        let maxAngle: Float = .pi / 3
        let spin = max(-0.35, min(0.35, paddleVelocity / 180))
        let angle = max(-maxAngle, min(maxAngle, hitPos * maxAngle + spin))

        let speed = max(28, sqrt(ball.vx * ball.vx + ball.vy * ball.vy))
        ball.vx = speed * sin(angle)
        ball.vy = abs(speed * cos(angle)) // Always bounce up (positive Y)
        let minVy = speed * 0.35
        if ball.vy < minVy {
            ball.vy = minVy
            let sign: Float = ball.vx >= 0 ? 1 : -1
            let newVx = sqrt(max(0, speed * speed - minVy * minVy))
            ball.vx = sign * newVx
        }

        // Prevent the "stuck in paddle" case by placing the ball just above the paddle.
        ball.y = paddle.y + paddle.height / 2 + ball.radius + 0.05
        clampBallSpeed(&ball)
        emit(.sound(.bounce, volume: 0.65))
        emit(.haptic(.light))
    }

    private func resolveBallBrickCollision(_ ball: inout Ball, _ brick: Brick) {
        let left = brick.x - brick.width / 2
        let right = brick.x + brick.width / 2
        let bottom = brick.y - brick.height / 2
        let top = brick.y + brick.height / 2

        let overlapLeft = (ball.x + ball.radius) - left
        let overlapRight = right - (ball.x - ball.radius)
        let overlapBottom = (ball.y + ball.radius) - bottom
        let overlapTop = top - (ball.y - ball.radius)

        if overlapLeft <= 0 || overlapRight <= 0 || overlapBottom <= 0 || overlapTop <= 0 {
            return
        }

        // Fireball / piercing balls pass through bricks (damage without reflection).
        if ball.isFireball || ball.isPiercing {
            return
        }

        let minOverlapX = min(overlapLeft, overlapRight)
        let minOverlapY = min(overlapBottom, overlapTop)

        if minOverlapX < minOverlapY {
            if overlapLeft < overlapRight {
                ball.x = left - ball.radius
                ball.vx = -abs(ball.vx)
            } else {
                ball.x = right + ball.radius
                ball.vx = abs(ball.vx)
            }
        } else {
            if overlapBottom < overlapTop {
                ball.y = bottom - ball.radius
                ball.vy = -abs(ball.vy)
            } else {
                ball.y = top + ball.radius
                ball.vy = abs(ball.vy)
            }
        }
        clampBallSpeed(&ball)
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
                        addScore(bricks[i].scoreValue)
                        runBricksBroken += 1
                    }
                }
            }
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
            addScore(brick.scoreValue)
            if Double.random(in: 0..<1) < 0.15 {
                spawnPowerup(atX: brick.centerX, y: brick.centerY)
            }
            applyBrickOnDestroyEffects(brick)
        }
    }

    private func activatePowerup(_ type: PowerUpType) {
        delegate?.onPowerupActivated(type: type)
        updateDailyChallenge(.powerupsCollected)
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
                var newBall = Ball(
                    x: paddle.x,
                    y: paddle.y + paddle.height / 2 + 1.0 + 1.0,
                    vx: Float(speed * cos(angle)),
                    vy: Float(abs(speed * sin(angle)))
                )
                newBall.isFireball = activeEffects.keys.contains(.fireball)
                newBall.isPiercing = activeEffects.keys.contains(.pierce)
                if activeEffects.keys.contains(.ricochet) {
                    newBall.ricochetBounces = 2
                }
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
            syncPaddleWidthFromEffects()
        case .shrink:
            syncPaddleWidthFromEffects()
        case .slowMotion:
            syncSpeedMultiplier()
        case .overdrive:
            syncSpeedMultiplier()
        case .freeze:
            freezeActive = true
            syncSpeedMultiplier()
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
                    newBall.ricochetBounces = ball.ricochetBounces
                    balls.append(newBall)
                }
            }
        case .extraLife:
            if gameMode == .volley {
                volleyBallCount = min(20, volleyBallCount + 1)
                delegate?.onTip(message: "Volley +1 ball (\(volleyBallCount) total).")
                emit(.sound(.powerup, volume: 0.42))
            } else if !gameMode.godMode {
                lives += 1
                delegate?.onLivesChanged(newLives: lives)
                emit(.sound(.life, volume: 0.9))
                emit(.haptic(.success))
            }
        case .ricochet:
            for i in 0..<balls.count {
                balls[i].ricochetBounces = (balls[i].ricochetBounces ?? 0) + 2
            }
        case .timeWarp:
            syncSpeedMultiplier()
        case .doubleScore:
            // Passive scoring modifier while effect is active.
            break
        }
        updatePowerupStatus()
    }

    private func deactivatePowerup(_ type: PowerUpType) {
        switch type {
        case .widePaddle:
            syncPaddleWidthFromEffects(removing: type)
        case .shrink:
            syncPaddleWidthFromEffects(removing: type)
        case .slowMotion:
            syncSpeedMultiplier(removing: type)
        case .guardrail:
            guardrailActive = false
        case .shield:
            shieldCharges = 0
        case .freeze:
            freezeActive = false
            syncSpeedMultiplier(removing: type)
        case .overdrive:
            syncSpeedMultiplier(removing: type)
        case .timeWarp:
            syncSpeedMultiplier(removing: type)
        case .pierce:
            pierceActive = false
            for i in 0..<balls.count { balls[i].isPiercing = false }
        case .fireball:
            for i in 0..<balls.count { balls[i].isFireball = false }
        case .ricochet:
            for i in 0..<balls.count { balls[i].ricochetBounces = nil }
        case .magnet:
            magnetActive = false
        case .gravityWell:
            gravityWellActive = false
        case .doubleScore:
            break
        default:
            break
        }
        updatePowerupStatus()
    }

    private func spawnPowerup(atX x: Float, y: Float) {
        let powerup = PowerUp(x: x, y: y, type: randomPowerupType())
        powerups.append(powerup)
    }

    private func randomPowerupType() -> PowerUpType {
        var weights: [PowerUpType: Double] = [
            .multiBall: 1.25,
            .laser: 1.05,
            .guardrail: 0.95,
            .shield: 0.9,
            .widePaddle: 0.95,
            .shrink: 0.45,
            .slowMotion: 0.9,
            .overdrive: 0.5,
            .magnet: 0.85,
            .extraLife: 0.55,
            .fireball: 0.7,
            .gravityWell: 0.7,
            .ballSplitter: 0.7,
            .freeze: 0.6,
            .pierce: 0.7,
            .ricochet: 0.8,
            .timeWarp: 0.7,
            .doubleScore: 0.6
        ]
        switch gameMode {
        case .timed:
            weights[.multiBall, default: 0] += 0.25
            weights[.laser, default: 0] += 0.2
            weights[.slowMotion, default: 0] += 0.15
        case .rush:
            weights[.guardrail, default: 0] += 0.35
            weights[.shield, default: 0] += 0.25
            weights[.slowMotion, default: 0] += 0.2
            weights[.extraLife, default: 0] += 0.1
        case .endless:
            weights[.fireball, default: 0] += 0.2
            weights[.pierce, default: 0] += 0.2
            weights[.gravityWell, default: 0] += 0.15
            weights[.ballSplitter, default: 0] += 0.1
            weights[.doubleScore, default: 0] += 0.1
        case .survival:
            weights[.shield, default: 0] += 0.2
            weights[.guardrail, default: 0] += 0.2
            weights[.extraLife, default: 0] += 0.05
            weights[.shrink] = (weights[.shrink] ?? 0) * 0.7
            weights[.overdrive] = (weights[.overdrive] ?? 0) * 0.7
            weights[.timeWarp, default: 0] += 0.12
        case .god:
            weights[.extraLife] = 0.15
            weights[.shrink] = 0.1
            weights[.overdrive] = 0.1
            weights[.doubleScore] = 0.2
        case .zen:
            weights[.extraLife] = 0.0
            weights[.shrink] = 0.08
            weights[.overdrive] = 0.08
            weights[.doubleScore] = 0.0
            weights[.timeWarp, default: 0] += 0.1
        case .invaders:
            weights[.shield, default: 0] += 0.3
            weights[.guardrail, default: 0] += 0.2
            weights[.laser, default: 0] += 0.35
            weights[.slowMotion, default: 0] += 0.1
            weights[.ricochet, default: 0] += 0.12
        default:
            break
        }

        let total = max(0.01, weights.values.reduce(0.0, +))
        let roll = Double.random(in: 0..<total)
        var acc = 0.0
        for (type, weight) in weights {
            acc += weight
            if roll <= acc { return type }
        }
        return .multiBall
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
        if gameMode == .volley && (volleyTurnActive || volleyQueuedBalls > 0) {
            return
        }
        if gameMode == .volley {
            return
        }

        if balls.isEmpty && gameMode.godMode {
            spawnBall()
            return
        }

        if balls.isEmpty && lives > 0 {
            lives -= 1
            runLivesLost += 1
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

    private func spawnBall(spawnX: Float? = nil) {
        var ball = Ball(
            x: (spawnX ?? paddle.x).clamped(to: paddle.width / 2...worldWidth - paddle.width / 2),
            y: paddle.y + paddle.height / 2 + 1.0 + 1.0,
            vx: 0,
            vy: 0
        )
        if activeEffects.keys.contains(.fireball) {
            ball.isFireball = true
        }
        if activeEffects.keys.contains(.pierce) {
            ball.isPiercing = true
        }
        if activeEffects.keys.contains(.ricochet) {
            ball.ricochetBounces = 2
        }
        balls.append(ball)
    }

    func launchBall() {
        syncAimForLaunch()
        attachReadyBallsToPaddle()

        if gameMode == .volley && state == .ready {
            launchVolleyTurn()
            return
        }

        guard let index = balls.firstIndex(where: { abs($0.vy) < 0.001 && abs($0.vx) < 0.001 }), index < balls.count else { return }

        var ball = balls[index]
        let levelBoost = min(speedBoostCap(), 1 + Float(levelIndex) * speedBoostSlope())
        let speed = Float(gameMode.launchSpeed) * levelBoost
        let angle = aimAngle.clamped(to: aimMinAngle...(Float.pi - aimMinAngle))
        ball.vx = speed * Float(Darwin.cos(Double(angle)))
        ball.vy = max(speed * Float(Darwin.sin(Double(angle))), speed * 0.18)
        balls[index] = ball
        aimHasInput = false
        aimNormalized = 0
        aimNormalizedTarget = 0
        if state == .ready {
            state = .running
        }
    }

    private func launchBallWithAim(_ ball: Ball, angleOffset: Float = 0) {
        guard let index = balls.firstIndex(where: { $0.id == ball.id }) else { return }

        var updatedBall = ball
        let levelBoost = min(speedBoostCap(), 1 + Float(levelIndex) * speedBoostSlope())
        let speed = Float(gameMode.launchSpeed) * levelBoost
        let angle = (aimAngle + angleOffset).clamped(to: aimMinAngle...(Float.pi - aimMinAngle))
        updatedBall.vx = speed * Float(Darwin.cos(Double(angle)))
        updatedBall.vy = max(speed * Float(Darwin.sin(Double(angle))), speed * 0.18)
        balls[index] = updatedBall
    }

    private func updatePowerupStatus() {
        if gameMode == .zen {
            delegate?.onPowerupStatusUpdated(status: [])
            return
        }

        let status = activeEffects
            .sorted { $0.key.rawValue < $1.key.rawValue }
            .map { (type, remaining) in
            let charges = type == .shield ? shieldCharges : 0
            return PowerupStatus(type: type, remainingSeconds: Int(remaining), charges: charges)
        }
        delegate?.onPowerupStatusUpdated(status: status)
    }

    private func syncPaddleWidthFromEffects(removing type: PowerUpType? = nil) {
        let wideActive = activeEffects.keys.contains(.widePaddle) && type != .widePaddle
        let shrinkActive = activeEffects.keys.contains(.shrink) && type != .shrink
        if wideActive && shrinkActive {
            paddle.width = basePaddleWidth
        } else if wideActive {
            paddle.width = basePaddleWidth * widePaddleScale
        } else if shrinkActive {
            paddle.width = basePaddleWidth * shrinkPaddleScale
        } else {
            paddle.width = basePaddleWidth
        }
    }

    private func syncSpeedMultiplier(removing type: PowerUpType? = nil) {
        let hasFreeze = activeEffects.keys.contains(.freeze) && type != .freeze
        let hasSlow = activeEffects.keys.contains(.slowMotion) && type != .slowMotion
        let hasOverdrive = activeEffects.keys.contains(.overdrive) && type != .overdrive
        let hasTimeWarp = activeEffects.keys.contains(.timeWarp) && type != .timeWarp
        if hasFreeze {
            speedMultiplier = 0.1
        } else if hasSlow {
            speedMultiplier = 0.7
        } else if hasOverdrive {
            speedMultiplier = 1.2
        } else {
            speedMultiplier = 1.0
        }
        timeWarpMultiplier = hasTimeWarp ? 0.7 : 1.0
    }

    func movePaddle(to x: Float) {
        paddleTargetX = max(paddle.width / 2, min(worldWidth - paddle.width / 2, x))
        if case .ready = state {
            paddle.x = paddleTargetX
            paddleVelocity = 0
            attachReadyBallsToPaddle()
        }
        updateAimFromPaddle()
    }

    func beginDrag(at x: Float) {
        isDragging = true
        movePaddle(to: x)
        aimNormalized = aimNormalizedTarget
        applyAimFromNormalized(aimNormalized)
    }

    func drag(to x: Float) {
        movePaddle(to: x)
        isDragging = true
        aimNormalized = aimNormalizedTarget
        applyAimFromNormalized(aimNormalized)
    }

    func endDrag(at x: Float) {
        movePaddle(to: x)
        syncAimForLaunch()
        isDragging = false
    }

    private func resetAim() {
        aimHasInput = false
        aimNormalized = 0
        aimNormalizedTarget = 0
        aimAngle = .pi * 0.5
        isDragging = false
    }

    private func updateAimFromPaddle() {
        let center = worldWidth * 0.5
        let sourceX = isDragging ? paddleTargetX : paddle.x
        let delta = (sourceX - center) / center
        aimNormalizedTarget = delta.clamped(to: -1...1)
    }

    private func applyAimFromNormalized(_ normalized: Float) {
        let clamped = normalized.clamped(to: -1...1)
        let stabilized: Float = abs(clamped) < aimCenterDeadZone ? 0 : clamped
        aimHasInput = isDragging || abs(stabilized) > 0.001
        let eased = stabilized * (0.8 + abs(stabilized) * 0.2)
        let centerAngle: Float = .pi * 0.5
        let maxDeflection = max(0.2, centerAngle - aimMinAngle)
        let signedDeflection = eased * maxDeflection
        aimAngle = (centerAngle - signedDeflection).clamped(to: aimMinAngle...(Float.pi - aimMinAngle))
    }

    private func syncAimForLaunch() {
        updateAimFromPaddle()
        aimNormalized = aimNormalizedTarget
        applyAimFromNormalized(aimNormalized)
    }

    private func updateAim(_ dt: Float) {
        if isDragging {
            aimNormalized = aimNormalizedTarget
        } else {
            let lerpFactor: Float
            if dt > 0 {
                lerpFactor = 1 - Float(Foundation.exp(Double(-aimSmoothingRate * dt)))
            } else {
                lerpFactor = 1
            }
            aimNormalized += (aimNormalizedTarget - aimNormalized) * lerpFactor
        }
        applyAimFromNormalized(aimNormalized)
    }

    func setSensitivity(_ newSensitivity: Float) {
        sensitivity = max(0.0, min(1.0, newSensitivity))
    }

    func updateViewport(aspectRatio: Float) {
        let clampedAspect = aspectRatio.clamped(to: 1.35...2.4)
        let newHeight = worldWidth * clampedAspect
        if abs(newHeight - worldHeight) < 0.25 {
            return
        }

        let oldHeight = worldHeight
        worldHeight = newHeight
        let yScale = newHeight / max(1, oldHeight)

        for i in 0..<balls.count {
            balls[i].y *= yScale
        }
        for i in 0..<bricks.count {
            bricks[i].y *= yScale
        }
        for i in 0..<powerups.count {
            powerups[i].y *= yScale
        }
        for i in 0..<beams.count {
            beams[i].y *= yScale
        }
        for i in 0..<enemyShots.count {
            enemyShots[i].y *= yScale
        }

        paddle.y = 8
        paddleTargetX = paddleTargetX.clamped(to: paddle.width / 2...worldWidth - paddle.width / 2)
        if case .ready = state {
            attachReadyBallsToPaddle()
        }
        updateAimFromPaddle()
    }

    func enableDailyChallenges(_ enabled: Bool) {
        dailyChallengesEnabled = enabled
    }

    private func updateDailyChallenge(_ type: ChallengeType, value: Int = 1) {
        if dailyChallengesEnabled {
            DailyChallengeStore.shared.updateProgress(type: type, value: value)
        }
    }

    private func addScore(_ points: Int) {
        guard points > 0 else { return }
        if gameMode == .zen { return }
        let multiplier = activeEffects.keys.contains(.doubleScore) ? 2 : 1
        score += points * multiplier
        delegate?.onScoreChanged(newScore: score)
    }

    private func updateInvaders(_ deltaTime: TimeInterval) {
        // Move invader bricks
        updateInvaderFormation(deltaTime)

        // Update enemy shots
        updateEnemyShots(deltaTime)

        // Fire new shots
        invaderShotTimer += deltaTime
        if invaderShotTimer >= invaderShotCooldown {
            fireInvaderShot()
            invaderShotTimer = 0
        }
    }

    private func updateInvaderFormation(_ deltaTime: TimeInterval) {
        // Simple left-right movement for invader bricks
        invaderFormationOffset += invaderDirection * invaderSpeed * Float(deltaTime)

        // Reverse direction at edges
        let maxOffset = worldWidth * 0.3
        if abs(invaderFormationOffset) > maxOffset {
            invaderDirection *= -1
            invaderFormationOffset = invaderDirection * maxOffset
        }

        // Apply movement to invader bricks
        for i in 0..<bricks.count {
            if bricks[i].type == .invader {
                bricks[i].x += invaderDirection * invaderSpeed * Float(deltaTime)
                // Keep within bounds
                bricks[i].x = max(bricks[i].width/2, min(worldWidth - bricks[i].width/2, bricks[i].x))
            }
        }
    }

    private func updateEnemyShots(_ deltaTime: TimeInterval) {
        enemyShots = enemyShots.map { shot in
            var updatedShot = shot
            updatedShot.y += shot.vy * Float(deltaTime)
            return updatedShot
        }.filter { shot in
            shot.y > -shot.radius // Remove shots that go off screen
        }
    }

    private func updateVolleyLaunchQueue(_ deltaTime: TimeInterval) {
        if !volleyTurnActive || volleyQueuedBalls <= 0 { return }
        volleyLaunchTimer -= deltaTime
        while volleyQueuedBalls > 0 && volleyLaunchTimer <= 0 {
            let spawnX = volleyLaunchX.clamped(to: paddle.width / 2...worldWidth - paddle.width / 2)
            spawnBall(spawnX: spawnX)
            if let lastBall = balls.last {
                launchBallWithAim(lastBall)
            }
            emit(.sound(.bounce, volume: 0.28))
            volleyQueuedBalls -= 1
            volleyLaunchTimer += 0.065
        }
    }

    private func resolveVolleyTurnIfReady() {
        if !volleyTurnActive { return }
        if volleyQueuedBalls > 0 || !balls.isEmpty { return }

        volleyTurnActive = false
        volleyTurnCount += 1
        volleyAdvanceRows += 1
        emit(.sound(.bounce, volume: 0.36))
        triggerScreenShake(0.7, 0.07)

        if shouldAwardVolleyBall(turnCount: volleyTurnCount) && volleyBallCount < 20 {
            volleyBallCount += 1
            delegate?.onTip(message: "Volley +1 ball (\(volleyBallCount) total).")
            emit(.sound(.powerup, volume: 0.32))
        }

        relayoutBricks()
        if hasVolleyBreach() {
            delegate?.onTip(message: "Breach! Bricks reached the launch line.")
            state = .gameOver
            delegate?.onGameOver(summary: makeSummary())
            return
        }

        spawnVolleyTopRow()
        relayoutBricks()
        if hasVolleyBreach() {
            delegate?.onTip(message: "Breach! Bricks reached the launch line.")
            state = .gameOver
            delegate?.onGameOver(summary: makeSummary())
            return
        }

        let averagedReturnX: Float
        if volleyReturnCount > 0 {
            averagedReturnX = volleyReturnSumX / Float(volleyReturnCount)
        } else {
            averagedReturnX = Float.nan
        }
        let anchorX: Float
        if averagedReturnX.isFinite {
            anchorX = averagedReturnX
        } else if volleyReturnAnchorX.isFinite {
            anchorX = volleyReturnAnchorX
        } else {
            anchorX = paddleTargetX
        }
        let launchX = anchorX * 0.78 + paddleTargetX * 0.22
        paddle.x = launchX.clamped(to: paddle.width/2...worldWidth - paddle.width/2)
        paddleTargetX = paddle.x
        volleyReturnAnchorX = Float.nan
        volleyReturnSumX = 0
        volleyReturnCount = 0
        spawnBall(spawnX: paddle.x)
        syncAimForLaunch()
        state = .ready
        updatePowerupStatus()
    }

    private func shouldAwardVolleyBall(turnCount: Int) -> Bool {
        if turnCount <= 3 {
            return true
        }
        if turnCount <= 10 {
            return turnCount % 2 == 0
        }
        return turnCount % 3 == 0
    }

    private func launchVolleyTurn() {
        if volleyTurnActive { return }
        var firstBall = balls.first(where: { abs($0.vx) < 0.001 && abs($0.vy) < 0.001 })
        if firstBall == nil {
            spawnBall(spawnX: paddle.x)
            firstBall = balls.last
        }
        guard let launchBall = firstBall else { return }
        if abs(launchBall.vx) >= 0.001 || abs(launchBall.vy) >= 0.001 { return }

        volleyTurnActive = true
        volleyQueuedBalls = max(0, volleyBallCount - 1)
        volleyLaunchTimer = 0
        volleyLaunchX = paddle.x
        volleyReturnAnchorX = Float.nan
        volleyReturnSumX = 0
        volleyReturnCount = 0

        launchBallWithAim(launchBall)
        emit(.sound(.bounce, volume: 0.45))
        aimHasInput = false
        aimNormalized = 0
        aimNormalizedTarget = 0
        state = .running
    }

    private func hasVolleyBreach() -> Bool {
        let launchLineY = paddle.y + paddle.height * 0.5 + 1.8
        return bricks.contains { $0.alive && $0.y <= launchLineY }
    }

    private func spawnVolleyTopRow() {
        let cols = 12
        let rowY = worldHeight * 0.75 + Float(volleyAdvanceRows) * 6
        let brickWidth = worldWidth / Float(cols) - 1

        var occupiedCols = Set<Int>()
        for brick in bricks where brick.alive && abs(brick.y - rowY) < 2.6 {
            let rawCol = Int((brick.x / worldWidth) * Float(cols))
            let col = max(0, min(cols - 1, rawCol))
            occupiedCols.insert(col)
        }

        let aliveBricks = bricks.filter { $0.alive }.count
        let softCap = max(cols * 5, 24)
        let congestionPenalty = Float(max(0, aliveBricks - softCap)) / Float(softCap)
        let clampedCongestion = congestionPenalty.clamped(to: 0...0.38)
        let earlyEase: Float
        if volleyTurnCount < 2 {
            earlyEase = 0.16
        } else if volleyTurnCount < 5 {
            earlyEase = 0.09
        } else {
            earlyEase = 0
        }
        let density = (0.68 + Float(levelIndex) * 0.009 + Float(volleyTurnCount) * 0.005 - clampedCongestion - earlyEase).clamped(to: 0.46...0.87)

        let ballRelief = (Float(max(0, volleyBallCount - 5)) * 0.04).clamped(to: 0...0.24)
        let hpScale = max(0.85, 1 + Float(levelIndex) * 0.06 + Float(volleyTurnCount) * 0.024 - ballRelief)
        let danger = (Float(volleyTurnCount) / 10).clamped(to: 0...1)
        let explosiveChance = min(0.1, 0.035 + danger * 0.05)
        let reinforcedChance = min(0.23, 0.11 + danger * 0.09)
        let armoredChance = volleyTurnCount < 2 ? 0 : min(0.17, 0.06 + danger * 0.08)

        let forcedGapPrimary = Int.random(in: 0..<cols)
        let forcedGapSecondary = volleyTurnCount < 5 && cols >= 8 ? (forcedGapPrimary + max(2, cols / 3)) % cols : -1
        let forcedGapTertiary = volleyTurnCount < 8 && cols >= 11 ? (forcedGapPrimary + cols / 2) % cols : -1
        var forcedGaps = Set([forcedGapPrimary])
        if forcedGapSecondary >= 0 { forcedGaps.insert(forcedGapSecondary) }
        if forcedGapTertiary >= 0 { forcedGaps.insert(forcedGapTertiary) }

        var spawned = 0
        for col in 0..<cols {
            if forcedGaps.contains(col) || occupiedCols.contains(col) {
                continue
            }
            if Float.random(in: 0...1) > density {
                continue
            }
            let roll = Float.random(in: 0...1)
            let type: BrickType
            if roll < explosiveChance {
                type = .explosive
            } else if roll < explosiveChance + reinforcedChance {
                type = .reinforced
            } else if roll < explosiveChance + reinforcedChance + armoredChance {
                type = .armored
            } else {
                type = .normal
            }
            let hp = max(1, Int(round(Float(type.baseHitPoints) * hpScale)))
            let x = Float(col) * (worldWidth / Float(cols)) + (worldWidth / Float(cols * 2))
            bricks.append(Brick(x: x, y: rowY, width: brickWidth, height: 5, type: type, hitPoints: hp))
            spawned += 1
        }

        if spawned == 0 {
            if let fallbackCol = (0..<cols).first(where: { !forcedGaps.contains($0) && !occupiedCols.contains($0) }) {
                let x = Float(fallbackCol) * (worldWidth / Float(cols)) + (worldWidth / Float(cols * 2))
                let hp = max(1, Int(round(Float(BrickType.normal.baseHitPoints) * hpScale)))
                bricks.append(Brick(x: x, y: rowY, width: brickWidth, height: 5, type: .normal, hitPoints: hp))
            }
        }
    }

    private func relayoutBricks() {
        // Move all bricks down by volleyAdvanceRows * brick height
        let moveDown = Float(volleyAdvanceRows) * 6
        for i in 0..<bricks.count {
            bricks[i].y -= moveDown
        }
        volleyAdvanceRows = 0
    }

    private func triggerScreenShake(_ intensity: Float, _ duration: Float) {
        // Simple screen shake effect - could be implemented in GameView
        delegate?.onFeedback(.sound(.explosion, volume: 0.4))
    }

    private func fireInvaderShot() {
        // Find a random invader brick to shoot from
        let invaderBricks = bricks.filter { $0.type == .invader } // Use invader type
        guard let shooter = invaderBricks.randomElement() else { return }

        let shot = EnemyShot(
            x: shooter.x,
            y: shooter.y - shooter.height/2,
            vx: 0,
            vy: -60, // Shoot downward
            radius: 2.0,
            color: (red: 1.0, green: 0.0, blue: 0.0) // Red shots
        )
        enemyShots.append(shot)
    }

    private func handleEnemyShotHit() {
        if shieldCharges > 0 {
            shieldCharges -= 1
            emit(.sound(.powerup, volume: 0.6)) // Shield absorb sound
            emit(.haptic(.medium))
        } else {
            // Shield broken, lose life
            runLivesLost += 1
            lives -= 1
            emit(.sound(.life, volume: 0.8))
            emit(.haptic(.medium))
            delegate?.onLivesChanged(newLives: lives)
            if lives <= 0 {
                state = .gameOver
                delegate?.onGameOver(summary: makeSummary())
            }
        }
    }

    private func resetLevel() {
        balls.removeAll()
        bricks.removeAll()
        powerups.removeAll()
        beams.removeAll()
        enemyShots.removeAll()
        comboCount = 0
        comboTimer = 0
        activeEffects.removeAll()
        speedMultiplier = 1.0
        timeWarpMultiplier = 1.0
        timerAccumulator = 0
        guardrailActive = false
        shieldCharges = gameMode.invaders ? shieldMaxCharges : 0
        laserCooldown = 0
        magnetActive = false
        gravityWellActive = false
        freezeActive = false
        pierceActive = false
        state = .ready
        paddleVelocity = 0
        lastPaddleX = paddle.x
        paddleTargetX = paddle.x
        resetAim()

        // Reset invader state
        if gameMode.invaders {
            invaderDirection = 1.0
            invaderFormationOffset = 0
            invaderShotTimer = 0
        }
        if gameMode.rush {
            timeRemaining = gameMode.timeLimitSeconds
        }
        if gameMode == .volley {
            volleyTurnActive = false
            volleyQueuedBalls = 0
            volleyLaunchTimer = 0
            volleyLaunchX = paddle.x
            volleyReturnAnchorX = Float.nan
            volleyReturnSumX = 0
            volleyReturnCount = 0
            volleyAdvanceRows = 0
            volleyBallCount = max(5, volleyBallCount)
        }

        // Generate level (simplified for now)
        generateLevel()
        if currentTip.isEmpty {
            delegate?.onTip(message: levelIndex == 0 ? "Tip: Release to launch. Hit the paddle edge to steer." : "Tip: Stack powerups for huge combos.")
        } else {
            delegate?.onTip(message: currentTip)
        }

        spawnBall()
        updateAimFromPaddle()
        updatePowerupStatus()
    }

    private func generateLevel() {
        let built = LevelFactory.buildLevel(
            index: levelIndex,
            worldWidth: worldWidth,
            worldHeight: worldHeight,
            mode: gameMode,
            endless: gameMode.endless,
            difficulty: difficultyForMode()
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
        runBricksBroken = 0
        runLivesLost = 0
        volleyTurnActive = false
        volleyBallCount = 5
        volleyQueuedBalls = 0
        volleyLaunchTimer = 0
        volleyLaunchX = 0
        volleyReturnAnchorX = Float.nan
        volleyReturnSumX = 0
        volleyReturnCount = 0
        volleyTurnCount = 0
        volleyAdvanceRows = 0
        delegate?.onScoreChanged(newScore: score)
        delegate?.onLivesChanged(newLives: lives)
        delegate?.onLevelChanged(newLevel: 1)
        resetLevel()
    }

    func pause() {
        if case .paused = state { return }
        isDragging = false
        state = .paused(previous: state)
    }

    func resume() {
        if case .paused(let previous) = state {
            state = previous
            isDragging = false
            updateAimFromPaddle()
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
        GameSummary(score: score, level: levelIndex + 1, durationSeconds: Int(elapsedSeconds.rounded(.down)), bricksBroken: runBricksBroken, livesLost: runLivesLost)
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

extension Float {
    func clamped(to range: ClosedRange<Float>) -> Float {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}

extension Array {
    func lastOrNull() -> Element? {
        return isEmpty ? nil : last
    }
}
