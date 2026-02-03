//
//  ScoreboardView.swift
//  BreakoutPlus
//
//  Scoreboard screen - placeholder for now
//

import SwiftUI

struct ScoreboardView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack {
                Text("Scoreboard")
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
    ScoreboardView()
        .environmentObject(GameViewModel())
}