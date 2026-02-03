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

    var body: some View {
        ZStack {
            // SpriteKit Game Scene
            SpriteView(scene: GameScene(viewModel: gameViewModel))
                .edgesIgnoringSafeArea(.all)

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

                    if gameViewModel.timeRemaining > 0 {
                        Text(timeString(from: gameViewModel.timeRemaining))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(gameViewModel.timeRemaining <= 10 ? .red : .white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(8)
                    }

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

                    if let powerup = gameViewModel.activePowerup {
                        Text(powerup.displayName)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.green)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(6)
                    }
                }
                .padding(.bottom, 30)
                .padding(.horizontal, 20)
            }
        }
        .navigationBarHidden(true)
        .statusBar(hidden: true)
    }

    private func timeString(from seconds: Int) -> String {
        let minutes = seconds / 60
        let remainingSeconds = seconds % 60
        return String(format: "%d:%02d", minutes, remainingSeconds)
    }
}

class GameScene: SKScene, GameEngineDelegate {
    private var gameEngine: GameEngine!
    private var viewModel: GameViewModel!
    private var lastUpdateTime: TimeInterval = 0

    // Visual nodes
    private var ballNodes: [SKSpriteNode] = []
    private var brickNodes: [SKSpriteNode] = []
    private var paddleNode: SKSpriteNode!
    private var powerupNodes: [SKSpriteNode] = []

    init(viewModel: GameViewModel) {
        self.viewModel = viewModel
        super.init(size: CGSize(width: 375, height: 667)) // Will be resized
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func didMove(to view: SKView) {
        setupScene()
        setupGame()
    }

    private func setupScene() {
        backgroundColor = UIColor(red: 0.06, green: 0.11, blue: 0.17, alpha: 1.0)
        scaleMode = .aspectFit
        physicsWorld.gravity = .zero
    }

    private func setupGame() {
        gameEngine = GameEngine(gameMode: viewModel.selectedGameMode)
        gameEngine.delegate = self

        // Create paddle
        paddleNode = SKSpriteNode(color: .white, size: CGSize(width: 180, height: 26))
        paddleNode.position = CGPoint(x: size.width/2, y: 80)
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

        gameEngine.update(currentTime: currentTime)
        updateVisuals()
    }

    private func updateVisuals() {
        // Update paddle
        let paddleX = CGFloat(gameEngine.paddle.x / 100.0 * size.width)
        paddleNode.position.x = paddleX
        paddleNode.size.width = CGFloat(gameEngine.paddle.width / 100.0 * size.width)

        // Update balls
        while ballNodes.count < gameEngine.balls.count {
            let ballNode = SKSpriteNode(color: .white, size: CGSize(width: 20, height: 20))
            addChild(ballNode)
            ballNodes.append(ballNode)
        }

        while ballNodes.count > gameEngine.balls.count {
            ballNodes.last?.removeFromParent()
            ballNodes.removeLast()
        }

        for (index, ball) in gameEngine.balls.enumerated() {
            if index < ballNodes.count {
                let ballX = CGFloat(ball.x / 100.0 * size.width)
                let ballY = size.height - CGFloat(ball.y / 160.0 * size.height)
                ballNodes[index].position = CGPoint(x: ballX, y: ballY)
            }
        }

        // Update bricks
        while brickNodes.count < gameEngine.bricks.count {
            let brickNode = SKSpriteNode(color: .blue, size: CGSize(width: 30, height: 15))
            addChild(brickNode)
            brickNodes.append(brickNode)
        }

        for (index, brick) in gameEngine.bricks.enumerated() {
            if index < brickNodes.count {
                let brickNode = brickNodes[index]
                let brickX = CGFloat(brick.x / 100.0 * size.width)
                let brickY = size.height - CGFloat(brick.y / 160.0 * size.height)

                brickNode.position = CGPoint(x: brickX, y: brickY)
                brickNode.size = CGSize(width: CGFloat(brick.width / 100.0 * size.width),
                                      height: CGFloat(brick.height / 160.0 * size.height))

                if brick.alive {
                    let color = brick.currentColor(theme: .neon)
                    brickNode.color = UIColor(red: CGFloat(color.red),
                                            green: CGFloat(color.green),
                                            blue: CGFloat(color.blue),
                                            alpha: 1.0)
                } else {
                    brickNode.isHidden = true
                }
            }
        }

        // Update powerups
        while powerupNodes.count < gameEngine.powerups.count {
            let powerupNode = SKSpriteNode(color: .yellow, size: CGSize(width: 20, height: 20))
            addChild(powerupNode)
            powerupNodes.append(powerupNode)
        }

        while powerupNodes.count > gameEngine.powerups.count {
            powerupNodes.last?.removeFromParent()
            powerupNodes.removeLast()
        }

        for (index, powerup) in gameEngine.powerups.enumerated() {
            if index < powerupNodes.count {
                let powerupX = CGFloat(powerup.x / 100.0 * size.width)
                let powerupY = size.height - CGFloat(powerup.y / 160.0 * size.height)
                powerupNodes[index].position = CGPoint(x: powerupX, y: powerupY)
            }
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let location = touch.location(in: self)

            // Convert to game coordinates
            let gameX = Float(location.x / size.width * 100.0)

            if gameEngine.balls.contains(where: { $0.vy == 0 }) {
                // Launch ball if it's ready
                gameEngine.launchBall()
            } else {
                // Move paddle
                gameEngine.movePaddle(to: gameX)
            }
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let location = touch.location(in: self)
            let gameX = Float(location.x / size.width * 100.0)
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
        }
    }

    func onComboAchieved(count: Int) {
        DispatchQueue.main.async {
            self.viewModel.comboCount = count
        }
    }

    func onTimeUpdated(seconds: Int) {
        DispatchQueue.main.async {
            self.viewModel.timeRemaining = seconds
        }
    }

    func onLevelComplete() {
        // Handle level completion
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.gameEngine.nextLevel()
        }
    }

    func onGameOver(finalScore: Int) {
        // Handle game over
        DispatchQueue.main.async {
            self.viewModel.exitToMenu()
        }
    }
}

#Preview {
    GameView()
        .environmentObject(GameViewModel())
}