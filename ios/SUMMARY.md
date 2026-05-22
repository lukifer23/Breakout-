# Breakout+ iOS Summary

## Current State (2026-05-22)

iOS is actively maintained for gameplay parity with Android. The app builds and runs via simulator CLI workflows, and the iOS codebase includes all 10 Android-standard game modes and 18 powerups.

**Android 1.0.11** is the authoritative gameplay reference. Full cross-platform gaps are tracked in [`Docs/PARITY.md`](../Docs/PARITY.md).

`BreakoutPlusMac` is **frozen** (dev-only, 5-mode stale fork) — not a release target.

## Implemented Coverage

- **Modes (10)**: Classic, Timed Challenge, Endless, God Mode, Level Rush, Volley, Tunnel Siege, Survival, Invaders, Zen Mode.
- **Brick types (10)**: Normal, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss, Invader.
- **Powerups (18)**: Multi-ball, Laser, Guardrail, Shield, Extra Life, Wide Paddle, Shrink, Slow Motion, Overdrive, Fireball, Magnet, Gravity Well, Ball Splitter, Freeze, Pierce, Ricochet, Time Warp, 2x Score.
- **Systems**: Combo scoring, progression, local scoreboard, daily challenge scaffolding, privacy/settings surfaces, audio/haptics hooks.

## Known Parity Gaps (vs Android 1.0.11)

See [`Docs/PARITY.md`](../Docs/PARITY.md) for the full matrix. Highest-impact engine gaps:

- `TunnelModeSystem` — no supply readiness / pity-drop runtime
- `VolleyModeSystem` — turn stall detection and nudge not ported
- Spatial hash + ball sub-stepping collision
- `LevelAdvancePolicy` — GOD/ZEN flow differences
- Daily challenge rewards not applied (`print()` stub in store)
- Magnet ball catch not implemented
- Mode-specific HUD status row (`ModeStatusText`)

## Recent Parity Work

- Added iOS **Zen Mode** support end-to-end.
- Added missing iOS powerups: **Ricochet**, **Time Warp**, **2x Score**.
- Hardened iOS **Volley** logic (5 balls, bottom-out anchor, turn resolution order).
- Tunnel Siege layout routing (fortress ring + gate); Invaders formation isolation fix.
- HUD clears paddle lane; Zen hides score/life pressure.

## Remaining Focus (Post Android Play Sign-Off)

- Port Android mode subsystems per `PARITY.md` backlog (no stubs).
- Per-mode balancing on physical devices (Volley/Tunnel pacing).
- Visual effects/animation coherence pass.
- Design token file matching Android `colors.xml` / `ModeAccent`.

## Build Verification Path

```bash
./ios/run_ios_sim.sh --simulator "iPhone 17 Pro"
```

Manual build:
```bash
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' build
```

## Documentation

- [`ios/README.md`](README.md) — build/run and regression pass
- [`ios/ARCHITECTURE.md`](ARCHITECTURE.md) — SpriteKit port architecture
- [`ios/ROADMAP.md`](ROADMAP.md) — iOS polish priorities
- [`Docs/PARITY.md`](../Docs/PARITY.md) — authoritative gap matrix
