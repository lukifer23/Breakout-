//
//  MenuView.swift
//  BreakoutPlus
//
//  Main menu with game mode selection - port of Android MainActivity
//

import SwiftUI
import UIKit

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
                            BreakoutBrandMark()

                            Text("Breakout+")
                                .font(.system(size: 44, weight: .black))
                                .foregroundColor(Color(hex: "F4F7FF"))
                                .tracking(1.0)

                            Text("High velocity brickbreaker")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(Color(hex: "A6B3C9"))
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

private struct BreakoutBrandMark: View {
    var body: some View {
        if UIImage(named: "BrandMark") != nil {
            Image("BrandMark")
                .resizable()
                .renderingMode(.original)
                .interpolation(.high)
                .scaledToFit()
                .frame(width: 96, height: 96)
                .shadow(color: Color(hex: "31E1F7").opacity(0.2), radius: 8, y: 2)
        } else {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "18233C"), Color(hex: "0B1220")]),
                            center: .center,
                            startRadius: 6,
                            endRadius: 46
                        )
                    )

                Circle()
                    .stroke(Color(hex: "31E1F7"), lineWidth: 5.5)
                    .frame(width: 64, height: 64)

                Rectangle()
                    .fill(Color(hex: "31E1F7"))
                    .frame(width: 5, height: 28)

                Rectangle()
                    .fill(Color(hex: "31E1F7"))
                    .frame(width: 28, height: 5)

                Circle()
                    .fill(Color(hex: "FF4FD8"))
                    .frame(width: 14, height: 14)
                    .offset(x: 22, y: -20)

                RoundedRectangle(cornerRadius: 2)
                    .fill(Color(hex: "FFC857"))
                    .frame(width: 42, height: 7)
                    .offset(y: 24)
            }
            .frame(width: 92, height: 92)
            .shadow(color: Color(hex: "31E1F7").opacity(0.2), radius: 8, y: 2)
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
        case .zen: return Color(hex: "6EE7B7")
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
