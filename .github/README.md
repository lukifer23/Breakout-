# Breakout+

A premium brickbreaker game designed specifically for foldable devices like the Samsung Galaxy Z Fold 7. Features advanced physics, multiple game modes, dynamic brick behaviors, and comprehensive powerup systems with GPU-accelerated rendering at 60+ FPS.

## Key Features
- **Foldable-First Design**: Optimized for Samsung Galaxy Z Fold 7 (folded 7.6" and unfolded 12.4" displays)
- **GPU Acceleration**: OpenGL ES 2.0 rendering for smooth gameplay
- **Advanced Physics**: Accurate collision detection with momentum preservation
- **Multiple Game Modes**: Classic, Timed Challenge, Endless, God Mode, and Level Rush
- **Dynamic Brick System**: 9 brick types with unique behaviors (moving, spawning, phase, boss)
- **Comprehensive Powerups**: 13 distinct powerups with visual effects and timers
- **Combo System**: Score multipliers for consecutive brick destruction
- **6 Visual Themes**: Unique color palettes and animated backgrounds
- **Procedural Audio**: Algorithmic sound generation with individual volume controls
- **Data Logging**: Built-in analytics for debugging

## Quick Start
1. Clone the repository
2. Enable USB debugging on your Android device
3. Connect device via USB
4. Build: `./gradlew assembleRelease`
5. Install: `adb install -r app/build/outputs/apk/release/app-release.apk`
6. Launch: `adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity`

## Project Status
Testing ready v1.0.4 - Complete implementation with all core features

## License
MIT License - see [LICENSE](LICENSE) for details

## Architecture
- **Language**: Kotlin
- **Graphics**: OpenGL ES 2.0
- **Build System**: Gradle
- **Target SDK**: API 35
- **Min SDK**: API 24
