# Breakout+

Android-first brick breaker for phones, foldables, and slates. The repository also includes an iOS port (`ios/`); Android is the active release track.

## Key Features
- **Foldable-First Design**: Optimized for Samsung Galaxy Z Fold class devices (folded and unfolded displays)
- **GPU Acceleration**: OpenGL ES 2.0 rendering with Choreographer frame pacing
- **Fixed-Step Simulation**: Stable gameplay timing across 45–240 Hz displays
- **10 Game Modes**: Classic, Timed Challenge, Endless, God Mode, Level Rush, Volley, Tunnel Siege, Survival, Invaders, Zen
- **10 Brick Types**: Normal, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss, Invader
- **18 Powerups**: Full stackable powerup system with visual feedback
- **Progression Systems**: Unlocks, daily challenges, scoreboards, lifetime stats
- **Offline-Only**: No network, no analytics SDK

## Quick Start
1. Clone the repository
2. Install JDK 17 and Android SDK platform 35
3. Build: `./gradlew :app:assembleDebug`
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
5. Launch: `adb shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity`

## Validation
```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
tools/mode_smoke_test.sh
```

## Project Status
Active Android hardening track — see [README.md](../README.md) and [Docs/ROADMAP.md](../Docs/ROADMAP.md).

- **Version**: 1.0.11 (versionCode 11)
- **Min SDK**: 26
- **Target SDK**: 35

## CI
GitHub Actions runs unit tests, lint, and debug assembly on push/PR (`.github/workflows/android.yml`).

## License
MIT License — see [LICENSE](../LICENSE) for details.
