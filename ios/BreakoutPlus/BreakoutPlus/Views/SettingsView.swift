//
//  SettingsView.swift
//  BreakoutPlus
//
//  Settings screen
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    // @ObservedObject private var scoreboard = ScoreboardStore.shared // TODO: Implement

    @AppStorage("soundEnabled") private var soundEnabled = true
    @AppStorage("musicEnabled") private var musicEnabled = true
    @AppStorage("vibrationEnabled") private var vibrationEnabled = true
    @AppStorage("tipsEnabled") private var tipsEnabled = true

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    Text("Settings")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Button("Menu") {
                        gameViewModel.exitToMenu()
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "31E1F7"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(hex: "1A1F26"))
                    .cornerRadius(10)
                }

                VStack(spacing: 12) {
                    ToggleRow(title: "Sound Effects", subtitle: "Bounce, brick, powerup cues", isOn: $soundEnabled)
                    ToggleRow(title: "Music", subtitle: "Background loop", isOn: $musicEnabled)
                    ToggleRow(title: "Vibration", subtitle: "Haptics on major impacts", isOn: $vibrationEnabled)
                    ToggleRow(title: "Tips", subtitle: "Show quick in-game hints", isOn: $tipsEnabled)
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Data")
                        .foregroundColor(.white.opacity(0.9))
                        .font(.system(size: 14, weight: .semibold))

                    Button("Reset Scoreboard") {
                        // scoreboard.reset() // TODO: Implement
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .background(Color(hex: "2A323D"))
                    .cornerRadius(12)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            .padding(.bottom, 24)
        }
    }
}

private struct ToggleRow: View {
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
                Text(subtitle)
                    .foregroundColor(.white.opacity(0.6))
                    .font(.system(size: 13))
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(Color(hex: "31E1F7"))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(hex: "1A1F26"))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color(hex: "31E1F7").opacity(0.16), lineWidth: 1)
        )
        .cornerRadius(14)
    }
}

#Preview {
    SettingsView()
        .environmentObject(GameViewModel())
}
