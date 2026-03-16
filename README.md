# Breakout+

Android-first brick breaker for phones, foldables, and slates.  
The repository also includes an iOS port (`ios/`), but Android is the active release track.

## Current Project State (As Of 2026-03-02)
- Android foundation is strong and feature-complete:
  - 10 game modes
  - 10 brick types
  - 18 powerups
  - progression, unlocks, scoreboards, daily challenges, lifetime stats
- Android still needs focused polish and structural cleanup for release confidence:
  - mode stability and balance (especially `VOLLEY` and `TUNNEL`)
  - consistent HUD/animation behavior across foldables and large slates
  - long-session performance hardening
  - complexity reduction in core runtime files without removing features
- Recent patch focus:
  - hardened GOD/ZEN progression and skip-level flow
  - restored tablet/slate GOD skip control parity
  - tightened slate/fold board-density consistency
  - unified Volley danger FX behavior across viewport classes
  - reduced Volley turn-stall edge cases from near-zero velocity jitter
  - added Tunnel supply readiness telemetry and anti-starvation pity-drop behavior
  - added persistent Tunnel gate-lane telegraph and stronger forced-supply visual feedback
  - unified gameplay event flash/shake/combo/clear feedback through centralized visual profiles
  - hardened PHASE brick transition logic and added regression test coverage

Latest validation snapshot:
- `./gradlew :app:testDebugUnitTest` passing
- Device smoke passing for `VOLLEY`, `TUNNEL`, `GOD`, `ZEN`, `INVADERS` on connected hardware

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
- Extracted mode systems (`VolleyModeSystem`, `TunnelModeSystem`, `ModeLayoutPolicy`, `ModeBoardMetrics`, `LevelAdvancePolicy`) to reduce monolith risk
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

### Validation
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
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
- `Docs/REQUIREMENTS.md`
- `Docs/ARCHITECTURE.md`
- `Docs/GAMEPLAY.md`
- `Docs/TESTING.md`
- `Docs/ROADMAP.md`
- `Docs/BUILD.md`
- `Docs/PLAY_RELEASE_GAPS_2026-02-14.md`

## License
MIT (`LICENSE`)
