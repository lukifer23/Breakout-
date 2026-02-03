//
//  HowToView.swift
//  BreakoutPlusMac
//

import SwiftUI

struct HowToView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack {
                Text("How to Play")
                    .font(.largeTitle)
                    .foregroundColor(.white)

                VStack(alignment: .leading, spacing: 15) {
                    Text("🎮 Controls:")
                        .font(.headline)
                        .foregroundColor(Color(hex: "31E1F7"))

                    Text("• Move mouse to control paddle")
                    Text("• Click mouse to launch ball")
                    Text("• Break all bricks to advance levels")

                    Text("🎯 Objective:")
                        .font(.headline)
                        .foregroundColor(Color(hex: "31E1F7"))
                        .padding(.top, 10)

                    Text("Destroy all bricks by bouncing the ball off your paddle. Don't let the ball fall off the bottom!")

                    Text("⚡ Power-ups:")
                        .font(.headline)
                        .foregroundColor(Color(hex: "31E1F7"))
                        .padding(.top, 10)

                    Text("Collect falling power-ups for special abilities like multi-ball, lasers, and shields.")
                }
                .foregroundColor(.white)
                .padding(.horizontal, 40)
                .padding(.top, 20)

                Spacer()

                Button("Back to Menu") {
                    gameViewModel.exitToMenu()
                }
                .font(.title)
                .foregroundColor(Color(hex: "31E1F7"))
                .padding()
            }
        }
    }
}

// Color extension
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