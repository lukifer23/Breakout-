//
//  PrivacyView.swift
//  BreakoutPlus
//
//  Privacy Policy screen
//

import SwiftUI

struct PrivacyView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    Text("Privacy Policy")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Button("Back") {
                        gameViewModel.exitToMenu()
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "31E1F7"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(hex: "1A1F26"))
                    .cornerRadius(10)
                }

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Breakout+ Privacy Policy")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Effective date: February 5, 2026")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white.opacity(0.8))

                        Text("Breakout+ is an offline game that stores your settings and scores locally on your device. The app does not require an account, does not include third-party analytics, and does not transmit gameplay data to external servers.")
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(.white.opacity(0.9))
                            .lineSpacing(4)

                        Text("Data stored on device")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.top, 8)

                        VStack(alignment: .leading, spacing: 8) {
                            BulletPoint(text: "Settings (sound, music, vibration, sensitivity, left-handed mode)")
                            BulletPoint(text: "Scoreboard entries (score, mode, level, duration)")
                            BulletPoint(text: "Optional gameplay logs (only if \"Enable Game Logging\" is turned on)")
                        }

                        Text("Logging")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.top, 8)

                        Text("If game logging is enabled, the app writes gameplay events to files stored locally on your device. These logs are not uploaded or shared.")
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(.white.opacity(0.9))
                            .lineSpacing(4)

                        Text("Data sharing")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.top, 8)

                        Text("Breakout+ does not sell, share, or transmit your data to third parties.")
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(.white.opacity(0.9))
                            .lineSpacing(4)

                        Text("Changes")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.top, 8)

                        Text("If this policy changes, the update will be reflected in the app and in the App Store listing.")
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(.white.opacity(0.9))
                            .lineSpacing(4)
                    }
                    .padding(.vertical, 8)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            .padding(.bottom, 24)
        }
    }
}

private struct BulletPoint: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("•")
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(Color(hex: "31E1F7"))
            Text(text)
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(.white.opacity(0.9))
                .lineSpacing(4)
        }
    }
}

#Preview {
    PrivacyView()
        .environmentObject(GameViewModel())
}