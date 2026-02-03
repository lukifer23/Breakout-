//
//  SettingsView.swift
//  BreakoutPlus
//
//  Settings screen - placeholder for now
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack {
                Text("Settings")
                    .font(.largeTitle)
                    .foregroundColor(.white)

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

#Preview {
    SettingsView()
        .environmentObject(GameViewModel())
}