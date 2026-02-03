// swift-tools-version:5.7

import PackageDescription

let package = Package(
    name: "BreakoutPlusCLI",
    platforms: [
        .macOS(.v12)
    ],
    products: [
        .executable(
            name: "BreakoutPlusCLI",
            targets: ["BreakoutPlusCLI"])
    ],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "BreakoutPlusCLI",
            dependencies: [],
            path: "BreakoutPlusMac",
            sources: ["main-cli.swift"],
            resources: []
        )
    ]
)