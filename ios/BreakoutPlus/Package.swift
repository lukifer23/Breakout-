// swift-tools-version:5.7
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "BreakoutPlus",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .executable(
            name: "BreakoutPlus",
            targets: ["BreakoutPlus"])
    ],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "BreakoutPlus",
            dependencies: [],
            path: "BreakoutPlus",
            resources: [
                .copy("Info.plist")
            ],
            linkerSettings: [
                .linkedFramework("UIKit", .when(platforms: [.iOS])),
                .linkedFramework("SwiftUI", .when(platforms: [.iOS])),
                .linkedFramework("SpriteKit", .when(platforms: [.iOS]))
            ]
        )
    ]
)