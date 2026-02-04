//
//  BreakoutPlusApp.swift
//  BreakoutPlus
//
//  Created by Breakout+ Team
//  Port from Android version v1.0.0
//

import SwiftUI

@main
struct BreakoutPlusApp: App {
    @StateObject private var gameViewModel = GameViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(gameViewModel)
                .statusBar(hidden: true)
                .persistentSystemOverlays(.hidden)
        }
    }
}