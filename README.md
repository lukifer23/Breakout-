# Breakout+

Android-first brick breaker for phones, foldables, and slates.  
The repository also includes an iOS port (`ios/`), but Android is the active release track.

## Current Project State (As Of 2026-05-22)

- Android **1.0.12** excellence pass — glass UI unified, Zen differentiated, performance hardened
- **CI**: GitHub Actions runs unit tests, lint, debug assembly, and release assembly (CI debug signing)
- **108+ JVM unit tests** passing; lint + assembleDebug + assembleRelease green (JDK 17 required locally)
- Android feature-complete: 10 modes, 10 brick types, 18 powerups, progression, unlocks, scoreboards, daily challenges, lifetime stats
- Extracted systems: `VolleyModeSystem`, `TunnelModeSystem`, `InvadersModeSystem`, `LevelAdvancePolicy`, `ModeAccent`, `GameHudController`
- iOS parity backlog documented in [`Docs/PARITY.md`](Docs/PARITY.md) — execute after Android Play release sign-off
- `BreakoutPlusMac` frozen (dev-only, unmaintained)

### Remaining Before Play Store Ship
- Device QA matrix (phone, fold, slate) — see [`Docs/TESTING.md`](Docs/TESTING.md)
- Play Console setup, release signing, tablet screenshots — see [`Docs/RELEASE_CHECKLIST.md`](Docs/RELEASE_CHECKLIST.md)
- Long-session device validation for Volley/Tunnel under heavy FX

### Recent 1.0.11 Highlights
- Locked Volley starting ball count at 5 (`VolleyModeSystem.STARTING_BALL_COUNT`)
- Unified mode accent colors; Survival uses distinct `bp_flame` vs Tunnel orange
- Extracted `InvadersModeSystem`; expanded Volley/Tunnel regression tests
- Added cross-platform parity matrix and Android hardening sign-off docs

Latest automated validation:
```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
CI=true ./gradlew :app:assembleRelease
```

## Active Engineering Goals
1. Preserve all existing features and mode identities.
2. Eliminate regressions while patching gameplay, visuals, and UX inconsistencies.
3. Improve stability/performance under heavy gameplay (multi-ball, dense boards, high FX).
4. Fix form-factor variance (folded/unfolded/slate) in HUD scaling and board density.
5. Reduce complexity in large runtime files by extracting focused systems.

## Execution Constraints
- No feature removals.
- No placeholders, no stubbed behavior, no mock gameplay paths.
- Ship real patches that compile, run, and pass validation.

## Android Runtime Snapshot
- OpenGL ES 2.0 renderer (`GLSurfaceView` + `Renderer2D`)
- Choreographer frame pacing (`RENDERMODE_WHEN_DIRTY`)
- Fixed-step-only simulation in `GameRenderer`
- Core gameplay state machine in `GameEngine`
- Extracted mode systems (`VolleyModeSystem`, `TunnelModeSystem`, `InvadersModeSystem`, `ModeLayoutPolicy`, `ModeBoardMetrics`, `LevelAdvancePolicy`, `ModeAccent`) to reduce monolith risk
- Fold-aware and large-screen responsive HUD strategy managed by `GameHudController`

## Game Content
### Modes (10)
- `CLASSIC`, `TIMED`, `ENDLESS`, `GOD`, `RUSH`, `VOLLEY`, `TUNNEL`, `SURVIVAL`, `INVADERS`, `ZEN`

### Brick Types (10)
- `NORMAL`, `REINFORCED`, `ARMORED`, `EXPLOSIVE`, `UNBREAKABLE`, `MOVING`, `SPAWNING`, `PHASE`, `BOSS`, `INVADER`

### Powerups (18)
- `MULTI_BALL`, `LASER`, `GUARDRAIL`, `SHIELD`, `LIFE`, `WIDE_PADDLE`, `SHRINK`, `SLOW`, `OVERDRIVE`, `FIREBALL`, `MAGNET`, `GRAVITY_WELL`, `BALL_SPLITTER`, `FREEZE`, `PIERCE`, `RICOCHET`, `TIME_WARP`, `DOUBLE_SCORE`

## Build & Validation (Android CLI)
### Prereqs
- JDK 17
- Android SDK platform 35
- `adb` on `PATH`

### Build Debug
```bash
./gradlew :app:assembleDebug
```

### Install + Launch
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

### Validation (local)
```bash
export JAVA_HOME=/path/to/jdk-17   # required; JDK 26 breaks Gradle in this project
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

### Mode Smoke Test
```bash
tools/mode_smoke_test.sh
```

### GOD/ZEN Progression Probe
```bash
tools/god_zen_progression_probe.sh
```

### All-Modes Progression Probe
```bash
tools/all_modes_progression_probe.sh
```

## Repository Structure
```text
app/        Android app (active release track)
ios/        iOS port
Docs/       Product, architecture, build, testing, and roadmap docs
tools/      Dev utilities (mode smoke tests + progression probes)
```

## Documentation
- [`Docs/REQUIREMENTS.md`](Docs/REQUIREMENTS.md) — product and quality requirements
- [`Docs/ARCHITECTURE.md`](Docs/ARCHITECTURE.md) — Android runtime architecture
- [`Docs/GAMEPLAY.md`](Docs/GAMEPLAY.md) — mode rules, bricks, powerups
- [`Docs/DESIGN.md`](Docs/DESIGN.md) — visual/HUD UX principles and mode accent tokens
- [`Docs/BUILD.md`](Docs/BUILD.md) — build, install, release, Fastlane
- [`Docs/TESTING.md`](Docs/TESTING.md) — unit tests, device probes, manual QA matrix
- [`Docs/ROADMAP.md`](Docs/ROADMAP.md) — active engineering workstreams
- [`Docs/PARITY.md`](Docs/PARITY.md) — Android vs iOS vs Mac parity matrix
- [`Docs/HARDENING_SIGNOFF.md`](Docs/HARDENING_SIGNOFF.md) — Android 1.0.11 hardening exit criteria
- [`Docs/RELEASE_CHECKLIST.md`](Docs/RELEASE_CHECKLIST.md) — Play Store release checklist
- [`Docs/RELEASE_NOTES.md`](Docs/RELEASE_NOTES.md) — version history
- [`ios/README.md`](ios/README.md) — iOS port status and build/run guide

## License
MIT (`LICENSE`)
