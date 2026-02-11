//
//  SettingsView.swift
//  BreakoutPlus
//
//  Settings screen
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @ObservedObject private var scoreboard = ScoreboardStore.shared

    @AppStorage("soundEnabled") private var soundEnabled = true
    @AppStorage("musicEnabled") private var musicEnabled = true
    @AppStorage("vibrationEnabled") private var vibrationEnabled = true
    @AppStorage("tipsEnabled") private var tipsEnabled = true
    @AppStorage("leftHanded") private var leftHanded = false
    @AppStorage("sensitivity") private var sensitivity: Double = 0.7
    @AppStorage("masterVolume") private var masterVolume: Double = 1.0
    @AppStorage("effectsVolume") private var effectsVolume: Double = 0.8
    @AppStorage("musicVolume") private var musicVolume: Double = 0.6

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

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Audio")
                                .foregroundColor(.white.opacity(0.9))
                                .font(.system(size: 14, weight: .semibold))

                            ToggleRow(title: "Sound Effects", subtitle: "Bounce, brick, powerup cues", isOn: $soundEnabled)
                            ToggleRow(title: "Music", subtitle: "Background loop", isOn: $musicEnabled)

                            SliderRow(title: "Master", value: $masterVolume)
                            SliderRow(title: "Effects", value: $effectsVolume)
                            SliderRow(title: "Music", value: $musicVolume)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Controls")
                                .foregroundColor(.white.opacity(0.9))
                                .font(.system(size: 14, weight: .semibold))

                            ToggleRow(title: "Left Handed", subtitle: "Swap pause and fire button positions", isOn: $leftHanded)
                            ToggleRow(title: "Tips", subtitle: "Show quick in-game hints", isOn: $tipsEnabled)

                            SliderRow(title: "Sensitivity", value: $sensitivity)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Accessibility")
                                .foregroundColor(.white.opacity(0.9))
                                .font(.system(size: 14, weight: .semibold))

                            ToggleRow(title: "Vibration", subtitle: "Haptics on major impacts", isOn: $vibrationEnabled)

                            Button("Privacy Policy") {
                                gameViewModel.navigateToPrivacy()
                            }
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color(hex: "31E1F7"))
                            .padding(.horizontal, 18)
                            .padding(.vertical, 12)
                            .background(Color(hex: "1A1F26"))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(hex: "31E1F7").opacity(0.25), lineWidth: 1)
                            )
                            .cornerRadius(12)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(alignment: .leading, spacing: 10) {
                            Text("Advanced")
                                .foregroundColor(.white.opacity(0.9))
                                .font(.system(size: 14, weight: .semibold))

                            Button("Reset Scoreboard") {
                                scoreboard.reset()
                            }
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 18)
                            .padding(.vertical, 12)
                            .background(Color(hex: "2A323D"))
                            .cornerRadius(12)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.top, 4)
                    .padding(.bottom, 16)
                }
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

private struct SliderRow: View {
    let title: String
    @Binding var value: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
                Spacer()
                Text("\(Int((value * 100).rounded()))%")
                    .foregroundColor(.white.opacity(0.75))
                    .font(.system(size: 13, weight: .semibold))
            }
            Slider(value: $value, in: 0...1, step: 0.01)
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
