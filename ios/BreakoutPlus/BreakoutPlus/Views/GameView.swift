//
//  GameView.swift
//  BreakoutPlus
//
//  Game view with SpriteKit integration - port of Android GameActivity
//

import SwiftUI
import SpriteKit
import UIKit

struct GameView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @State private var scene: GameScene?

    var body: some View {
        GeometryReader { geometry in
            let isZenMode = gameViewModel.selectedGameMode == .zen
            let safeTop = resolvedSafeTop(from: geometry)
            let hudTop = safeTop + 18
            let visibleStatuses = Array(gameViewModel.powerupStatuses.prefix(3))

            ZStack {
                // SpriteKit Game Scene
                if let scene {
                    SpriteView(scene: scene)
                        .edgesIgnoringSafeArea(.all)
                }

                // HUD Overlay
                VStack(spacing: 6) {
                    HStack {
                        if !isZenMode {
                            Text("Score: \(gameViewModel.score)")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 9)
                                .padding(.vertical, 5)
                                .background(Color.black.opacity(0.45))
                                .cornerRadius(8)

                            Spacer()

                            let isCountdown = gameViewModel.selectedGameMode.timeLimitSeconds > 0
                            Text("\(isCountdown ? "Time" : "Elapsed") \(timeString(from: gameViewModel.timeRemaining))")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(isCountdown && gameViewModel.timeRemaining <= 10 ? .red : .white)
                                .padding(.horizontal, 9)
                                .padding(.vertical, 5)
                                .background(Color.black.opacity(0.45))
                                .cornerRadius(8)

                            Text("Lives: \(gameViewModel.lives)")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 9)
                                .padding(.vertical, 5)
                                .background(Color.black.opacity(0.45))
                                .cornerRadius(8)
                        } else {
                            Spacer()
                            Text("Zen Mode")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white.opacity(0.95))
                                .padding(.horizontal, 9)
                                .padding(.vertical, 5)
                                .background(Color.black.opacity(0.45))
                                .cornerRadius(8)
                            Spacer()
                        }
                    }
                    .padding(.top, hudTop)
                    .padding(.horizontal, 16)

                    HStack {
                        Text("Level \(gameViewModel.level)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.black.opacity(0.45))
                            .cornerRadius(6)

                        Spacer()

                        if !isZenMode && gameViewModel.comboCount > 1 {
                            Text("Combo x\(gameViewModel.comboCount)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.yellow)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.black.opacity(0.45))
                                .cornerRadius(6)
                        }

                        if !isZenMode {
                            ForEach(visibleStatuses, id: \.type) { status in
                                PowerupChip(status: status)
                            }
                            if gameViewModel.powerupStatuses.count > visibleStatuses.count {
                                Text("+\(gameViewModel.powerupStatuses.count - visibleStatuses.count)")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 7)
                                    .padding(.vertical, 3)
                                    .background(Color.black.opacity(0.45))
                                    .cornerRadius(6)
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    Spacer()
                }
                .allowsHitTesting(false)

                // Top controls
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
                        }
                    }
                    .padding(.top, safeTop + 6)
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

                if let tip = gameViewModel.tipMessage, !tip.isEmpty, !gameViewModel.isPaused {
                    VStack {
                        Text(tip)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(Color.black.opacity(0.55))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(hex: "31E1F7").opacity(0.25), lineWidth: 1)
                            )
                            .cornerRadius(12)
                            .padding(.top, safeTop + 122)
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
            .frame(maxWidth: .infinity, maxHeight: .infinity)
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

    private func resolvedSafeTop(from geometry: GeometryProxy) -> CGFloat {
        let sceneTop = geometry.safeAreaInsets.top
        let windowTop = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .filter(\.isKeyWindow)
            .map(\.safeAreaInsets.top)
            .max() ?? 0
        return max(sceneTop, windowTop, 10)
    }

    private func syncAudioSettings() {
        AudioManager.shared.configure(
            soundEnabled: gameViewModel.soundEnabled,
            musicEnabled: gameViewModel.musicEnabled,
            masterVolume: Float(gameViewModel.masterVolume),
            effectsVolume: Float(gameViewModel.effectsVolume),
            musicVolume: Float(gameViewModel.musicVolume)
        )
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
    private var ballNodes: [BallVisualNode] = []
    private var brickNodes: [BrickVisualNode] = []
    private var paddleNode: SKSpriteNode!
    private var powerupNodes: [PowerupVisualNode] = []
    private var beamNodes: [SKNode] = []
    private var enemyShotNodes: [SKShapeNode] = []
    private var aimGuideDots: [SKShapeNode] = []
    private var flashNode: SKSpriteNode?
    private var lastBrickSnapshot: [UUID: (alive: Bool, hp: Int)] = [:]
    private var lastEngineState: GameState?
    private var activeTouchID: ObjectIdentifier?
    private var pixelScale: CGFloat = max(2.0, UIScreen.main.scale)
    private var shakeOffset: CGPoint = .zero
    private var shakeTimeRemaining: TimeInterval = 0
    private var shakeDuration: TimeInterval = 0
    private var shakeIntensity: CGFloat = 0
    private var lastComboFlashAt: TimeInterval = 0

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
        pixelScale = max(1, view.contentScaleFactor)

        setupScene()
        setupGame()
    }

    private func setupScene() {
        backgroundColor = UIColor(red: 0.06, green: 0.11, blue: 0.17, alpha: 1.0)
        scaleMode = .resizeFill
        physicsWorld.gravity = .zero

        // Add flash overlay for combo effects
        flashNode = SKSpriteNode(color: UIColor(red: 0.19, green: 0.88, blue: 0.97, alpha: 1.0), size: size)
        flashNode?.position = CGPoint(x: size.width/2, y: size.height/2)
        flashNode?.alpha = 0.0
        flashNode?.zPosition = 100
        if let flashNode = flashNode {
            addChild(flashNode)
        }
    }

    private func setupGame() {
        gameEngine = GameEngine(gameMode: viewModel.selectedGameMode, sensitivity: Float(viewModel.sensitivity))
        gameEngine.updateViewport(aspectRatio: Float(size.height / max(size.width, 1)))
        gameEngine.enableDailyChallenges(true)
        gameEngine.delegate = self

        AudioManager.shared.configure(
            soundEnabled: viewModel.soundEnabled,
            musicEnabled: viewModel.musicEnabled,
            masterVolume: Float(viewModel.masterVolume),
            effectsVolume: Float(viewModel.effectsVolume),
            musicVolume: Float(viewModel.musicVolume)
        )

        // Create paddle
        paddleNode = SKSpriteNode(color: .white, size: CGSize(width: 180, height: 26))
        addChild(paddleNode)

        // Create initial game objects
        updateVisuals()
        syncAudioPlayback(force: true)
    }

    override func didChangeSize(_ oldSize: CGSize) {
        super.didChangeSize(oldSize)
        if let view {
            pixelScale = max(1, view.contentScaleFactor)
        }
        flashNode?.size = size
        flashNode?.position = CGPoint(x: size.width / 2, y: size.height / 2)
        guard gameEngine != nil else { return }
        gameEngine.updateViewport(aspectRatio: Float(size.height / max(size.width, 1)))
        updateVisuals()
    }

    override func update(_ currentTime: TimeInterval) {
        if lastUpdateTime == 0 {
            lastUpdateTime = currentTime
            return
        }

        let deltaTime = max(0.0, min(1.0 / 60.0, currentTime - lastUpdateTime))
        lastUpdateTime = currentTime

        gameEngine.update(deltaTime: deltaTime)
        updateScreenShake(deltaTime)
        updateVisuals()
        syncAudioPlayback()
    }

    private func updateVisuals() {
        let theme = gameEngine.currentTheme
        backgroundColor = UIColor(
            red: CGFloat(theme.background.red),
            green: CGFloat(theme.background.green),
            blue: CGFloat(theme.background.blue),
            alpha: 1.0
        )
        flashNode?.color = UIColor(
            red: CGFloat(theme.accent.red),
            green: CGFloat(theme.accent.green),
            blue: CGFloat(theme.accent.blue),
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
        paddleNode.position = snappedPoint(CGPoint(x: paddleX + shakeOffset.x, y: paddleY + shakeOffset.y))
        paddleNode.size.width = snappedLength(CGFloat(gameEngine.paddle.width / gameEngine.worldWidth) * size.width)
        paddleNode.size.height = snappedLength(CGFloat(gameEngine.paddle.height / gameEngine.worldHeight) * size.height)

        // Update balls
        while ballNodes.count < gameEngine.balls.count {
            let ballNode = BallVisualNode()
            addChild(ballNode)
            ballNodes.append(ballNode)
        }

        while ballNodes.count > gameEngine.balls.count {
            ballNodes.last?.removeFromParent()
            ballNodes.removeLast()
        }

        for (index, ball) in gameEngine.balls.enumerated() {
            if index < ballNodes.count {
                ballNodes[index].apply(
                    ball: ball,
                    worldWidth: gameEngine.worldWidth,
                    worldHeight: gameEngine.worldHeight,
                    sceneSize: size,
                    pixelScale: pixelScale,
                    shakeOffset: shakeOffset
                )
            }
        }

        // Update bricks
        while brickNodes.count < gameEngine.bricks.count {
            let brickNode = BrickVisualNode()
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
                brickNode.apply(
                    brick: brick,
                    theme: theme,
                    worldWidth: gameEngine.worldWidth,
                    worldHeight: gameEngine.worldHeight,
                    sceneSize: size,
                    time: lastUpdateTime,
                    pixelScale: pixelScale,
                    shakeOffset: shakeOffset
                )

                // Impact FX: detect brick damage/destruction by comparing to prior snapshot.
                if let prev = lastBrickSnapshot[brick.id] {
                    if prev.alive && !brick.alive {
                        spawnBrickBurst(at: brickNode.position, type: brick.type, theme: theme)
                    } else if brick.alive && prev.hp > brick.hitPoints {
                        spawnHitSpark(at: brickNode.position, theme: theme)
                        brickNode.pulseHit()
                    }
                }
                lastBrickSnapshot[brick.id] = (alive: brick.alive, hp: brick.hitPoints)

                if brick.alive {
                    brickNode.isHidden = false
                } else {
                    brickNode.isHidden = true
                }
            }
        }

        // Update powerups
        while powerupNodes.count < gameEngine.powerups.count {
            let powerupNode = PowerupVisualNode()
            addChild(powerupNode)
            powerupNodes.append(powerupNode)
        }

        while powerupNodes.count > gameEngine.powerups.count {
            powerupNodes.last?.removeFromParent()
            powerupNodes.removeLast()
        }

        for (index, powerup) in gameEngine.powerups.enumerated() {
            if index < powerupNodes.count {
                powerupNodes[index].apply(
                    powerup: powerup,
                    worldWidth: gameEngine.worldWidth,
                    worldHeight: gameEngine.worldHeight,
                    sceneSize: size,
                    time: lastUpdateTime,
                    pixelScale: pixelScale,
                    shakeOffset: shakeOffset
                )
            }
        }

        // Update beams
        while beamNodes.count < gameEngine.beams.count {
            let beamNode = SKShapeNode(rectOf: CGSize(width: 6, height: 60), cornerRadius: 2)
            beamNode.isAntialiased = false
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
                beamNodes[index].position = snappedPoint(CGPoint(x: bx + shakeOffset.x, y: by + shakeOffset.y))
                if let shape = beamNodes[index] as? SKShapeNode {
                    let w = snappedLength(CGFloat(beam.width / gameEngine.worldWidth) * size.width)
                    let h = snappedLength(CGFloat(beam.height / gameEngine.worldHeight) * size.height)
                    shape.path = CGPath(
                        roundedRect: CGRect(
                            x: -w / 2,
                            y: -h / 2,
                            width: w,
                            height: h
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
            shotNode.isAntialiased = false
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
                enemyShotNodes[index].position = snappedPoint(CGPoint(x: shotX + shakeOffset.x, y: shotY + shakeOffset.y))
            }
        }

        updateAimGuide(theme: theme)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard activeTouchID == nil, let touch = touches.first else { return }
        activeTouchID = ObjectIdentifier(touch)
        let location = touch.location(in: self)
        let gameX = Float(location.x / size.width) * gameEngine.worldWidth
        gameEngine.beginDrag(at: gameX)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let activeTouchID else { return }
        guard touches.contains(where: { ObjectIdentifier($0) == activeTouchID }) else { return }

        if let touch = touches.first(where: { ObjectIdentifier($0) == activeTouchID }) {
            let location = touch.location(in: self)
            let gameX = Float(location.x / size.width) * gameEngine.worldWidth
            gameEngine.endDrag(at: gameX)
        } else {
            gameEngine.endDrag(at: gameEngine.paddle.x)
        }

        self.activeTouchID = nil

        if gameEngine.shouldShowAimGuide {
            gameEngine.launchBall()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let activeTouchID else { return }
        guard let touch = touches.first(where: { ObjectIdentifier($0) == activeTouchID }) else { return }
        let location = touch.location(in: self)
        let gameX = Float(location.x / size.width) * gameEngine.worldWidth
        gameEngine.drag(to: gameX)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let activeTouchID else { return }
        if touches.contains(where: { ObjectIdentifier($0) == activeTouchID }) {
            gameEngine.endDrag(at: gameEngine.paddle.x)
            self.activeTouchID = nil
        }
    }

    private func syncAudioPlayback(force: Bool = false) {
        let state = gameEngine.state
        if !force, let lastEngineState, lastEngineState == state {
            return
        }
        switch state {
        case .running:
            AudioManager.shared.startMusicIfEnabled()
        case .ready, .paused, .levelComplete, .gameOver:
            AudioManager.shared.pauseMusic()
        }
        lastEngineState = state
    }

    private struct AimHit {
        let t: CGFloat
        let nx: CGFloat
        let ny: CGFloat
        let hitsBrick: Bool
    }

    private func updateAimGuide(theme: LevelTheme) {
        guard gameEngine.shouldShowAimGuide,
              let ball = gameEngine.balls.first(where: { abs($0.vx) < 0.001 && abs($0.vy) < 0.001 }) else {
            hideAimGuide()
            return
        }

        let accent = UIColor(
            red: CGFloat(theme.accent.red),
            green: CGFloat(theme.accent.green),
            blue: CGFloat(theme.accent.blue),
            alpha: 1.0
        )

        var dots: [(position: CGPoint, radius: CGFloat, alpha: CGFloat)] = []
        dots.reserveCapacity(96)

        var dx = CGFloat(Darwin.cos(Double(gameEngine.aimGuideAngle)))
        var dy = CGFloat(Darwin.sin(Double(gameEngine.aimGuideAngle)))
        var startX = CGFloat(ball.x)
        var startY = CGFloat(ball.y)
        let radius = CGFloat(ball.radius)

        let arrowLength = max(12, min(22, CGFloat(gameEngine.worldHeight) * 0.16))
        let arrowSteps = 7
        for i in 1...arrowSteps {
            let t = CGFloat(i) / CGFloat(arrowSteps)
            let size = 0.42 + t * 0.34
            let alpha = min(0.9, 0.35 + t * 0.55)
            dots.append((
                position: worldToScene(x: startX + dx * arrowLength * t, y: startY + dy * arrowLength * t),
                radius: size,
                alpha: alpha
            ))
        }

        for segment in 0..<5 {
            let hit = findAimCollision(startX: startX, startY: startY, dirX: dx, dirY: dy, radius: radius)
            if !hit.t.isFinite || hit.t <= 0 {
                break
            }

            let segmentLength = hit.t
            let spacing = max(4.2, CGFloat(gameEngine.worldHeight) * 0.06)
            let steps = max(6, min(16, Int(segmentLength / spacing)))
            let segmentAlpha = max(0.14, 0.38 - CGFloat(segment) * 0.08)

            for i in 1...steps {
                let t = CGFloat(i) / CGFloat(steps)
                let alpha = min(segmentAlpha, max(0, segmentAlpha * (1 - t)))
                let size = 0.32 + (1 - t) * 0.18
                dots.append((
                    position: worldToScene(x: startX + dx * segmentLength * t, y: startY + dy * segmentLength * t),
                    radius: size,
                    alpha: alpha
                ))
            }

            let impactX = startX + dx * segmentLength
            let impactY = startY + dy * segmentLength
            if hit.hitsBrick {
                dots.append((
                    position: worldToScene(x: impactX, y: impactY),
                    radius: 0.7,
                    alpha: 0.88
                ))
                break
            }

            if hit.nx == 0 && hit.ny == 0 {
                startX = impactX + dx * 0.02
                startY = impactY + dy * 0.02
            } else {
                if hit.nx != 0 { dx = -dx }
                if hit.ny != 0 { dy = -dy }
                startX = impactX + dx * 0.02
                startY = impactY + dy * 0.02
            }
        }

        while aimGuideDots.count < dots.count {
            let dot = SKShapeNode(circleOfRadius: 1)
            dot.lineWidth = 0
            dot.zPosition = 40
            addChild(dot)
            aimGuideDots.append(dot)
        }

        for i in 0..<aimGuideDots.count {
            if i < dots.count {
                let dot = dots[i]
                let node = aimGuideDots[i]
                node.isHidden = false
                node.position = dot.position
                node.path = CGPath(
                    ellipseIn: CGRect(x: -dot.radius, y: -dot.radius, width: dot.radius * 2, height: dot.radius * 2),
                    transform: nil
                )
                node.fillColor = accent.withAlphaComponent(dot.alpha)
                node.strokeColor = node.fillColor
            } else {
                aimGuideDots[i].isHidden = true
            }
        }
    }

    private func hideAimGuide() {
        for dot in aimGuideDots {
            dot.isHidden = true
        }
    }

    private func worldToScene(x: CGFloat, y: CGFloat) -> CGPoint {
        CGPoint(
            x: x / CGFloat(gameEngine.worldWidth) * size.width,
            y: y / CGFloat(gameEngine.worldHeight) * size.height
        )
    }

    private func updateScreenShake(_ deltaTime: TimeInterval) {
        guard shakeTimeRemaining > 0 else {
            shakeOffset = .zero
            return
        }
        shakeTimeRemaining = max(0, shakeTimeRemaining - deltaTime)
        let progress = CGFloat(shakeTimeRemaining / max(0.001, shakeDuration))
        let amplitude = shakeIntensity * progress
        let raw = CGPoint(
            x: CGFloat.random(in: -amplitude...amplitude),
            y: CGFloat.random(in: -amplitude...amplitude)
        )
        shakeOffset = snappedPoint(raw)
        if shakeTimeRemaining <= 0 {
            shakeOffset = .zero
        }
    }

    func triggerScreenShake(intensity: Float, duration: Float) {
        let clampedIntensity = CGFloat(max(0.2, min(3.4, intensity * 1.8)))
        let clampedDuration = TimeInterval(max(0.03, min(0.22, duration)))
        shakeIntensity = max(shakeIntensity, clampedIntensity)
        shakeDuration = max(shakeDuration, clampedDuration)
        shakeTimeRemaining = max(shakeTimeRemaining, clampedDuration)
    }

    private func snappedPoint(_ point: CGPoint) -> CGPoint {
        CGPoint(x: snappedLength(point.x), y: snappedLength(point.y))
    }

    private func snappedLength(_ value: CGFloat) -> CGFloat {
        guard pixelScale > 0 else { return value }
        return (value * pixelScale).rounded() / pixelScale
    }

    private func findAimCollision(startX: CGFloat, startY: CGFloat, dirX: CGFloat, dirY: CGFloat, radius: CGFloat) -> AimHit {
        let wallHit = findAimWallCollision(startX: startX, startY: startY, dirX: dirX, dirY: dirY, radius: radius)
        if let brickHit = findAimBrickCollision(
            startX: startX,
            startY: startY,
            dirX: dirX,
            dirY: dirY,
            radius: radius,
            maxDistance: wallHit.t
        ), brickHit.t < wallHit.t {
            return brickHit
        }
        return wallHit
    }

    private func findAimWallCollision(startX: CGFloat, startY: CGFloat, dirX: CGFloat, dirY: CGFloat, radius: CGFloat) -> AimHit {
        var bestT = CGFloat.greatestFiniteMagnitude
        var normalX: CGFloat = 0
        var normalY: CGFloat = 0

        let worldWidth = CGFloat(gameEngine.worldWidth)
        let worldHeight = CGFloat(gameEngine.worldHeight)

        if dirY > 0 {
            let tTop = (worldHeight - radius - startY) / dirY
            if tTop > 0 && tTop < bestT {
                bestT = tTop
                normalX = 0
                normalY = -1
            }
        }
        if dirX > 0 {
            let tRight = (worldWidth - radius - startX) / dirX
            if tRight > 0 && tRight < bestT {
                bestT = tRight
                normalX = -1
                normalY = 0
            }
        } else if dirX < 0 {
            let tLeft = (radius - startX) / dirX
            if tLeft > 0 && tLeft < bestT {
                bestT = tLeft
                normalX = 1
                normalY = 0
            }
        }

        return AimHit(t: bestT, nx: normalX, ny: normalY, hitsBrick: false)
    }

    private func findAimBrickCollision(
        startX: CGFloat,
        startY: CGFloat,
        dirX: CGFloat,
        dirY: CGFloat,
        radius: CGFloat,
        maxDistance: CGFloat
    ) -> AimHit? {
        let epsilon: CGFloat = 1e-5
        var bestT = maxDistance
        var bestNx: CGFloat = 0
        var bestNy: CGFloat = 0
        var found = false

        for brick in gameEngine.bricks where brick.alive {
            let left = CGFloat(brick.x - brick.width / 2) - radius
            let right = CGFloat(brick.x + brick.width / 2) + radius
            let bottom = CGFloat(brick.y - brick.height / 2) - radius
            let top = CGFloat(brick.y + brick.height / 2) + radius

            let tNearX: CGFloat
            let tFarX: CGFloat
            if abs(dirX) < epsilon {
                if startX <= left || startX >= right { continue }
                tNearX = -CGFloat.greatestFiniteMagnitude
                tFarX = CGFloat.greatestFiniteMagnitude
            } else {
                let tx1 = (left - startX) / dirX
                let tx2 = (right - startX) / dirX
                tNearX = min(tx1, tx2)
                tFarX = max(tx1, tx2)
            }

            let tNearY: CGFloat
            let tFarY: CGFloat
            if abs(dirY) < epsilon {
                if startY <= bottom || startY >= top { continue }
                tNearY = -CGFloat.greatestFiniteMagnitude
                tFarY = CGFloat.greatestFiniteMagnitude
            } else {
                let ty1 = (bottom - startY) / dirY
                let ty2 = (top - startY) / dirY
                tNearY = min(ty1, ty2)
                tFarY = max(ty1, ty2)
            }

            let tEnter = max(tNearX, tNearY)
            let tExit = min(tFarX, tFarY)
            if tExit <= 0 || tEnter <= 0 || tEnter >= tExit || tEnter >= bestT {
                continue
            }

            let normalX: CGFloat
            let normalY: CGFloat
            if abs(tNearX - tNearY) < 0.0001 {
                if abs(dirX) >= abs(dirY) {
                    normalX = dirX > 0 ? -1 : 1
                    normalY = 0
                } else {
                    normalX = 0
                    normalY = dirY > 0 ? -1 : 1
                }
            } else if tNearX > tNearY {
                normalX = dirX > 0 ? -1 : 1
                normalY = 0
            } else {
                normalX = 0
                normalY = dirY > 0 ? -1 : 1
            }

            bestT = tEnter
            bestNx = normalX
            bestNy = normalY
            found = true
        }

        if found {
            return AimHit(t: bestT, nx: bestNx, ny: bestNy, hitsBrick: true)
        }
        return nil
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

            // Keep flash events sparse and predictable to avoid perceived flicker.
            let now = Date().timeIntervalSinceReferenceDate
            if count >= 10 && count % 10 == 0 && (now - self.lastComboFlashAt) >= 0.55 {
                self.lastComboFlashAt = now
                self.flashNode?.removeAction(forKey: "comboFlash")
                self.flashNode?.run(
                    SKAction.sequence([
                        SKAction.fadeAlpha(to: 0.038, duration: 0.03),
                        SKAction.fadeAlpha(to: 0.0, duration: 0.14)
                    ]),
                    withKey: "comboFlash"
                )
            }
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
        case .screenShake(let intensity, let duration):
            triggerScreenShake(intensity: intensity, duration: duration)
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
            let trimmedName = self.viewModel.playerName.trimmingCharacters(in: .whitespacesAndNewlines)
            ScoreboardStore.shared.add(
                score: summary.score,
                mode: self.viewModel.selectedGameMode,
                name: trimmedName.isEmpty ? "Player" : trimmedName,
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
        }
        syncAudioPlayback(force: true)
    }

    func pauseGame() {
        gameEngine.pause()
        activeTouchID = nil
        lastUpdateTime = 0
        syncAudioPlayback(force: true)
    }

    func resumeGame() {
        gameEngine.resume()
        activeTouchID = nil
        lastUpdateTime = 0
        syncAudioPlayback(force: true)
    }

    func restartGame() {
        // Clear nodes so we don't leave stale state around.
        for node in ballNodes { node.removeFromParent() }
        for node in brickNodes { node.removeFromParent() }
        for node in powerupNodes { node.removeFromParent() }
        for node in beamNodes { node.removeFromParent() }
        for node in enemyShotNodes { node.removeFromParent() }
        for node in aimGuideDots { node.removeFromParent() }
        ballNodes.removeAll()
        brickNodes.removeAll()
        powerupNodes.removeAll()
        beamNodes.removeAll()
        enemyShotNodes.removeAll()
        aimGuideDots.removeAll()

        gameEngine.restart()
        activeTouchID = nil
        viewModel.score = 0
        viewModel.lives = viewModel.selectedGameMode.baseLives
        viewModel.level = 1
        viewModel.comboCount = 0
        viewModel.timeRemaining = viewModel.selectedGameMode.timeLimitSeconds
        viewModel.activePowerup = nil
        viewModel.laserActive = false
        viewModel.tipMessage = nil
        updateVisuals()
        syncAudioPlayback(force: true)
        lastUpdateTime = 0
    }

    func nextLevel() {
        gameEngine.nextLevel()
        activeTouchID = nil
        viewModel.activePowerup = nil
        viewModel.laserActive = false
        viewModel.tipMessage = nil
        syncAudioPlayback(force: true)
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
        spawnParticleBurst(at: position, color: color, count: 10, speed: 150, radius: 2.1, life: 0.22)
        spawnImpactRing(at: position, color: color, startRadius: 2.5, endRadius: 19, lineWidth: 2.4, life: 0.16)
    }

    private func spawnBrickBurst(at position: CGPoint, type: BrickType, theme: LevelTheme) {
        let base = theme.brickPalette[type] ?? theme.accent
        let color = UIColor(red: CGFloat(base.red), green: CGFloat(base.green), blue: CGFloat(base.blue), alpha: 1.0)
        let count = (type == .explosive || type == .boss) ? 20 : 12
        let speed: CGFloat = (type == .explosive || type == .boss) ? 260 : 180
        let radius: CGFloat = (type == .explosive || type == .boss) ? 3.0 : 2.4
        let life: TimeInterval = (type == .explosive || type == .boss) ? 0.42 : 0.28
        spawnParticleBurst(at: position, color: color, count: count, speed: speed, radius: radius, life: life)
        spawnShardBurst(
            at: position,
            color: brighten(color, by: 0.25),
            count: (type == .explosive || type == .boss) ? 12 : 8,
            speed: speed * 0.68,
            life: life * 0.92
        )
        spawnImpactRing(
            at: position,
            color: brighten(color, by: 0.35),
            startRadius: 3.5,
            endRadius: (type == .explosive || type == .boss) ? 38 : 26,
            lineWidth: (type == .explosive || type == .boss) ? 3.1 : 2.0,
            life: (type == .explosive || type == .boss) ? 0.28 : 0.2
        )
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
            p.blendMode = .add
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

    private func spawnImpactRing(
        at position: CGPoint,
        color: UIColor,
        startRadius: CGFloat,
        endRadius: CGFloat,
        lineWidth: CGFloat,
        life: TimeInterval
    ) {
        let ring = SKShapeNode(circleOfRadius: startRadius)
        ring.fillColor = .clear
        ring.strokeColor = color.withAlphaComponent(0.82)
        ring.lineWidth = lineWidth
        ring.position = position
        ring.zPosition = 52
        ring.blendMode = .add
        addChild(ring)

        let grow = SKAction.customAction(withDuration: life) { node, elapsed in
            let t = CGFloat(elapsed / CGFloat(life))
            let radius = startRadius + (endRadius - startRadius) * t
            if let shape = node as? SKShapeNode {
                shape.path = CGPath(
                    ellipseIn: CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2),
                    transform: nil
                )
                shape.alpha = 1 - t
            }
        }
        ring.run(SKAction.sequence([grow, SKAction.removeFromParent()]))
    }

    private func spawnShardBurst(
        at position: CGPoint,
        color: UIColor,
        count: Int,
        speed: CGFloat,
        life: TimeInterval
    ) {
        for _ in 0..<count {
            let w = CGFloat.random(in: 2.2...5.8)
            let h = CGFloat.random(in: 1.4...3.8)
            let shard = SKShapeNode(rectOf: CGSize(width: w, height: h), cornerRadius: min(w, h) * 0.25)
            shard.fillColor = color
            shard.strokeColor = color
            shard.lineWidth = 0
            shard.position = position
            shard.zPosition = 51
            shard.blendMode = .add
            addChild(shard)

            let angle = CGFloat.random(in: 0..<(CGFloat.pi * 2))
            let dist = speed * CGFloat(life) * CGFloat.random(in: 0.45...1.0)
            let dx = CGFloat(Darwin.cos(Double(angle))) * dist
            let dy = CGFloat(Darwin.sin(Double(angle))) * dist
            let rotate = SKAction.rotate(byAngle: CGFloat.random(in: -1.4...1.4), duration: life)
            rotate.timingMode = .easeOut
            let move = SKAction.moveBy(x: dx, y: dy, duration: life)
            move.timingMode = .easeOut
            let fade = SKAction.fadeOut(withDuration: life)
            fade.timingMode = .easeOut
            shard.run(SKAction.sequence([
                SKAction.group([move, fade, rotate]),
                SKAction.removeFromParent()
            ]))
        }
    }
}

private final class BallVisualNode: SKNode {
    private let shadowNode = SKShapeNode()
    private let glowNode = SKShapeNode()
    private let coreNode = SKShapeNode()
    private let rimNode = SKShapeNode()
    private let specNode = SKShapeNode()
    private let trailNodes: [SKShapeNode] = (0..<4).map { _ in SKShapeNode() }
    private var lastPositionPoint: CGPoint?

    override init() {
        super.init()

        shadowNode.isAntialiased = false
        shadowNode.fillColor = UIColor.black.withAlphaComponent(0.26)
        shadowNode.strokeColor = .clear
        shadowNode.lineWidth = 0
        shadowNode.zPosition = -3
        addChild(shadowNode)

        glowNode.isAntialiased = false
        glowNode.fillColor = UIColor.white.withAlphaComponent(0.18)
        glowNode.strokeColor = .clear
        glowNode.lineWidth = 0
        glowNode.blendMode = .add
        glowNode.zPosition = -2
        addChild(glowNode)

        coreNode.isAntialiased = false
        coreNode.lineWidth = 0
        coreNode.zPosition = 0
        addChild(coreNode)

        rimNode.fillColor = .clear
        rimNode.zPosition = 1
        addChild(rimNode)

        specNode.fillColor = UIColor.white.withAlphaComponent(0.75)
        specNode.strokeColor = .clear
        specNode.zPosition = 2
        addChild(specNode)

        for node in trailNodes {
            node.lineWidth = 0
            node.strokeColor = .clear
            node.zPosition = -1
            node.blendMode = .add
            node.isHidden = true
            addChild(node)
        }
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func apply(
        ball: Ball,
        worldWidth: Float,
        worldHeight: Float,
        sceneSize: CGSize,
        pixelScale: CGFloat,
        shakeOffset: CGPoint
    ) {
        let x = CGFloat(ball.x / worldWidth) * sceneSize.width
        let y = CGFloat(ball.y / worldHeight) * sceneSize.height
        let scenePoint = snappedPoint(CGPoint(x: x + shakeOffset.x, y: y + shakeOffset.y), scale: pixelScale)
        position = scenePoint

        let r = max(5.0, snappedLength(CGFloat(ball.radius / worldWidth) * sceneSize.width * 1.28, scale: pixelScale))
        let baseRect = CGRect(x: -r, y: -r, width: r * 2, height: r * 2)
        let speed = CGFloat(sqrt(ball.vx * ball.vx + ball.vy * ball.vy))

        let style = styleForBall(ball)
        let coreColor = style.core
        let rimColor = style.rim
        let glowColor = style.glow

        shadowNode.path = CGPath(ellipseIn: CGRect(x: -r * 0.82, y: -r * 0.8, width: r * 1.64, height: r * 1.36), transform: nil)
        shadowNode.position = snappedPoint(CGPoint(x: r * 0.12, y: -r * 0.12), scale: pixelScale)

        glowNode.path = CGPath(ellipseIn: CGRect(x: -r * 1.18, y: -r * 1.18, width: r * 2.36, height: r * 2.36), transform: nil)
        glowNode.fillColor = glowColor.withAlphaComponent(ball.isFireball ? 0.2 : 0.12)

        coreNode.path = CGPath(ellipseIn: baseRect, transform: nil)
        coreNode.fillColor = coreColor
        coreNode.alpha = ball.isFireball ? 0.98 : 0.95

        rimNode.path = CGPath(ellipseIn: CGRect(x: -r * 0.94, y: -r * 0.94, width: r * 1.88, height: r * 1.88), transform: nil)
        rimNode.strokeColor = rimColor
        rimNode.lineWidth = max(1.2, r * 0.17)
        rimNode.alpha = 0.92

        specNode.path = CGPath(ellipseIn: CGRect(x: -r * 0.34, y: r * 0.08, width: r * 0.78, height: r * 0.54), transform: nil)
        specNode.fillColor = UIColor.white.withAlphaComponent(ball.isFireball ? 0.55 : 0.72)

        updateTrail(
            speed: speed,
            radius: r,
            scenePoint: scenePoint,
            color: glowColor.withAlphaComponent(ball.isFireball ? 0.42 : 0.28)
        )

        if ball.isFireball {
            zRotation += 0.07
            setScale(1.0 + min(0.08, speed * 0.00085))
        } else {
            zRotation = 0
            setScale(1.0)
        }
    }

    private func updateTrail(speed: CGFloat, radius: CGFloat, scenePoint: CGPoint, color: UIColor) {
        guard let last = lastPositionPoint else {
            lastPositionPoint = scenePoint
            for node in trailNodes { node.isHidden = true }
            return
        }

        let dx = scenePoint.x - last.x
        let dy = scenePoint.y - last.y
        let motion = sqrt(dx * dx + dy * dy)
        let shouldShow = motion > 0.16 || speed > 24

        for (idx, node) in trailNodes.enumerated() {
            if !shouldShow {
                node.isHidden = true
                continue
            }
            let step = CGFloat(idx + 1)
            let t = step / CGFloat(trailNodes.count + 1)
            let rr = max(1.4, radius * (0.72 - t * 0.42))
            node.path = CGPath(ellipseIn: CGRect(x: -rr, y: -rr, width: rr * 2, height: rr * 2), transform: nil)
            node.position = CGPoint(x: -dx * step * 0.8, y: -dy * step * 0.8)
            node.fillColor = color
            node.alpha = max(0.1, (1 - t) * 0.32)
            node.isHidden = false
        }

        lastPositionPoint = scenePoint
    }

    private func styleForBall(_ ball: Ball) -> (core: UIColor, rim: UIColor, glow: UIColor) {
        if ball.isFireball {
            return (
                core: UIColor(red: 1.0, green: 0.42, blue: 0.10, alpha: 1.0),
                rim: UIColor(red: 1.0, green: 0.80, blue: 0.25, alpha: 1.0),
                glow: UIColor(red: 1.0, green: 0.34, blue: 0.08, alpha: 1.0)
            )
        }
        if ball.isPiercing {
            return (
                core: UIColor(red: 0.62, green: 0.90, blue: 1.0, alpha: 1.0),
                rim: UIColor(red: 0.95, green: 1.0, blue: 1.0, alpha: 1.0),
                glow: UIColor(red: 0.32, green: 0.84, blue: 1.0, alpha: 1.0)
            )
        }
        return (
            core: UIColor(red: 0.88, green: 0.91, blue: 0.98, alpha: 1.0),
            rim: UIColor(red: 0.98, green: 0.99, blue: 1.0, alpha: 1.0),
            glow: UIColor(red: 0.42, green: 0.72, blue: 1.0, alpha: 1.0)
        )
    }

    private func snappedPoint(_ point: CGPoint, scale: CGFloat) -> CGPoint {
        CGPoint(x: snappedLength(point.x, scale: scale), y: snappedLength(point.y, scale: scale))
    }

    private func snappedLength(_ value: CGFloat, scale: CGFloat) -> CGFloat {
        guard scale > 0 else { return value }
        return (value * scale).rounded() / scale
    }
}

private final class BrickVisualNode: SKNode {
    private let shadow = SKShapeNode()
    private let glow = SKShapeNode()
    private let base = SKShapeNode()
    private let bevelTop = SKShapeNode()
    private let bevelBottom = SKShapeNode()
    private let shine = SKShapeNode()
    private let border = SKShapeNode()
    private let glyph = SKShapeNode()
    private let cracks = SKShapeNode()
    private let wobbleSeed = CGFloat.random(in: 0...(CGFloat.pi * 2))

    override init() {
        super.init()

        shadow.isAntialiased = false
        shadow.lineWidth = 0
        shadow.fillColor = UIColor.black.withAlphaComponent(0.16)
        shadow.strokeColor = .clear
        shadow.zPosition = -2
        addChild(shadow)

        glow.isAntialiased = false
        glow.lineWidth = 0
        glow.alpha = 0.14
        glow.zPosition = -1
        addChild(glow)

        base.isAntialiased = false
        base.lineWidth = 0
        base.zPosition = 0
        addChild(base)

        bevelTop.isAntialiased = false
        bevelTop.lineWidth = 0
        bevelTop.zPosition = 1
        addChild(bevelTop)

        bevelBottom.isAntialiased = false
        bevelBottom.lineWidth = 0
        bevelBottom.zPosition = 1
        addChild(bevelBottom)

        shine.isAntialiased = false
        shine.lineWidth = 0
        shine.zPosition = 2
        addChild(shine)

        border.isAntialiased = false
        border.fillColor = .clear
        border.zPosition = 3
        addChild(border)

        glyph.lineWidth = 0
        glyph.zPosition = 4
        addChild(glyph)

        cracks.fillColor = .clear
        cracks.strokeColor = UIColor.white.withAlphaComponent(0.14)
        cracks.lineWidth = 1.2
        cracks.zPosition = 5
        addChild(cracks)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func pulseHit() {
        removeAction(forKey: "brickHitPulse")
        run(
            SKAction.sequence([
                SKAction.scale(to: 1.1, duration: 0.05),
                SKAction.scale(to: 1.0, duration: 0.10)
            ]),
            withKey: "brickHitPulse"
        )
    }

    func apply(
        brick: Brick,
        theme: LevelTheme,
        worldWidth: Float,
        worldHeight: Float,
        sceneSize: CGSize,
        time: TimeInterval,
        pixelScale: CGFloat,
        shakeOffset: CGPoint
    ) {
        let brickX = snappedLength(CGFloat(brick.x / worldWidth) * sceneSize.width + shakeOffset.x, scale: pixelScale)
        let brickY = snappedLength(CGFloat(brick.y / worldHeight) * sceneSize.height + shakeOffset.y, scale: pixelScale)
        let width = max(8, snappedLength(CGFloat(brick.width / worldWidth) * sceneSize.width, scale: pixelScale))
        let height = max(6, snappedLength(CGFloat(brick.height / worldHeight) * sceneSize.height, scale: pixelScale))
        let corner = max(2, min(width, height) * 0.24)
        let rect = CGRect(x: -width / 2, y: -height / 2, width: width, height: height)
        let roundedPath = CGPath(
            roundedRect: rect,
            cornerWidth: corner,
            cornerHeight: corner,
            transform: nil
        )

        var posY = brickY
        if brick.type == .moving {
            posY += CGFloat(Darwin.sin(time * 4.6 + Double(wobbleSeed))) * 1.2
        }
        position = snappedPoint(CGPoint(x: brickX, y: posY), scale: pixelScale)

        let c = brick.currentColor(theme: theme)
        let baseColor = UIColor(red: CGFloat(c.red), green: CGFloat(c.green), blue: CGFloat(c.blue), alpha: 1.0)
        let hpRatio = brick.type == .unbreakable ? CGFloat(1.0) : CGFloat(brick.hitPoints) / CGFloat(max(1, brick.maxHitPoints))
        let damage = 1.0 - hpRatio
        let edgeColor = brighten(baseColor, by: 0.45)
        let glyphColor = brighten(baseColor, by: 0.7).withAlphaComponent(0.88)

        shadow.path = CGPath(
            roundedRect: rect.offsetBy(dx: width * 0.02, dy: -height * 0.03),
            cornerWidth: corner,
            cornerHeight: corner,
            transform: nil
        )

        glow.path = CGPath(
            roundedRect: rect.insetBy(dx: -0.35, dy: -0.35),
            cornerWidth: corner + 0.5,
            cornerHeight: corner + 0.5,
            transform: nil
        )
        glow.fillColor = baseColor.withAlphaComponent(brick.type == .unbreakable ? 0.05 : 0.03)

        base.path = roundedPath
        base.fillColor = darken(baseColor, by: damage * 0.42)
        base.alpha = brick.type == .phase
            ? CGFloat(0.58 + 0.34 * Darwin.sin(time * 6.2 + Double(wobbleSeed)))
            : CGFloat(0.85 + (1.0 - damage) * 0.15)

        let shineRect = CGRect(
            x: -width * 0.46,
            y: height * 0.08,
            width: width * 0.92,
            height: max(1.5, height * 0.2)
        )
        shine.path = CGPath(
            roundedRect: shineRect,
            cornerWidth: max(1, corner * 0.45),
            cornerHeight: max(1, corner * 0.45),
            transform: nil
        )
        shine.fillColor = UIColor.white.withAlphaComponent(brick.type == .unbreakable ? 0.09 : 0.05)

        let topBevelRect = CGRect(
            x: -width * 0.46,
            y: height * 0.26,
            width: width * 0.92,
            height: max(1.2, height * 0.12)
        )
        bevelTop.path = CGPath(
            roundedRect: topBevelRect,
            cornerWidth: max(1, corner * 0.32),
            cornerHeight: max(1, corner * 0.32),
            transform: nil
        )
        bevelTop.fillColor = brighten(baseColor, by: 0.22).withAlphaComponent(0.14)

        let bottomBevelRect = CGRect(
            x: -width * 0.44,
            y: -height * 0.38,
            width: width * 0.88,
            height: max(1.2, height * 0.14)
        )
        bevelBottom.path = CGPath(
            roundedRect: bottomBevelRect,
            cornerWidth: max(1, corner * 0.28),
            cornerHeight: max(1, corner * 0.28),
            transform: nil
        )
        bevelBottom.fillColor = darken(baseColor, by: 0.2).withAlphaComponent(0.14)

        border.path = roundedPath
        border.lineWidth = max(1.2, height * 0.11)
        border.strokeColor = brick.type == .unbreakable ? UIColor.white.withAlphaComponent(0.9) : edgeColor.withAlphaComponent(0.88)

        glyph.path = brickGlyphPath(type: brick.type, width: width * 0.65, height: height * 0.6)
        glyph.fillColor = glyphColor
        glyph.strokeColor = glyphColor
        glyph.lineWidth = (brick.type == .phase || brick.type == .boss || brick.type == .unbreakable) ? 1.6 : 0
        glyph.alpha = brick.type == .phase
            ? CGFloat(0.5 + 0.5 * Darwin.sin(time * 8.0 + Double(wobbleSeed) * 1.7))
            : CGFloat(0.7 + (1.0 - damage) * 0.3)

        if damage > 0.12 && brick.type != .unbreakable {
            cracks.isHidden = false
            let crackPath = CGMutablePath()
            crackPath.move(to: CGPoint(x: -width * 0.22, y: height * 0.18))
            crackPath.addLine(to: CGPoint(x: -width * 0.05, y: height * 0.02))
            crackPath.addLine(to: CGPoint(x: width * 0.06, y: height * 0.14))
            crackPath.move(to: CGPoint(x: width * 0.2, y: -height * 0.08))
            crackPath.addLine(to: CGPoint(x: width * 0.05, y: -height * 0.22))
            crackPath.addLine(to: CGPoint(x: -width * 0.08, y: -height * 0.1))
            cracks.path = crackPath
            cracks.alpha = min(0.62, damage * 0.95)
        } else {
            cracks.isHidden = true
            cracks.path = nil
        }
    }

    private func snappedPoint(_ point: CGPoint, scale: CGFloat) -> CGPoint {
        CGPoint(x: snappedLength(point.x, scale: scale), y: snappedLength(point.y, scale: scale))
    }

    private func snappedLength(_ value: CGFloat, scale: CGFloat) -> CGFloat {
        guard scale > 0 else { return value }
        return (value * scale).rounded() / scale
    }

    private func brickGlyphPath(type: BrickType, width: CGFloat, height: CGFloat) -> CGPath {
        let path = CGMutablePath()
        switch type {
        case .normal:
            path.addEllipse(in: CGRect(x: -width * 0.11, y: -height * 0.11, width: width * 0.22, height: height * 0.22))
        case .reinforced:
            path.addRect(CGRect(x: -width * 0.42, y: height * 0.1, width: width * 0.84, height: height * 0.18))
            path.addRect(CGRect(x: -width * 0.42, y: -height * 0.28, width: width * 0.84, height: height * 0.18))
        case .armored:
            path.addRect(CGRect(x: -width * 0.08, y: -height * 0.45, width: width * 0.16, height: height * 0.9))
            path.addRect(CGRect(x: -width * 0.45, y: -height * 0.08, width: width * 0.9, height: height * 0.16))
        case .explosive:
            for i in 0..<8 {
                let a = CGFloat(i) * (.pi * 2 / 8)
                let inner = CGPoint(x: CGFloat(Darwin.cos(Double(a))) * width * 0.12, y: CGFloat(Darwin.sin(Double(a))) * height * 0.12)
                let outer = CGPoint(x: CGFloat(Darwin.cos(Double(a))) * width * 0.42, y: CGFloat(Darwin.sin(Double(a))) * height * 0.42)
                path.move(to: inner)
                path.addLine(to: outer)
            }
            path.addEllipse(in: CGRect(x: -width * 0.12, y: -height * 0.12, width: width * 0.24, height: height * 0.24))
        case .unbreakable:
            let r = min(width, height) * 0.34
            for i in 0..<6 {
                let a = CGFloat(i) * (.pi * 2 / 6) - (.pi / 6)
                let p = CGPoint(x: CGFloat(Darwin.cos(Double(a))) * r, y: CGFloat(Darwin.sin(Double(a))) * r)
                if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
            }
            path.closeSubpath()
        case .moving:
            path.move(to: CGPoint(x: -width * 0.36, y: 0))
            path.addLine(to: CGPoint(x: -width * 0.1, y: height * 0.26))
            path.addLine(to: CGPoint(x: -width * 0.1, y: -height * 0.26))
            path.closeSubpath()
            path.move(to: CGPoint(x: width * 0.36, y: 0))
            path.addLine(to: CGPoint(x: width * 0.1, y: height * 0.26))
            path.addLine(to: CGPoint(x: width * 0.1, y: -height * 0.26))
            path.closeSubpath()
        case .spawning:
            path.addEllipse(in: CGRect(x: -width * 0.38, y: -height * 0.1, width: width * 0.24, height: height * 0.24))
            path.addEllipse(in: CGRect(x: -width * 0.1, y: -height * 0.24, width: width * 0.24, height: height * 0.24))
            path.addEllipse(in: CGRect(x: width * 0.18, y: -height * 0.1, width: width * 0.24, height: height * 0.24))
        case .phase:
            path.addRect(CGRect(x: -width * 0.32, y: -height * 0.36, width: width * 0.16, height: height * 0.72))
            path.addRect(CGRect(x: -width * 0.08, y: -height * 0.24, width: width * 0.16, height: height * 0.48))
            path.addRect(CGRect(x: width * 0.16, y: -height * 0.36, width: width * 0.16, height: height * 0.72))
        case .boss:
            path.move(to: CGPoint(x: -width * 0.4, y: -height * 0.22))
            path.addLine(to: CGPoint(x: -width * 0.24, y: height * 0.28))
            path.addLine(to: CGPoint(x: 0, y: -height * 0.04))
            path.addLine(to: CGPoint(x: width * 0.24, y: height * 0.28))
            path.addLine(to: CGPoint(x: width * 0.4, y: -height * 0.22))
            path.closeSubpath()
        case .invader:
            path.addRoundedRect(in: CGRect(x: -width * 0.42, y: -height * 0.16, width: width * 0.84, height: height * 0.34), cornerWidth: width * 0.14, cornerHeight: height * 0.14, transform: .identity)
            path.addEllipse(in: CGRect(x: -width * 0.3, y: -height * 0.05, width: width * 0.12, height: height * 0.12))
            path.addEllipse(in: CGRect(x: width * 0.18, y: -height * 0.05, width: width * 0.12, height: height * 0.12))
        }
        return path
    }
}

private final class PowerupVisualNode: SKNode {
    private let shadow = SKShapeNode()
    private let aura = SKShapeNode()
    private let ring = SKShapeNode()
    private let orbit = SKShapeNode()
    private let core = SKShapeNode()
    private let icon = SKShapeNode()
    private let gleam = SKShapeNode()
    private let spark = SKShapeNode()
    private let phaseSeed = CGFloat.random(in: 0...(CGFloat.pi * 2))

    override init() {
        super.init()

        shadow.isAntialiased = false
        shadow.lineWidth = 0
        shadow.fillColor = UIColor.black.withAlphaComponent(0.26)
        shadow.strokeColor = .clear
        shadow.zPosition = -1
        addChild(shadow)

        aura.isAntialiased = false
        aura.lineWidth = 0
        aura.zPosition = 0
        addChild(aura)

        ring.fillColor = .clear
        ring.zPosition = 1
        addChild(ring)

        orbit.fillColor = .clear
        orbit.zPosition = 2
        orbit.lineCap = .round
        addChild(orbit)

        core.isAntialiased = false
        core.lineWidth = 0
        core.zPosition = 3
        addChild(core)

        gleam.lineWidth = 0
        gleam.zPosition = 4
        addChild(gleam)

        icon.lineWidth = 0
        icon.zPosition = 5
        addChild(icon)

        spark.fillColor = UIColor.white.withAlphaComponent(0.8)
        spark.strokeColor = .clear
        spark.zPosition = 6
        addChild(spark)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func apply(
        powerup: PowerUp,
        worldWidth: Float,
        worldHeight: Float,
        sceneSize: CGSize,
        time: TimeInterval,
        pixelScale: CGFloat,
        shakeOffset: CGPoint
    ) {
        let px = CGFloat(powerup.x / worldWidth) * sceneSize.width + shakeOffset.x
        let py = CGFloat(powerup.y / worldHeight) * sceneSize.height + shakeOffset.y
        position = snappedPoint(CGPoint(x: px, y: py), scale: pixelScale)

        let baseColor = UIColor(
            red: CGFloat(powerup.type.color.red),
            green: CGFloat(powerup.type.color.green),
            blue: CGFloat(powerup.type.color.blue),
            alpha: 1.0
        )
        let pulse = 0.88 + 0.14 * CGFloat(Darwin.sin(time * 8.2 + Double(phaseSeed)))
        let radius = max(5.8, snappedLength(CGFloat(powerup.width / worldWidth) * sceneSize.width * 0.92, scale: pixelScale))

        shadow.path = CGPath(ellipseIn: CGRect(x: -radius * 0.86, y: -radius * 0.56, width: radius * 1.72, height: radius * 0.98), transform: nil)
        shadow.position = snappedPoint(CGPoint(x: radius * 0.12, y: -radius * 0.16), scale: pixelScale)

        aura.path = CGPath(ellipseIn: CGRect(x: -radius * 1.22, y: -radius * 1.22, width: radius * 2.44, height: radius * 2.44), transform: nil)
        aura.fillColor = baseColor.withAlphaComponent(0.08)
        aura.alpha = 0.46 * pulse

        ring.path = CGPath(ellipseIn: CGRect(x: -radius * 1.05, y: -radius * 1.05, width: radius * 2.1, height: radius * 2.1), transform: nil)
        ring.strokeColor = brighten(baseColor, by: 0.38).withAlphaComponent(0.85)
        ring.lineWidth = max(1.2, radius * 0.16)

        orbit.path = CGPath(
            ellipseIn: CGRect(x: -radius * 1.24, y: -radius * 0.86, width: radius * 2.48, height: radius * 1.72),
            transform: nil
        )
        orbit.strokeColor = brighten(baseColor, by: 0.56).withAlphaComponent(0.52)
        orbit.lineWidth = max(1.0, radius * 0.08)
        orbit.glowWidth = radius * 0.06

        core.path = CGPath(ellipseIn: CGRect(x: -radius * 0.92, y: -radius * 0.92, width: radius * 1.84, height: radius * 1.84), transform: nil)
        core.fillColor = darken(baseColor, by: 0.22)
        core.alpha = pulse

        gleam.path = CGPath(
            roundedRect: CGRect(x: -radius * 0.6, y: radius * 0.08, width: radius * 1.2, height: radius * 0.36),
            cornerWidth: radius * 0.18,
            cornerHeight: radius * 0.18,
            transform: nil
        )
        gleam.fillColor = UIColor.white.withAlphaComponent(0.13)

        icon.path = powerupGlyphPath(type: powerup.type, radius: radius * 0.9)
        icon.fillColor = brighten(baseColor, by: 0.62)
        icon.strokeColor = UIColor.white.withAlphaComponent(0.88)
        icon.lineWidth = (powerup.type == .freeze || powerup.type == .pierce || powerup.type == .magnet || powerup.type == .ricochet || powerup.type == .timeWarp) ? max(1.1, radius * 0.09) : 0
        spark.path = CGPath(ellipseIn: CGRect(x: radius * 0.22, y: radius * 0.24, width: radius * 0.24, height: radius * 0.24), transform: nil)
        spark.alpha = 0.5 + 0.45 * CGFloat(Darwin.sin(time * 5.4 + Double(phaseSeed)))

        orbit.zRotation = CGFloat(time * 1.45 + Double(phaseSeed))
        zRotation = CGFloat(Darwin.sin(time * 2.1 + Double(phaseSeed))) * 0.06
        xScale = pulse
        yScale = pulse
    }

    private func snappedPoint(_ point: CGPoint, scale: CGFloat) -> CGPoint {
        CGPoint(x: snappedLength(point.x, scale: scale), y: snappedLength(point.y, scale: scale))
    }

    private func snappedLength(_ value: CGFloat, scale: CGFloat) -> CGFloat {
        guard scale > 0 else { return value }
        return (value * scale).rounded() / scale
    }

    private func powerupGlyphPath(type: PowerUpType, radius: CGFloat) -> CGPath {
        let path = CGMutablePath()
        switch type {
        case .multiBall:
            path.addEllipse(in: CGRect(x: -radius * 0.38, y: -radius * 0.18, width: radius * 0.4, height: radius * 0.4))
            path.addEllipse(in: CGRect(x: radius * 0.02, y: -radius * 0.1, width: radius * 0.36, height: radius * 0.36))
            path.addEllipse(in: CGRect(x: -radius * 0.06, y: radius * 0.14, width: radius * 0.32, height: radius * 0.32))
        case .laser:
            path.move(to: CGPoint(x: -radius * 0.44, y: radius * 0.26))
            path.addLine(to: CGPoint(x: radius * 0.1, y: radius * 0.26))
            path.addLine(to: CGPoint(x: -radius * 0.1, y: radius * 0.52))
            path.addLine(to: CGPoint(x: radius * 0.44, y: -radius * 0.26))
            path.addLine(to: CGPoint(x: -radius * 0.1, y: -radius * 0.26))
            path.addLine(to: CGPoint(x: radius * 0.1, y: -radius * 0.52))
            path.closeSubpath()
        case .guardrail:
            path.addRect(CGRect(x: -radius * 0.46, y: -radius * 0.08, width: radius * 0.92, height: radius * 0.16))
            path.addRect(CGRect(x: -radius * 0.38, y: radius * 0.16, width: radius * 0.76, height: radius * 0.12))
            path.addRect(CGRect(x: -radius * 0.38, y: -radius * 0.28, width: radius * 0.76, height: radius * 0.12))
        case .shield:
            path.move(to: CGPoint(x: 0, y: radius * 0.52))
            path.addLine(to: CGPoint(x: -radius * 0.42, y: radius * 0.1))
            path.addLine(to: CGPoint(x: -radius * 0.28, y: -radius * 0.44))
            path.addLine(to: CGPoint(x: radius * 0.28, y: -radius * 0.44))
            path.addLine(to: CGPoint(x: radius * 0.42, y: radius * 0.1))
            path.closeSubpath()
        case .extraLife:
            path.addEllipse(in: CGRect(x: -radius * 0.26, y: radius * 0.06, width: radius * 0.3, height: radius * 0.3))
            path.addEllipse(in: CGRect(x: radius * -0.02, y: radius * 0.06, width: radius * 0.3, height: radius * 0.3))
            path.move(to: CGPoint(x: -radius * 0.38, y: radius * 0.2))
            path.addLine(to: CGPoint(x: 0, y: -radius * 0.42))
            path.addLine(to: CGPoint(x: radius * 0.38, y: radius * 0.2))
            path.closeSubpath()
        case .widePaddle:
            path.addRoundedRect(in: CGRect(x: -radius * 0.56, y: -radius * 0.16, width: radius * 1.12, height: radius * 0.32), cornerWidth: radius * 0.12, cornerHeight: radius * 0.12, transform: .identity)
            path.addRect(CGRect(x: -radius * 0.1, y: -radius * 0.44, width: radius * 0.2, height: radius * 0.88))
        case .shrink:
            path.addRoundedRect(in: CGRect(x: -radius * 0.42, y: -radius * 0.12, width: radius * 0.84, height: radius * 0.24), cornerWidth: radius * 0.1, cornerHeight: radius * 0.1, transform: .identity)
            path.move(to: CGPoint(x: -radius * 0.42, y: radius * 0.36))
            path.addLine(to: CGPoint(x: radius * 0.42, y: radius * 0.36))
            path.addLine(to: CGPoint(x: radius * 0.26, y: radius * 0.22))
            path.move(to: CGPoint(x: -radius * 0.42, y: -radius * 0.36))
            path.addLine(to: CGPoint(x: radius * 0.42, y: -radius * 0.36))
            path.addLine(to: CGPoint(x: radius * 0.26, y: -radius * 0.22))
        case .slowMotion:
            path.addEllipse(in: CGRect(x: -radius * 0.46, y: -radius * 0.46, width: radius * 0.92, height: radius * 0.92))
            path.addRect(CGRect(x: -radius * 0.04, y: -radius * 0.02, width: radius * 0.08, height: radius * 0.3))
            path.addRect(CGRect(x: -radius * 0.02, y: -radius * 0.04, width: radius * 0.24, height: radius * 0.08))
        case .overdrive:
            path.move(to: CGPoint(x: -radius * 0.5, y: -radius * 0.34))
            path.addLine(to: CGPoint(x: radius * 0.04, y: -radius * 0.04))
            path.addLine(to: CGPoint(x: -radius * 0.08, y: radius * 0.18))
            path.addLine(to: CGPoint(x: radius * 0.5, y: radius * 0.36))
            path.addLine(to: CGPoint(x: -radius * 0.04, y: radius * 0.06))
            path.addLine(to: CGPoint(x: radius * 0.08, y: -radius * 0.18))
            path.closeSubpath()
        case .fireball:
            path.addEllipse(in: CGRect(x: -radius * 0.36, y: -radius * 0.28, width: radius * 0.72, height: radius * 0.72))
            path.move(to: CGPoint(x: radius * 0.18, y: radius * 0.42))
            path.addLine(to: CGPoint(x: radius * 0.46, y: radius * 0.66))
            path.addLine(to: CGPoint(x: radius * 0.34, y: radius * 0.28))
            path.closeSubpath()
        case .magnet:
            path.addArc(center: CGPoint.zero, radius: radius * 0.42, startAngle: .pi * 0.15, endAngle: .pi * 1.85, clockwise: false)
            path.addRect(CGRect(x: -radius * 0.5, y: -radius * 0.44, width: radius * 0.16, height: radius * 0.22))
            path.addRect(CGRect(x: radius * 0.34, y: -radius * 0.44, width: radius * 0.16, height: radius * 0.22))
        case .gravityWell:
            path.addEllipse(in: CGRect(x: -radius * 0.44, y: -radius * 0.44, width: radius * 0.88, height: radius * 0.88))
            path.addEllipse(in: CGRect(x: -radius * 0.22, y: -radius * 0.22, width: radius * 0.44, height: radius * 0.44))
        case .ballSplitter:
            path.addRect(CGRect(x: -radius * 0.05, y: -radius * 0.48, width: radius * 0.1, height: radius * 0.96))
            path.addRect(CGRect(x: -radius * 0.48, y: -radius * 0.05, width: radius * 0.96, height: radius * 0.1))
            path.addEllipse(in: CGRect(x: -radius * 0.18, y: -radius * 0.18, width: radius * 0.36, height: radius * 0.36))
        case .freeze:
            for i in 0..<3 {
                let a = CGFloat(i) * (.pi / 3)
                let dx = CGFloat(Darwin.cos(Double(a))) * radius * 0.44
                let dy = CGFloat(Darwin.sin(Double(a))) * radius * 0.44
                path.move(to: CGPoint(x: -dx, y: -dy))
                path.addLine(to: CGPoint(x: dx, y: dy))
            }
        case .pierce:
            path.move(to: CGPoint(x: 0, y: radius * 0.58))
            path.addLine(to: CGPoint(x: -radius * 0.24, y: radius * 0.18))
            path.addLine(to: CGPoint(x: -radius * 0.1, y: radius * 0.18))
            path.addLine(to: CGPoint(x: -radius * 0.1, y: -radius * 0.52))
            path.addLine(to: CGPoint(x: radius * 0.1, y: -radius * 0.52))
            path.addLine(to: CGPoint(x: radius * 0.1, y: radius * 0.18))
            path.addLine(to: CGPoint(x: radius * 0.24, y: radius * 0.18))
            path.closeSubpath()
        case .ricochet:
            path.move(to: CGPoint(x: -radius * 0.48, y: -radius * 0.18))
            path.addLine(to: CGPoint(x: -radius * 0.1, y: -radius * 0.18))
            path.addLine(to: CGPoint(x: -radius * 0.2, y: -radius * 0.32))
            path.move(to: CGPoint(x: -radius * 0.18, y: -radius * 0.42))
            path.addLine(to: CGPoint(x: -radius * 0.18, y: -radius * 0.06))
            path.move(to: CGPoint(x: radius * 0.48, y: radius * 0.18))
            path.addLine(to: CGPoint(x: radius * 0.1, y: radius * 0.18))
            path.addLine(to: CGPoint(x: radius * 0.2, y: radius * 0.32))
            path.move(to: CGPoint(x: radius * 0.18, y: radius * 0.42))
            path.addLine(to: CGPoint(x: radius * 0.18, y: radius * 0.06))
        case .timeWarp:
            path.addEllipse(in: CGRect(x: -radius * 0.46, y: -radius * 0.46, width: radius * 0.92, height: radius * 0.92))
            path.move(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: 0, y: radius * 0.26))
            path.move(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: -radius * 0.2, y: 0))
            path.addEllipse(in: CGRect(x: radius * 0.14, y: radius * 0.14, width: radius * 0.12, height: radius * 0.12))
        case .doubleScore:
            path.move(to: CGPoint(x: -radius * 0.42, y: 0))
            path.addLine(to: CGPoint(x: radius * 0.42, y: 0))
            path.move(to: CGPoint(x: 0, y: -radius * 0.42))
            path.addLine(to: CGPoint(x: 0, y: radius * 0.42))
            path.addRect(CGRect(x: -radius * 0.22, y: -radius * 0.22, width: radius * 0.14, height: radius * 0.14))
            path.addRect(CGRect(x: radius * 0.08, y: -radius * 0.22, width: radius * 0.14, height: radius * 0.14))
            path.addRect(CGRect(x: -radius * 0.22, y: radius * 0.08, width: radius * 0.14, height: radius * 0.14))
            path.addRect(CGRect(x: radius * 0.08, y: radius * 0.08, width: radius * 0.14, height: radius * 0.14))
        }
        return path
    }
}

private func brighten(_ color: UIColor, by amount: CGFloat) -> UIColor {
    var h: CGFloat = 0
    var s: CGFloat = 0
    var b: CGFloat = 0
    var a: CGFloat = 0
    guard color.getHue(&h, saturation: &s, brightness: &b, alpha: &a) else {
        return color
    }
    return UIColor(hue: h, saturation: max(0, s - amount * 0.12), brightness: min(1, b + amount), alpha: a)
}

private func darken(_ color: UIColor, by amount: CGFloat) -> UIColor {
    var h: CGFloat = 0
    var s: CGFloat = 0
    var b: CGFloat = 0
    var a: CGFloat = 0
    guard color.getHue(&h, saturation: &s, brightness: &b, alpha: &a) else {
        return color
    }
    return UIColor(hue: h, saturation: min(1, s + amount * 0.2), brightness: max(0, b - amount), alpha: a)
}

private struct PowerupChip: View {
    let status: PowerupStatus

    var body: some View {
        HStack(spacing: 4) {
            Text(status.type.displayName)
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            if status.remainingSeconds > 0 {
                Text("\(status.remainingSeconds)s")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.yellow)
            }
            if status.charges > 0 {
                Text("(\(status.charges))")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.cyan)
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(Color(red: Double(status.type.color.red), green: Double(status.type.color.green), blue: Double(status.type.color.blue)).opacity(0.8))
        .cornerRadius(6)
    }
}

#Preview {
    GameView()
        .environmentObject(GameViewModel())
}
