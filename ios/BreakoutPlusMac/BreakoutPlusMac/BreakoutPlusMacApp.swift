//
//  BreakoutPlusMacApp.swift
//  BreakoutPlusMac
//
//  macOS version of Breakout+ using SwiftUI
//  Mouse controls instead of touch
//

import SwiftUI

@main
struct BreakoutPlusMacApp: App {
    @StateObject private var gameViewModel = GameViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(gameViewModel)
                .frame(minWidth: 800, minHeight: 600)
        }
        .windowResizability(.contentSize)
        .defaultSize(width: 800, height: 600)
    }
}