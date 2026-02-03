//
//  MenuView.swift
//  BreakoutPlusMac
//
//  Main menu for macOS Breakout+
//  Adapted from iOS version
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

                    Text("MAC EDITION")
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

    @State private var isHovered = false

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
                    .fill(isHovered ? Color(hex: "2A2F36") : Color(hex: "1A1F26"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color(hex: "31E1F7").opacity(isHovered ? 0.6 : 0.3), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
        .onHover { hovering in
            isHovered = hovering
        }
    }
}

// Color extension (copied from iOS version)
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

#Preview {
    MenuView()
        .environmentObject(GameViewModel())
        .frame(width: 800, height: 600)
}