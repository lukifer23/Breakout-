// swift-tools-version:5.7
// iOS-specific package for BreakoutPlus

import PackageDescription

let package = Package(
    name: "BreakoutPlus-iOS",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .executable(
            name: "BreakoutPlus-iOS",
            targets: ["BreakoutPlus-iOS"])
    ],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "BreakoutPlus-iOS",
            dependencies: [],
            path: "BreakoutPlus",
            exclude: [
                "BreakoutPlus.xcodeproj"
            ],
            resources: [
                .copy("BreakoutPlus/Resources/Audio"),
                .copy("BreakoutPlus/Resources/Assets.xcassets")
            ],
            cSettings: [
                .headerSearchPath("BreakoutPlus")
            ]
        )
    ]
)