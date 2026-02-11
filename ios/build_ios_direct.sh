#!/bin/bash

# Direct iOS build script bypassing Xcode project issues
# Builds BreakoutPlus iOS app using Swift compiler directly

set -e

echo "🔨 Building BreakoutPlus iOS App (Direct Compilation)"
echo "=================================================="

PROJECT_DIR="/Users/admin/Downloads/VSCode/Android Game/ios/BreakoutPlus"
BUILD_DIR="$PROJECT_DIR/build"
IOS_SDK="/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk"

echo "📁 Build directory: $BUILD_DIR"
echo "📱 iOS SDK: $IOS_SDK"
echo ""

# Create build directory
mkdir -p "$BUILD_DIR"

# Collect all Swift files
SWIFT_FILES=(
    "$PROJECT_DIR/BreakoutPlus/BreakoutPlusApp.swift"
    "$PROJECT_DIR/BreakoutPlus/ContentView.swift"
    "$PROJECT_DIR/BreakoutPlus/ViewModels/GameViewModel.swift"
    "$PROJECT_DIR/BreakoutPlus/Models/GameMode.swift"
    "$PROJECT_DIR/BreakoutPlus/Models/BrickType.swift"
    "$PROJECT_DIR/BreakoutPlus/Models/PowerUpType.swift"
    "$PROJECT_DIR/BreakoutPlus/Models/DailyChallenge.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/GameEngine.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/Ball.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/Brick.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/Paddle.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/PowerUp.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/LevelTheme.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/Beam.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/Models/EnemyShot.swift"
    "$PROJECT_DIR/BreakoutPlus/Core/LevelFactory.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/ScoreboardStore.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/AudioManager.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/Haptics.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/DailyChallengeStore.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/ProgressionStore.swift"
    "$PROJECT_DIR/BreakoutPlus/Services/LifetimeStatsStore.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/SplashView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/MenuView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/GameView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/SettingsView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/ScoreboardView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/HowToView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/PrivacyView.swift"
    "$PROJECT_DIR/BreakoutPlus/Views/DailyChallengesView.swift"
)

echo "📄 Found ${#SWIFT_FILES[@]} Swift files"

# Build command
BUILD_CMD="swiftc"
BUILD_CMD="$BUILD_CMD -sdk $IOS_SDK"
BUILD_CMD="$BUILD_CMD -target arm64-apple-ios16.0"
BUILD_CMD="$BUILD_CMD -o $BUILD_DIR/BreakoutPlus"

# Add framework paths
BUILD_CMD="$BUILD_CMD -F /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/Library/Frameworks"
BUILD_CMD="$BUILD_CMD -F /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks"

# Add all Swift files
for file in "${SWIFT_FILES[@]}"; do
    BUILD_CMD="$BUILD_CMD \"$file\""
done

echo "🚀 Building..."
echo "$BUILD_CMD"
echo ""

eval "$BUILD_CMD"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS! BreakoutPlus iOS app built successfully!"
    echo "📦 Output: $BUILD_DIR/BreakoutPlus"
    echo ""
    echo "📱 To install on device/simulator:"
    echo "   1. Boot simulator: xcrun simctl boot <device-id>"
    echo "   2. Install: xcrun simctl install booted $BUILD_DIR/BreakoutPlus"
    echo "   3. Launch: xcrun simctl launch booted com.breakoutplus.ios"
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi