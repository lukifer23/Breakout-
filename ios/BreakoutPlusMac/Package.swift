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
            sources: [
                "main-cli.swift",
                "Models/GameMode.swift",
                "Models/BrickType.swift",
                "Models/PowerUpType.swift",
                "Core/GameEngine.swift",
                "Core/Models/Ball.swift",
                "Core/Models/Brick.swift",
                "Core/Models/Paddle.swift",
                "Core/Models/PowerUp.swift",
                "Core/Models/LevelTheme.swift"
            ],
            resources: []
        )
    ]
)