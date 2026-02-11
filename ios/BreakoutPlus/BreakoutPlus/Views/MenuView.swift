//
//  MenuView.swift
//  BreakoutPlus
//
//  Main menu with game mode selection - port of Android MainActivity
//

import SwiftUI

struct MenuView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            GeometryReader { geometry in
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 26) {
                        // Title
                        VStack(spacing: 10) {
                            Text("BREAKOUT")
                                .font(.system(size: 48, weight: .bold))
                                .foregroundColor(Color(hex: "31E1F7"))

                            Text("+")
                                .font(.system(size: 36, weight: .bold))
                                .foregroundColor(Color(hex: "FF4FD8"))
                                .offset(y: -8)

                            Text("PREMIUM")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(hex: "FFFFFF").opacity(0.7))
                        }

                        Text("Pick a mode tailored to your play style.")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))

                        // Game Mode Buttons
                        VStack(spacing: 15) {
                            ForEach(GameMode.allCases) { mode in
                                GameModeButton(mode: mode) {
                                    gameViewModel.startGame(mode: mode)
                                }
                            }
                        }

                        // Bottom buttons
                        VStack(spacing: 12) {
                            HStack(spacing: 20) {
                                Button(action: {
                                    gameViewModel.navigateToDailyChallenges()
                                }) {
                                    Text("Challenges")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color(hex: "31E1F7"))
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 10)
                                        .background(Color(hex: "1A1F26"))
                                        .cornerRadius(8)
                                }

                                Button(action: {
                                    gameViewModel.navigateToScoreboard()
                                }) {
                                    Text("Scores")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color(hex: "31E1F7"))
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 10)
                                        .background(Color(hex: "1A1F26"))
                                        .cornerRadius(8)
                                }
                            }

                            HStack(spacing: 20) {
                                Button(action: {
                                    gameViewModel.navigateToSettings()
                                }) {
                                    Text("Settings")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color(hex: "31E1F7"))
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 10)
                                        .background(Color(hex: "1A1F26"))
                                        .cornerRadius(8)
                                }

                                Button(action: {
                                    gameViewModel.navigateToHowTo()
                                }) {
                                    Text("How To")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color(hex: "31E1F7"))
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 10)
                                        .background(Color(hex: "1A1F26"))
                                        .cornerRadius(8)
                                }
                            }
                        }
                    }
                    .padding(.vertical, 40)
                    .padding(.horizontal, 20)
                    .frame(minHeight: geometry.size.height, alignment: .top)
                }
            }
        }
    }
}

struct GameModeButton: View {
    let mode: GameMode
    let action: () -> Void

    private var accent: Color {
        switch mode {
        case .classic: return Color(hex: "31E1F7")
        case .timed: return Color(hex: "FFC857")
        case .endless: return Color(hex: "2CEAA3")
        case .god: return Color(hex: "FF4FD8")
        case .rush: return Color(hex: "FF5D5D")
        case .volley: return Color(hex: "4ECDC4") // Teal for volley
        case .tunnel: return Color(hex: "4AA3FF")
        case .survival: return Color(hex: "FF8A3D")
        case .invaders: return Color(hex: "9C6ADE") // Purple/violet for invaders
        }
    }

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Rectangle()
                    .fill(accent)
                    .frame(width: 46, height: 4)
                    .cornerRadius(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(mode.displayName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(accent)

                Text(mode.meta)
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "FFFFFF").opacity(0.7))

                Text(mode.description)
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 10)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(hex: "1A1F26"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(accent.opacity(0.35), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(PressableCardButtonStyle())
    }
}

private struct PressableCardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.easeInOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    MenuView()
        .environmentObject(GameViewModel())
}
