// swift-tools-version:5.7
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "BreakoutPlus",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "BreakoutPlus",
            targets: ["BreakoutPlus"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "BreakoutPlus",
            dependencies: [],
            path: "BreakoutPlus/BreakoutPlus",
            resources: []
        )
    ]
)