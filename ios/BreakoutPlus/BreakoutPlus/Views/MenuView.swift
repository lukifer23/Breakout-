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

            VStack(spacing: 30) {
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

                Spacer()

                // Game Mode Buttons
                VStack(spacing: 15) {
                    ForEach(GameMode.allCases) { mode in
                        GameModeButton(mode: mode) {
                            gameViewModel.startGame(mode: mode)
                        }
                    }
                }

                Spacer()

                // Bottom buttons
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
            .padding(.vertical, 50)
            .padding(.horizontal, 20)
        }
    }
}

struct GameModeButton: View {
    let mode: GameMode
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text(mode.displayName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)

                Text(mode.meta)
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "FFFFFF").opacity(0.7))

                Text(mode.description)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "31E1F7"))
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
                            .stroke(Color(hex: "31E1F7").opacity(0.3), lineWidth: 1)
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
