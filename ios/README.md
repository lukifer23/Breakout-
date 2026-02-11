# Breakout+ iOS

## Status

iOS parity work is active and playable with CLI-only simulator workflows.

Verified in this repo:
- `xcodebuild ... build` succeeds for simulator.
- `./ios/run_ios_sim.sh --simulator "iPhone 17 Pro"` builds, installs, and launches.

This is not documented as "finished parity"; balancing and visual polish are still in progress.

## Current Feature Coverage

- **Game modes (9)**: Classic, Timed Challenge, Endless, God Mode, Level Rush, Volley, Tunnel Siege, Survival, Invaders.
- **Brick types (10)**: Normal, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss, Invader.
- **Powerups (15)**: Multi-ball, Laser, Guardrail, Shield, Extra Life, Wide Paddle, Shrink, Slow Motion, Overdrive, Fireball, Magnet, Gravity Well, Ball Splitter, Freeze, Pierce.
- **Themes**: Neon, Sunset, Cobalt, Aurora, Forest, Lava, Invaders (mode-routed rotation).
- **Core systems**: progression, combo scoring, scoreboard (mode-aware + lifetime stats), settings, privacy policy, daily challenges scaffold.

## Recent iOS Work

- Added **Tunnel Siege** mode support in iOS mode model, menu, and how-to content.
- Reworked iOS `LevelFactory` routing so generation is mode-aware:
  - Dedicated Tunnel fortress layout (unbreakable ring + narrow gate + interior core).
  - Dedicated Invaders path.
  - Mode-themed level rotation.
- Fixed Invaders formation update to move only invader bricks (prevents movement/audio spam side effects).
- Updated speed/difficulty curves to include Tunnel mode.
- HUD interaction and layout improvements:
  - HUD stays clear of paddle area and does not intercept gameplay touches.
  - Powerup drop visuals reduced to lower visual clutter.
- Aiming and launch guide smoothing improvements are wired in engine + scene.

## Known Gaps / Next Polish Targets

- Continue visual polish pass on collision FX and brick destruction detail in all modes.
- Continue per-mode balance tuning (ball speed, density, pacing), especially Volley and Tunnel progression pacing.
- Validate scrolling/gesture behavior on both simulator and real device; simulator input can differ from touch hardware.
- Maintain Android/iOS behavior alignment when changing mode logic.

## Build and Run (CLI Only)

From repo root:

```bash
./ios/run_ios_sim.sh --simulator "iPhone 17 Pro"
```

Manual build:

```bash
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' build
```

Notes:
- Use `xcrun simctl list devices` to choose an installed simulator.
- Physical-device deployment still requires valid Apple signing/provisioning.

## Recommended Manual Regression Pass

Run this after major gameplay changes:

1. Enter each mode and confirm mode identity (layout/rules are distinct).
2. Tunnel Siege: verify fortress ring + narrow gate are present every start/restart.
3. Invaders: verify formation moves smoothly and does not trigger continuous jitter SFX.
4. Volley: verify row movement direction and no unintended unbreakable bricks.
5. Aim guide: drag across full paddle span and verify smooth continuous guide angle.
6. HUD: verify no UI element blocks paddle control.
7. Powerups: verify drop size/readability and collection behavior.
8. Level progression: verify `NEXT LEVEL` always advances level, never mode-select fallback.
