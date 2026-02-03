//
//  SplashView.swift
//  BreakoutPlusMac
//
//  Splash screen for macOS version
//

import SwiftUI

struct SplashView: View {
    @State private var opacity = 0.0
    @State private var scale = 0.8

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 20) {
                Text("BREAKOUT")
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(Color(hex: "31E1F7"))
                    .opacity(opacity)
                    .scaleEffect(scale)

                Text("+")
                    .font(.system(size: 64, weight: .bold))
                    .foregroundColor(Color(hex: "FF4FD8"))
                    .opacity(opacity)
                    .scaleEffect(scale)
                    .offset(y: -10)

                Text("MAC EDITION")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Color(hex: "FFFFFF"))
                    .opacity(opacity * 0.7)
                    .offset(y: 10)

                Text("Mouse Controls - CLI Built")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "FFFFFF").opacity(0.5))
                    .padding(.top, 20)
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.0)) {
                opacity = 1.0
                scale = 1.0
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

#Preview {
    SplashView()
        .frame(width: 800, height: 600)
}