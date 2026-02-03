//
//  HowToView.swift
//  BreakoutPlus
//
//  How-to-play screen - placeholder for now
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
    HowToView()
        .environmentObject(GameViewModel())
}