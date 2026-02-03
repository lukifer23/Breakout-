//
//  GameView.swift
//  BreakoutPlusMac
//
//  macOS game view with mouse controls
//  Adapted from iOS SpriteKit version
//

import SwiftUI

struct GameView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @State private var mouseLocation: CGPoint = .zero
    @State private var isMousePressed = false

    var body: some View {
        ZStack {
            // Game area background
            Color.black.edgesIgnoringSafeArea(.all)

            // Game canvas (simplified version without SpriteKit for CLI compatibility)
            GameCanvasView(gameViewModel: gameViewModel,
                          mouseLocation: mouseLocation,
                          isMousePressed: isMousePressed)

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
                .padding(.top, 20)
                .padding(.horizontal, 20)

                Spacer()

                // Controls info
                VStack(spacing: 8) {
                    Text("Controls: Move mouse to control paddle, Click to launch ball")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.7))

                    Text("Level \(gameViewModel.level)")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(6)

                    if gameViewModel.comboCount > 1 {
                        Text("Combo x\(gameViewModel.comboCount)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.yellow)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(6)
                    }
                }
                .padding(.bottom, 20)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear {
            NSEvent.addLocalMonitorForEvents(matching: [.mouseMoved, .leftMouseDown, .leftMouseUp]) { event in
                handleMouseEvent(event)
                return event
            }
        }
    }

    private func handleMouseEvent(_ event: NSEvent) {
        let window = NSApplication.shared.windows.first
        if let window = window, let location = window.mouseLocationOutsideOfEventStream {
            // Convert to view coordinates
            let viewLocation = CGPoint(x: location.x, y: window.frame.height - location.y)
            mouseLocation = viewLocation

            // Convert to game coordinates (assuming 800x600 game area)
            let gameX = Float(viewLocation.x / 800.0 * 100.0)
            // For now, just log the mouse position - we'll implement game logic next
            print("Mouse at game coordinate: \(gameX)")
        }

        switch event.type {
        case .leftMouseDown:
            isMousePressed = true
            print("Mouse clicked - would launch ball")
        case .leftMouseUp:
            isMousePressed = false
        default:
            break
        }
    }

    private func timeString(from seconds: Int) -> String {
        let minutes = seconds / 60
        let remainingSeconds = seconds % 60
        return String(format: "%d:%02d", minutes, remainingSeconds)
    }
}

// Simplified game canvas without SpriteKit for CLI compatibility
struct GameCanvasView: View {
    @ObservedObject var gameViewModel: GameViewModel
    let mouseLocation: CGPoint
    let isMousePressed: Bool

    var body: some View {
        ZStack {
            // Game background
            Color.blue.opacity(0.1)

            // Simple paddle (white rectangle)
            Rectangle()
                .fill(Color.white)
                .frame(width: 120, height: 20)
                .position(x: 400, y: 550) // Fixed position for now

            // Simple ball (white circle)
            Circle()
                .fill(Color.white)
                .frame(width: 16, height: 16)
                .position(x: 400, y: 520) // Fixed position for now

            // Simple bricks (colored rectangles)
            VStack(spacing: 5) {
                ForEach(0..<6) { row in
                    HStack(spacing: 5) {
                        ForEach(0..<10) { col in
                            Rectangle()
                                .fill(brickColor(for: row, col))
                                .frame(width: 70, height: 20)
                        }
                    }
                }
            }
            .position(x: 400, y: 200)

            // Debug info
            VStack {
                Text("Mouse: (\(Int(mouseLocation.x)), \(Int(mouseLocation.y)))")
                    .foregroundColor(.white)
                    .font(.system(size: 12))
                Text("Pressed: \(isMousePressed ? "YES" : "NO")")
                    .foregroundColor(.white)
                    .font(.system(size: 12))
            }
            .position(x: 100, y: 50)
        }
        .frame(width: 800, height: 600)
        .background(Color.black)
    }

    private func brickColor(for row: Int, col: Int) -> Color {
        let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple]
        return colors[row % colors.count].opacity(0.8)
    }
}

#Preview {
    GameView()
        .environmentObject(GameViewModel())
        .frame(width: 800, height: 600)
}