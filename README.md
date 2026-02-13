# Breakout+

Android-first brick breaker built for modern phones and foldables, with a separate iOS port in the same repository.

## Current Scope
- Primary release focus: Android (`app/`)
- iOS lives in `ios/` and remains a separate codepath
- Development workflow supports full CLI build/test/deploy

## Android Feature Set
- OpenGL ES 2.0 renderer (`GLSurfaceView` + custom `Renderer2D`)
- Choreographer-paced rendering with fixed-step simulation in `GameRenderer`
- 10 game modes
- 10 brick types
- 15 powerups
- Journey progression (chapter/stage labels every 10 levels)
- Daily challenges, per-mode scoreboard, lifetime stats
- Fold-aware and large-screen layout handling

## Game Modes (10)
- `CLASSIC`: standard progression, 3 lives
- `TIMED`: 2:30 score attack
- `ENDLESS`: infinite scaling levels
- `GOD`: endless practice with no life loss
- `RUSH`: 55s per level, 1 life
- `VOLLEY`: turn-based chain launch, descending rows
- `TUNNEL`: fortified ring + narrow gate lane
- `SURVIVAL`: one life, faster scaling
- `INVADERS`: moving fleet + enemy fire + shield system
- `ZEN`: no lives/score pressure, continuous flow

## Brick Types (10)
- Normal, Reinforced, Armored, Explosive, Unbreakable
- Moving, Spawning, Phase, Boss, Invader

## Powerups (15)
- Multi-ball, Laser, Guardrail, Shield, Extra life
- Wide paddle, Shrink, Slow, Overdrive, Fireball
- Magnet, Gravity Well, Ball Splitter, Freeze, Pierce

## Android Build & Run (CLI)

### Prereqs
- JDK 17
- Android SDK with platform 35
- `adb` on PATH
- Ruby + Bundler (only if using Fastlane upload lanes)

### Build debug APK
```bash
./gradlew :app:assembleDebug
```

### Install + launch on device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

### Build release artifacts
```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

## Validation Commands
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

## Automated Device Smoke Test
```bash
tools/mode_smoke_test.sh
```
Useful env vars:
- `BP_SERIAL`
- `BP_GAME_MODES`
- `BP_MODE_WAIT`
- `BP_AUTO_PLAY`
- `BP_AUTO_PLAY_SECONDS`

## Project Layout
```text
app/src/main/java/com/breakoutplus/
  MainActivity.kt
  ModeSelectActivity.kt
  GameActivity.kt
  SettingsActivity.kt
  ScoreboardActivity.kt
  DailyChallengesActivity.kt
  HowToActivity.kt
  PrivacyActivity.kt
  FoldAwareActivity.kt
  ...

app/src/main/java/com/breakoutplus/game/
  GameGLSurfaceView.kt
  GameRenderer.kt
  GameEngine.kt
  LevelFactory.kt
  GameMode.kt
  ModeBalance.kt
  Renderer2D.kt
  GameAudioManager.kt
  ...

Docs/
ios/
```

## Docs Index
- `Docs/REQUIREMENTS.md`
- `Docs/ARCHITECTURE.md`
- `Docs/DESIGN.md`
- `Docs/GAMEPLAY.md`
- `Docs/BUILD.md`
- `Docs/TESTING.md`
- `Docs/ASSETS.md`
- `Docs/ROADMAP.md`
- `Docs/DATA_SAFETY.md`
- `Docs/PRIVACY_POLICY.md`
- `Docs/RELEASE_CHECKLIST.md`
- `Docs/RELEASE_NOTES.md`
- `Docs/STORE_LISTING.md`

## Notes
- Debug build package: `com.breakoutplus.debug`
- Release package: `com.breakoutplus`
- Data is local-only (settings, scores, optional local logs)

## License
MIT (`LICENSE`)
