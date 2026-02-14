# Breakout+ iOS Summary

## Current State

iOS is actively maintained for gameplay parity with Android. The app builds and runs via simulator CLI workflows, and the iOS codebase now includes all Android-standard game modes and powerup classes.

## Implemented Coverage

- **Modes (10)**: Classic, Timed Challenge, Endless, God Mode, Level Rush, Volley, Tunnel Siege, Survival, Invaders, Zen Mode.
- **Brick types (10)**: Normal, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss, Invader.
- **Powerups (18)**: Multi-ball, Laser, Guardrail, Shield, Extra Life, Wide Paddle, Shrink, Slow Motion, Overdrive, Fireball, Magnet, Gravity Well, Ball Splitter, Freeze, Pierce, Ricochet, Time Warp, 2x Score.
- **Systems**: Combo scoring, progression, local scoreboard, daily challenge scaffolding, privacy/settings surfaces, audio/haptics hooks.

## Recent Parity Work

- Added iOS **Zen Mode** support end-to-end.
- Added missing iOS powerups: **Ricochet**, **Time Warp**, **2x Score**.
- Hardened iOS **Volley** logic:
  - Starts at 5 balls.
  - Bottom-out returns in turn flow (no unintended life consumption).
  - Improved turn resolution and row-spawn pressure behavior.
- Updated HUD behavior for Zen’s low-pressure presentation.
- Replaced hardcoded scoreboard player name with persisted settings-backed value.

## Remaining Focus

- Per-mode balancing on physical devices (especially Volley/Tunnel pacing).
- Visual effects/animation coherence pass after broader playtesting.
- Regression validation across all modes after tuning changes.

## Build Verification Path

```bash
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' build
```

Use `./ios/run_ios_sim.sh` for streamlined simulator build/install/launch.
