//
//  GameView.swift
//  BreakoutPlus
//
//  Game view with SpriteKit integration - port of Android GameActivity
//

import SwiftUI
import SpriteKit

struct GameView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @State private var scene: GameScene?

    var body: some View {
        ZStack {
            // SpriteKit Game Scene
            if let scene {
                SpriteView(scene: scene)
                    .edgesIgnoringSafeArea(.all)
            }

            // HUD Overlay
            VStack {
                // Top HUD
                HStack {
                    Text("Score: \(gameViewModel.score)")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)

                    Spacer()

                    let isCountdown = gameViewModel.selectedGameMode.timeLimitSeconds > 0
                    Text("\(isCountdown ? "Time" : "Elapsed") \(timeString(from: gameViewModel.timeRemaining))")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(isCountdown && gameViewModel.timeRemaining <= 10 ? .red : .white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)

                    Text("Lives: \(gameViewModel.lives)")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)
                }
                .padding(.top, 50)
                .padding(.horizontal, 20)

                Spacer()

                // Bottom HUD
                HStack {
                    Text("Level \(gameViewModel.level)")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(6)

                    Spacer()

                    if gameViewModel.comboCount > 1 {
                        Text("Combo x\(gameViewModel.comboCount)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.yellow)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(6)
                    }

                    ForEach(gameViewModel.powerupStatuses, id: \.type) { status in
                        PowerupChip(status: status)
                    }
                }
                .padding(.bottom, 30)
                .padding(.horizontal, 20)
            }

            // Top-center controls
            VStack {
                HStack {
                    if gameViewModel.leftHanded {
                        Button(action: {
                            if gameViewModel.isPaused {
                                gameViewModel.isPaused = false
                                scene?.resumeGame()
                            } else {
                                gameViewModel.isPaused = true
                                scene?.pauseGame()
                            }
                        }) {
                            Text(gameViewModel.isPaused ? "RESUME" : "PAUSE")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.black.opacity(0.55))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color(hex: "31E1F7").opacity(0.35), lineWidth: 1)
                                )
                                .cornerRadius(12)
                        }
                        Spacer()
                    } else {
                        Spacer()
                        Button(action: {
                            if gameViewModel.isPaused {
                                gameViewModel.isPaused = false
                                scene?.resumeGame()
                            } else {
                                gameViewModel.isPaused = true
                                scene?.pauseGame()
                            }
                        }) {
                            Text(gameViewModel.isPaused ? "RESUME" : "PAUSE")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.black.opacity(0.55))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color(hex: "31E1F7").opacity(0.35), lineWidth: 1)
                                )
                                .cornerRadius(12)
                        }
                        Spacer()
                    }
                }
                .padding(.top, 14)
                .padding(.horizontal, 18)
                Spacer()
            }

            // Laser fire button (only when laser is active)
            if gameViewModel.powerupStatuses.contains(where: { $0.type == .laser }) && !gameViewModel.isPaused && !gameViewModel.showGameOver && !gameViewModel.showLevelComplete {
                VStack {
                    Spacer()
                    HStack {
                        if gameViewModel.leftHanded {
                            Button(action: { scene?.fireLasers() }) {
                                Text("FIRE")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 18)
                                    .padding(.vertical, 12)
                                    .background(Color(hex: "FF4FD8").opacity(0.85))
                                    .cornerRadius(14)
                            }
                            .padding(.leading, 18)
                            .padding(.bottom, 46)
                            Spacer()
                        } else {
                            Spacer()
                            Button(action: { scene?.fireLasers() }) {
                                Text("FIRE")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 18)
                                    .padding(.vertical, 12)
                                    .background(Color(hex: "FF4FD8").opacity(0.85))
                                    .cornerRadius(14)
                            }
                            .padding(.trailing, 18)
                            .padding(.bottom, 46)
                        }
                    }
                }
            }

            // Tip banner
            if let tip = gameViewModel.tipMessage, !tip.isEmpty, !gameViewModel.isPaused {
                VStack {
                    Text(tip)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.55))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color(hex: "31E1F7").opacity(0.25), lineWidth: 1)
                        )
                        .cornerRadius(12)
                        .padding(.top, 110)
                    Spacer()
                }
                .transition(.opacity)
            }

            // Overlays
            if gameViewModel.isPaused {
                PauseOverlay(
                    onResume: {
                        gameViewModel.isPaused = false
                        scene?.resumeGame()
                    },
                    onRestart: {
                        gameViewModel.isPaused = false
                        gameViewModel.showGameOver = false
                        gameViewModel.showLevelComplete = false
                        scene?.restartGame()
                    },
                    onMenu: {
                        gameViewModel.exitToMenu()
                    }
                )
            }

            if gameViewModel.showLevelComplete {
                EndOverlay(
                    title: "LEVEL CLEAR",
                    summary: gameViewModel.lastSummary,
                    primaryTitle: "NEXT LEVEL",
                    secondaryTitle: "MENU",
                    onPrimary: {
                        gameViewModel.showLevelComplete = false
                        scene?.nextLevel()
                    },
                    onSecondary: {
                        gameViewModel.exitToMenu()
                    }
                )
            }

            if gameViewModel.showGameOver {
                EndOverlay(
                    title: "GAME OVER",
                    summary: gameViewModel.lastSummary,
                    primaryTitle: "RESTART",
                    secondaryTitle: "MENU",
                    onPrimary: {
                        gameViewModel.showGameOver = false
                        scene?.restartGame()
                    },
                    onSecondary: {
                        gameViewModel.exitToMenu()
                    }
                )
            }
        }
        .navigationBarHidden(true)
        .statusBar(hidden: true)
        .onAppear {
            if scene == nil {
                scene = GameScene(viewModel: gameViewModel)
            }
            AudioManager.shared.configure(
                soundEnabled: gameViewModel.soundEnabled,
                musicEnabled: gameViewModel.musicEnabled,
                masterVolume: Float(gameViewModel.masterVolume),
                effectsVolume: Float(gameViewModel.effectsVolume),
                musicVolume: Float(gameViewModel.musicVolume)
            )
            AudioManager.shared.startMusicIfEnabled()
        }
        .onDisappear {
            AudioManager.shared.stopMusic()
        }
        .onChange(of: gameViewModel.soundEnabled) { _ in syncAudioSettings() }
        .onChange(of: gameViewModel.musicEnabled) { _ in syncAudioSettings() }
        .onChange(of: gameViewModel.masterVolume) { _ in syncAudioSettings() }
        .onChange(of: gameViewModel.effectsVolume) { _ in syncAudioSettings() }
        .onChange(of: gameViewModel.musicVolume) { _ in syncAudioSettings() }
    }

    private func timeString(from seconds: Int) -> String {
        let minutes = seconds / 60
        let remainingSeconds = seconds % 60
        return String(format: "%d:%02d", minutes, remainingSeconds)
    }

    private func syncAudioSettings() {
        AudioManager.shared.configure(
            soundEnabled: gameViewModel.soundEnabled,
            musicEnabled: gameViewModel.musicEnabled,
            masterVolume: Float(gameViewModel.masterVolume),
            effectsVolume: Float(gameViewModel.effectsVolume),
            musicVolume: Float(gameViewModel.musicVolume)
        )
        AudioManager.shared.startMusicIfEnabled()
    }
}

private struct PauseOverlay: View {
    let onResume: () -> Void
    let onRestart: () -> Void
    let onMenu: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).edgesIgnoringSafeArea(.all)
            VStack(spacing: 14) {
                Text("PAUSED")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.white)

                Text("Take a breath. Then break more bricks.")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white.opacity(0.7))

                VStack(spacing: 10) {
                    Button(action: onResume) {
                        Text("RESUME")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PrimaryOverlayButton())

                    Button(action: onRestart) {
                        Text("RESTART")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(SecondaryOverlayButton())

                    Button(action: onMenu) {
                        Text("MENU")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(SecondaryOverlayButton())
                }
                .frame(maxWidth: 320)
            }
            .padding(.horizontal, 24)
        }
    }
}

private struct EndOverlay: View {
    let title: String
    let summary: GameSummary?
    let primaryTitle: String
    let secondaryTitle: String
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.62).edgesIgnoringSafeArea(.all)
            VStack(spacing: 14) {
                Text(title)
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)

                if let summary {
                    Text("Score \(summary.score) • Level \(summary.level) • \(formatDuration(summary.durationSeconds))")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white.opacity(0.75))
                }

                VStack(spacing: 10) {
                    Button(action: onPrimary) {
                        Text(primaryTitle)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PrimaryOverlayButton())

                    Button(action: onSecondary) {
                        Text(secondaryTitle)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(SecondaryOverlayButton())
                }
                .frame(maxWidth: 320)
            }
            .padding(.horizontal, 24)
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        if seconds <= 0 { return "--" }
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}

private struct PrimaryOverlayButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(.white)
            .padding(.vertical, 14)
            .background(Color(hex: "31E1F7").opacity(configuration.isPressed ? 0.75 : 0.95))
            .cornerRadius(14)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}

private struct SecondaryOverlayButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(.white)
            .padding(.vertical, 14)
            .background(Color(hex: "1A1F26").opacity(configuration.isPressed ? 0.85 : 1.0))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color(hex: "31E1F7").opacity(0.18), lineWidth: 1)
            )
            .cornerRadius(14)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}

class GameScene: SKScene, GameEngineDelegate {
    private var gameEngine: GameEngine!
    private var viewModel: GameViewModel!
    private var lastUpdateTime: TimeInterval = 0

    // Visual nodes
    private var ballNodes: [SKNode] = []
    private var brickNodes: [SKSpriteNode] = []
    private var paddleNode: SKSpriteNode!
    private var powerupNodes: [SKNode] = []
    private var beamNodes: [SKNode] = []
    private var enemyShotNodes: [SKShapeNode] = []
    private var flashNode: SKSpriteNode?
    private var lastBrickSnapshot: [UUID: (alive: Bool, hp: Int)] = [:]

    init(viewModel: GameViewModel) {
        self.viewModel = viewModel
        super.init(size: CGSize(width: 375, height: 667)) // Will be resized
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func didMove(to view: SKView) {
        // Prefer higher refresh rates (120Hz) on devices that support it.
        view.preferredFramesPerSecond = 120
        view.ignoresSiblingOrder = true
        view.shouldCullNonVisibleNodes = true

        setupScene()
        setupGame()
    }

    private func setupScene() {
        backgroundColor = UIColor(red: 0.06, green: 0.11, blue: 0.17, alpha: 1.0)
        scaleMode = .aspectFit
        physicsWorld.gravity = .zero

        // Add flash overlay for combo effects
        flashNode = SKSpriteNode(color: .white, size: size)
        flashNode?.position = CGPoint(x: size.width/2, y: size.height/2)
        flashNode?.alpha = 0.0
        flashNode?.zPosition = 100
        if let flashNode = flashNode {
            addChild(flashNode)
        }
    }

    private func setupGame() {
        gameEngine = GameEngine(gameMode: viewModel.selectedGameMode, sensitivity: Float(viewModel.sensitivity))
        gameEngine.enableDailyChallenges(true)
        gameEngine.delegate = self

        AudioManager.shared.configure(
            soundEnabled: viewModel.soundEnabled,
            musicEnabled: viewModel.musicEnabled,
            masterVolume: Float(viewModel.masterVolume),
            effectsVolume: Float(viewModel.effectsVolume),
            musicVolume: Float(viewModel.musicVolume)
        )
        AudioManager.shared.startMusicIfEnabled()

        // Create paddle
        paddleNode = SKSpriteNode(color: .white, size: CGSize(width: 180, height: 26))
        addChild(paddleNode)

        // Create initial game objects
        updateVisuals()
    }

    override func update(_ currentTime: TimeInterval) {
        if lastUpdateTime == 0 {
            lastUpdateTime = currentTime
            return
        }

        let deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        gameEngine.update(deltaTime: deltaTime)
        updateVisuals()
    }

    private func updateVisuals() {
        let theme = gameEngine.currentTheme
        backgroundColor = UIColor(
            red: CGFloat(theme.background.red),
            green: CGFloat(theme.background.green),
            blue: CGFloat(theme.background.blue),
            alpha: 1.0
        )
        paddleNode.color = UIColor(
            red: CGFloat(theme.paddle.red),
            green: CGFloat(theme.paddle.green),
            blue: CGFloat(theme.paddle.blue),
            alpha: 1.0
        )

        // Update paddle
        let paddleX = CGFloat(gameEngine.paddle.x / gameEngine.worldWidth) * size.width
        let paddleY = CGFloat(gameEngine.paddle.y / gameEngine.worldHeight) * size.height
        paddleNode.position = CGPoint(x: paddleX, y: paddleY)
        paddleNode.size.width = CGFloat(gameEngine.paddle.width / gameEngine.worldWidth) * size.width
        paddleNode.size.height = CGFloat(gameEngine.paddle.height / gameEngine.worldHeight) * size.height

        // Update balls
        while ballNodes.count < gameEngine.balls.count {
            let ballPath = CGPath(ellipseIn: CGRect(x: -10, y: -10, width: 20, height: 20), transform: nil)
            let ballNode = SKShapeNode(path: ballPath)
            ballNode.fillColor = .white
            ballNode.strokeColor = .cyan
            ballNode.lineWidth = 2
            ballNode.glowWidth = 1.0

            addChild(ballNode)
            ballNodes.append(ballNode)
        }

        while ballNodes.count > gameEngine.balls.count {
            ballNodes.last?.removeFromParent()
            ballNodes.removeLast()
        }

        for (index, ball) in gameEngine.balls.enumerated() {
            if index < ballNodes.count {
                let ballX = CGFloat(ball.x / gameEngine.worldWidth) * size.width
                let ballY = CGFloat(ball.y / gameEngine.worldHeight) * size.height
                ballNodes[index].position = CGPoint(x: ballX, y: ballY)
                if let shape = ballNodes[index] as? SKShapeNode {
                    if ball.isFireball {
                        shape.fillColor = .orange
                        shape.strokeColor = .red
                        shape.lineWidth = 2
                    } else if ball.isPiercing {
                        shape.fillColor = .white
                        shape.strokeColor = .cyan
                        shape.lineWidth = 2
                    } else {
                        shape.fillColor = .white
                        shape.strokeColor = .white
                        shape.lineWidth = 0
                    }
                }
            }
        }

        // Update bricks
        while brickNodes.count < gameEngine.bricks.count {
            let brickNode = SKSpriteNode(color: .blue, size: CGSize(width: 30, height: 15))
            addChild(brickNode)
            brickNodes.append(brickNode)
        }

        while brickNodes.count > gameEngine.bricks.count {
            brickNodes.last?.removeFromParent()
            brickNodes.removeLast()
        }

        for (index, brick) in gameEngine.bricks.enumerated() {
            if index < brickNodes.count {
                let brickNode = brickNodes[index]
                let brickX = CGFloat(brick.x / gameEngine.worldWidth) * size.width
                let brickY = CGFloat(brick.y / gameEngine.worldHeight) * size.height

                brickNode.position = CGPoint(x: brickX, y: brickY)
                brickNode.size = CGSize(width: CGFloat(brick.width / gameEngine.worldWidth) * size.width,
                                      height: CGFloat(brick.height / gameEngine.worldHeight) * size.height)

                // Impact FX: detect brick damage/destruction by comparing to prior snapshot.
                if let prev = lastBrickSnapshot[brick.id] {
                    if prev.alive && !brick.alive {
                        spawnBrickBurst(at: brickNode.position, type: brick.type, theme: theme)
                    } else if brick.alive && prev.hp > brick.hitPoints {
                        spawnHitSpark(at: brickNode.position, theme: theme)
                        brickNode.run(SKAction.sequence([
                            SKAction.scale(to: 1.08, duration: 0.05),
                            SKAction.scale(to: 1.0, duration: 0.08)
                        ]))
                    }
                }
                lastBrickSnapshot[brick.id] = (alive: brick.alive, hp: brick.hitPoints)

                if brick.alive {
                    brickNode.isHidden = false
                    let color = brick.currentColor(theme: theme)
                    brickNode.color = UIColor(red: CGFloat(color.red),
                                            green: CGFloat(color.green),
                                            blue: CGFloat(color.blue),
                                            alpha: 1.0)

                    // Add damage effect for partially destroyed bricks
                    if brick.hitPoints < brick.maxHitPoints {
                        brickNode.colorBlendFactor = 0.4
                        brickNode.blendMode = .add
                        let damageRatio = CGFloat(brick.hitPoints) / CGFloat(brick.maxHitPoints)
                        brickNode.alpha = 0.7 + (damageRatio * 0.3) // Fade as damaged
                    } else {
                        brickNode.colorBlendFactor = 0.0
                        brickNode.blendMode = .alpha
                        brickNode.alpha = 1.0
                    }
                } else {
                    brickNode.isHidden = true
                }
            }
        }

        // Update powerups
        while powerupNodes.count < gameEngine.powerups.count {
            let powerupNode = SKShapeNode(circleOfRadius: 10)
            powerupNode.fillColor = .yellow
            powerupNode.strokeColor = .yellow
            powerupNode.lineWidth = 0
            addChild(powerupNode)
            powerupNodes.append(powerupNode)
        }

        while powerupNodes.count > gameEngine.powerups.count {
            powerupNodes.last?.removeFromParent()
            powerupNodes.removeLast()
        }

        for (index, powerup) in gameEngine.powerups.enumerated() {
            if index < powerupNodes.count {
                let powerupX = CGFloat(powerup.x / gameEngine.worldWidth) * size.width
                let powerupY = CGFloat(powerup.y / gameEngine.worldHeight) * size.height
                powerupNodes[index].position = CGPoint(x: powerupX, y: powerupY)

                // Update appearance based on powerup type
                if let shapeNode = powerupNodes[index] as? SKShapeNode {
                    switch powerup.type {
                    case .multiBall:
                        shapeNode.path = CGPath(rect: CGRect(x: -8, y: -8, width: 16, height: 16), transform: nil)
                        shapeNode.fillColor = .red
                        shapeNode.strokeColor = .red
                    case .laser:
                        shapeNode.path = CGPath(rect: CGRect(x: -6, y: -10, width: 12, height: 20), transform: nil)
                        shapeNode.fillColor = .orange
                        shapeNode.strokeColor = .orange
                    case .guardrail:
                        shapeNode.path = CGPath(ellipseIn: CGRect(x: -10, y: -6, width: 20, height: 12), transform: nil)
                        shapeNode.fillColor = .blue
                        shapeNode.strokeColor = .blue
                    case .shield:
                        let shieldPath = CGMutablePath()
                        shieldPath.move(to: CGPoint(x: 0, y: 10))
                        shieldPath.addLine(to: CGPoint(x: -8, y: 2))
                        shieldPath.addLine(to: CGPoint(x: -8, y: -8))
                        shieldPath.addLine(to: CGPoint(x: 8, y: -8))
                        shieldPath.addLine(to: CGPoint(x: 8, y: 2))
                        shieldPath.closeSubpath()
                        shapeNode.path = shieldPath
                        shapeNode.fillColor = .cyan
                        shapeNode.strokeColor = .cyan
                    case .extraLife:
                        shapeNode.path = CGPath(ellipseIn: CGRect(x: -10, y: -10, width: 20, height: 20), transform: nil)
                        shapeNode.fillColor = .green
                        shapeNode.strokeColor = .green
                    case .widePaddle:
                        shapeNode.path = CGPath(rect: CGRect(x: -12, y: -6, width: 24, height: 12), transform: nil)
                        shapeNode.fillColor = .purple
                        shapeNode.strokeColor = .purple
                    case .shrink:
                        shapeNode.path = CGPath(rect: CGRect(x: -8, y: -5, width: 16, height: 10), transform: nil)
                        shapeNode.fillColor = .red
                        shapeNode.strokeColor = .red
                    case .slowMotion:
                        shapeNode.path = CGPath(ellipseIn: CGRect(x: -10, y: -10, width: 20, height: 20), transform: nil)
                        shapeNode.fillColor = .white
                        shapeNode.strokeColor = .white
                    case .overdrive:
                        shapeNode.path = CGPath(rect: CGRect(x: -6, y: -10, width: 12, height: 20), transform: nil)
                        shapeNode.fillColor = .orange
                        shapeNode.strokeColor = .orange
                    case .fireball:
                        shapeNode.path = CGPath(ellipseIn: CGRect(x: -10, y: -10, width: 20, height: 20), transform: nil)
                        shapeNode.fillColor = .red
                        shapeNode.strokeColor = .orange
                        shapeNode.lineWidth = 2
                    case .magnet:
                        let magnetPath = CGMutablePath()
                        magnetPath.addEllipse(in: CGRect(x: -8, y: -8, width: 16, height: 16))
                        magnetPath.move(to: CGPoint(x: -8, y: 0))
                        magnetPath.addLine(to: CGPoint(x: 8, y: 0))
                        shapeNode.path = magnetPath
                        shapeNode.fillColor = .gray
                        shapeNode.strokeColor = .gray
                    case .gravityWell:
                        shapeNode.path = CGPath(ellipseIn: CGRect(x: -10, y: -10, width: 20, height: 20), transform: nil)
                        shapeNode.fillColor = .black
                        shapeNode.strokeColor = .white
                        shapeNode.lineWidth = 2
                    case .ballSplitter:
                        let splitterPath = CGMutablePath()
                        splitterPath.addEllipse(in: CGRect(x: -8, y: -8, width: 16, height: 16))
                        splitterPath.move(to: CGPoint(x: -8, y: 0))
                        splitterPath.addLine(to: CGPoint(x: 8, y: 0))
                        splitterPath.move(to: CGPoint(x: 0, y: -8))
                        splitterPath.addLine(to: CGPoint(x: 0, y: 8))
                        shapeNode.path = splitterPath
                        shapeNode.fillColor = .yellow
                        shapeNode.strokeColor = .yellow
                    case .freeze:
                        let freezePath = CGMutablePath()
                        freezePath.move(to: CGPoint(x: 0, y: 10))
                        freezePath.addLine(to: CGPoint(x: -6, y: 4))
                        freezePath.addLine(to: CGPoint(x: 6, y: 4))
                        freezePath.closeSubpath()
                        freezePath.move(to: CGPoint(x: 0, y: 4))
                        freezePath.addLine(to: CGPoint(x: 0, y: -10))
                        shapeNode.path = freezePath
                        shapeNode.fillColor = .blue
                        shapeNode.strokeColor = .blue
                    case .pierce:
                        shapeNode.path = CGPath(rect: CGRect(x: -4, y: -10, width: 8, height: 20), transform: nil)
                        shapeNode.fillColor = .white
                        shapeNode.strokeColor = .white
                    }
                }
            }
        }

        // Update beams
        while beamNodes.count < gameEngine.beams.count {
            let beamNode = SKShapeNode(rectOf: CGSize(width: 6, height: 60), cornerRadius: 2)
            beamNode.fillColor = UIColor(red: 1.0, green: 0.2, blue: 0.8, alpha: 0.95)
            beamNode.strokeColor = beamNode.fillColor
            beamNode.lineWidth = 0
            addChild(beamNode)
            beamNodes.append(beamNode)
        }

        while beamNodes.count > gameEngine.beams.count {
            beamNodes.last?.removeFromParent()
            beamNodes.removeLast()
        }

        for (index, beam) in gameEngine.beams.enumerated() {
            if index < beamNodes.count {
                let bx = CGFloat(beam.x / gameEngine.worldWidth) * size.width
                let by = CGFloat(beam.y / gameEngine.worldHeight) * size.height
                beamNodes[index].position = CGPoint(x: bx, y: by)
                if let shape = beamNodes[index] as? SKShapeNode {
                    shape.path = CGPath(
                        roundedRect: CGRect(
                            x: -CGFloat(beam.width / gameEngine.worldWidth) * size.width / 2,
                            y: -CGFloat(beam.height / gameEngine.worldHeight) * size.height / 2,
                            width: CGFloat(beam.width / gameEngine.worldWidth) * size.width,
                            height: CGFloat(beam.height / gameEngine.worldHeight) * size.height
                        ),
                        cornerWidth: 2,
                        cornerHeight: 2,
                        transform: nil
                    )
                }
            }
        }

        // Update enemy shots (Invaders mode)
        while enemyShotNodes.count < gameEngine.enemyShots.count {
            let shotPath = CGPath(ellipseIn: CGRect(x: -3, y: -3, width: 6, height: 6), transform: nil)
            let shotNode = SKShapeNode(path: shotPath)
            shotNode.fillColor = UIColor(red: 1.0, green: 0.3, blue: 0.0, alpha: 1.0)
            shotNode.strokeColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
            shotNode.lineWidth = 1
            addChild(shotNode)
            enemyShotNodes.append(shotNode)
        }

        while enemyShotNodes.count > gameEngine.enemyShots.count {
            enemyShotNodes.last?.removeFromParent()
            enemyShotNodes.removeLast()
        }

        for (index, shot) in gameEngine.enemyShots.enumerated() {
            if index < enemyShotNodes.count {
                let shotX = CGFloat(shot.x / gameEngine.worldWidth) * size.width
                let shotY = CGFloat(shot.y / gameEngine.worldHeight) * size.height
                enemyShotNodes[index].position = CGPoint(x: shotX, y: shotY)
            }
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let location = touch.location(in: self)

            // Convert to game coordinates
            let gameX = Float(location.x / size.width) * gameEngine.worldWidth

            // Always move paddle first
            gameEngine.movePaddle(to: gameX)
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        // Launch ball on touch release if it's ready (stationary)
        if gameEngine.balls.contains(where: { $0.vy == 0 }) {
            gameEngine.launchBall()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let location = touch.location(in: self)
            let gameX = Float(location.x / size.width) * gameEngine.worldWidth
            gameEngine.movePaddle(to: gameX)
        }
    }

    // GameEngineDelegate methods
    func onScoreChanged(newScore: Int) {
        DispatchQueue.main.async {
            self.viewModel.score = newScore
        }
    }

    func onLivesChanged(newLives: Int) {
        DispatchQueue.main.async {
            self.viewModel.lives = newLives
        }
    }

    func onLevelChanged(newLevel: Int) {
        DispatchQueue.main.async {
            self.viewModel.level = newLevel
        }
    }

    func onPowerupActivated(type: PowerUpType) {
        DispatchQueue.main.async {
            self.viewModel.activePowerup = type
            if type == .laser {
                self.viewModel.laserActive = true
            }
            if type.duration > 0 {
                let captured = type
                DispatchQueue.main.asyncAfter(deadline: .now() + type.duration) {
                    if self.viewModel.activePowerup == captured {
                        self.viewModel.activePowerup = nil
                    }
                    if captured == .laser {
                        self.viewModel.laserActive = false
                    }
                }
            }
        }
    }

    func onComboAchieved(count: Int) {
        DispatchQueue.main.async {
            self.viewModel.comboCount = count

            // Flash effect for combos
            self.flashNode?.run(SKAction.sequence([
                SKAction.fadeAlpha(to: 0.3, duration: 0.1),
                SKAction.fadeAlpha(to: 0.0, duration: 0.2)
            ]))
        }
    }

    func onTimeUpdated(seconds: Int) {
        DispatchQueue.main.async {
            self.viewModel.timeRemaining = seconds
        }
    }

    func onPowerupStatusUpdated(status: [PowerupStatus]) {
        DispatchQueue.main.async {
            self.viewModel.powerupStatuses = status
        }
    }

    func onFeedback(_ event: GameFeedbackEvent) {
        switch event {
        case .sound(let sound, let volume):
            if viewModel.soundEnabled {
                AudioManager.shared.play(sound, volume: volume)
            }
        case .haptic(let haptic):
            if viewModel.vibrationEnabled {
                Haptics.shared.trigger(haptic)
            }
        }
    }

    func onTip(message: String) {
        DispatchQueue.main.async {
            self.viewModel.tipMessage = message
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) {
                if self.viewModel.tipMessage == message {
                    self.viewModel.tipMessage = nil
                }
            }
        }
    }

    func onLevelComplete(summary: GameSummary) {
        DispatchQueue.main.async {
            self.viewModel.lastSummary = summary
            self.viewModel.showLevelComplete = true
            self.viewModel.isPaused = false
            // Update progression
            ProgressionStore.shared.addXp(ProgressionStore.shared.xpForLevel(summary.level))
            ProgressionStore.shared.updateBestLevel(summary.level)
        }
    }

    func onGameOver(summary: GameSummary) {
        DispatchQueue.main.async {
            self.viewModel.lastSummary = summary
            self.viewModel.showGameOver = true
            self.viewModel.isPaused = false
            ScoreboardStore.shared.add(
                score: summary.score,
                mode: self.viewModel.selectedGameMode,
                name: "Player",  // TODO: Prompt for name
                level: summary.level,
                durationSeconds: summary.durationSeconds
            )
            // Update lifetime stats
            LifetimeStatsStore.shared.recordRun(
                bricksBroken: summary.bricksBroken,
                livesLost: summary.livesLost,
                durationSeconds: summary.durationSeconds,
                score: summary.score
            )
            AudioManager.shared.pauseMusic()
        }
    }

    func pauseGame() {
        gameEngine.pause()
        lastUpdateTime = 0
        AudioManager.shared.pauseMusic()
    }

    func resumeGame() {
        gameEngine.resume()
        lastUpdateTime = 0
        AudioManager.shared.startMusicIfEnabled()
    }

    func restartGame() {
        // Clear nodes so we don't leave stale state around.
        for node in ballNodes { node.removeFromParent() }
        for node in brickNodes { node.removeFromParent() }
        for node in powerupNodes { node.removeFromParent() }
        for node in beamNodes { node.removeFromParent() }
        for node in enemyShotNodes { node.removeFromParent() }
        ballNodes.removeAll()
        brickNodes.removeAll()
        powerupNodes.removeAll()
        beamNodes.removeAll()
        enemyShotNodes.removeAll()

        gameEngine.restart()
        AudioManager.shared.startMusicIfEnabled()
        viewModel.score = 0
        viewModel.lives = viewModel.selectedGameMode.baseLives
        viewModel.level = 1
        viewModel.comboCount = 0
        viewModel.timeRemaining = viewModel.selectedGameMode.timeLimitSeconds
        viewModel.activePowerup = nil
        viewModel.laserActive = false
        viewModel.tipMessage = nil
        updateVisuals()
        lastUpdateTime = 0
    }

    func nextLevel() {
        gameEngine.nextLevel()
        AudioManager.shared.startMusicIfEnabled()
        viewModel.activePowerup = nil
        viewModel.laserActive = false
        viewModel.tipMessage = nil
        lastUpdateTime = 0
    }

    func fireLasers() {
        gameEngine.fireLasers()
    }

    private func spawnHitSpark(at position: CGPoint, theme: LevelTheme) {
        let color = UIColor(
            red: CGFloat(theme.accent.red),
            green: CGFloat(theme.accent.green),
            blue: CGFloat(theme.accent.blue),
            alpha: 1.0
        )
        spawnParticleBurst(at: position, color: color, count: 8, speed: 120, radius: 2.2, life: 0.22)
    }

    private func spawnBrickBurst(at position: CGPoint, type: BrickType, theme: LevelTheme) {
        let base = theme.brickPalette[type] ?? theme.accent
        let color = UIColor(red: CGFloat(base.red), green: CGFloat(base.green), blue: CGFloat(base.blue), alpha: 1.0)
        let count = (type == .explosive || type == .boss) ? 20 : 12
        let speed: CGFloat = (type == .explosive || type == .boss) ? 260 : 180
        let radius: CGFloat = (type == .explosive || type == .boss) ? 3.0 : 2.4
        let life: TimeInterval = (type == .explosive || type == .boss) ? 0.42 : 0.28
        spawnParticleBurst(at: position, color: color, count: count, speed: speed, radius: radius, life: life)
        if type == .explosive || type == .boss {
            // Small screen shake for weight.
            run(SKAction.sequence([
                SKAction.moveBy(x: 6, y: 0, duration: 0.03),
                SKAction.moveBy(x: -12, y: 0, duration: 0.05),
                SKAction.moveBy(x: 6, y: 0, duration: 0.03),
            ]))
        }
    }

    private func spawnParticleBurst(
        at position: CGPoint,
        color: UIColor,
        count: Int,
        speed: CGFloat,
        radius: CGFloat,
        life: TimeInterval
    ) {
        for _ in 0..<count {
            let p = SKShapeNode(circleOfRadius: radius)
            p.fillColor = color
            p.strokeColor = color
            p.lineWidth = 0
            p.position = position
            p.zPosition = 50
            addChild(p)

            let angle = CGFloat.random(in: 0..<(CGFloat.pi * 2))
            let dist = speed * CGFloat(life) * CGFloat.random(in: 0.45...1.0)
            let dx = cos(angle) * dist
            let dy = sin(angle) * dist

            let move = SKAction.moveBy(x: dx, y: dy, duration: life)
            move.timingMode = .easeOut
            let fade = SKAction.fadeOut(withDuration: life)
            fade.timingMode = .easeOut
            p.run(SKAction.sequence([
                SKAction.group([move, fade]),
                SKAction.removeFromParent()
            ]))
        }
    }
}

private struct PowerupChip: View {
    let status: PowerupStatus

    var body: some View {
        HStack(spacing: 4) {
            Text(status.type.displayName)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(.white)
            if status.remainingSeconds > 0 {
                Text("\(status.remainingSeconds)s")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.yellow)
            }
            if status.charges > 0 {
                Text("(\(status.charges))")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.cyan)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color(red: Double(status.type.color.red), green: Double(status.type.color.green), blue: Double(status.type.color.blue)).opacity(0.8))
        .cornerRadius(6)
    }
}

#Preview {
    GameView()
        .environmentObject(GameViewModel())
}
