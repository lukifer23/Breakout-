# Breakout+ Platform Parity Matrix

Android is the authoritative gameplay implementation. iOS is an active parity port. **BreakoutPlusMac is frozen** — dev-only, not a release target.

Last updated: 2026-05-22 (hardening initiative)

## Release Targets

| Platform | Status | Notes |
|----------|--------|-------|
| Android | **Primary / shipping** | OpenGL ES 2.0, fold-aware UI, full feature set |
| iOS | **Active parity port** | SwiftUI + SpriteKit; engine gaps remain |
| macOS (`BreakoutPlusMac`) | **Frozen / dev-only** | 5-mode stale fork; no maintenance |

## Game Modes (10)

| Mode | Android | iOS | Mac |
|------|---------|-----|-----|
| Classic | Full | Full | Full |
| Timed Challenge | Full | Full | Full |
| Endless | Full | Full | Full |
| God Mode | Full | Full | Full |
| Level Rush | Full (55s/level) | Full (55s) | Partial (45s) |
| Volley | Full (`VolleyModeSystem`) | Partial (no stall system) | Missing |
| Tunnel Siege | Full (`TunnelModeSystem`) | Layout only (no supply/pity) | Missing |
| Survival | Full | Full | Missing |
| Invaders | Full | Partial (shield model differs) | Missing |
| Zen | Full (silent auto-advance) | Partial (level-complete overlay) | Missing |

## Engine Subsystems

| Subsystem | Android | iOS | Mac |
|-----------|---------|-----|-----|
| Fixed-step simulation | Yes (`GameRenderer`) | No (60 Hz clamp) | No |
| Spatial-hash collision | Yes | No (O(n²)) | No |
| Ball sub-stepping | Yes | No | No |
| `VolleyModeSystem` | Yes | Inline logic | N/A |
| `TunnelModeSystem` | Yes | Missing | N/A |
| `LevelAdvancePolicy` | Yes | Missing | N/A |
| `ModeStatusText` HUD | Yes | Generic chips | N/A |
| Magnet ball catch | Yes | Missing | N/A |
| `GameCollisionSystem` | Yes | N/A | N/A |

## Daily Challenges

| Feature | Android | iOS | Mac |
|---------|---------|-----|-----|
| Challenge templates | 13 (3 random/day) | 5 fixed | N/A |
| Progress tracking | Full | Partial | N/A |
| Reward application | Wired in gameplay | `print()` only | N/A |

## UI / UX

| Feature | Android | iOS | Mac |
|---------|---------|-----|-----|
| Main menu + mode select | Separate screens | Combined | Combined |
| Scoreboard "All modes" | Yes | Per-mode tabs only | Per-mode only |
| High-score name dialog | End-of-run | Settings field | N/A |
| Dark mode toggle | Yes | No | No |
| FPS counter | Yes | No | No |
| High refresh rate toggle | Yes | No | N/A |
| Foldable / hinge layout | Yes | No | N/A |
| Design tokens | `colors.xml`, `UiMotion` | Inline hex per view | Inline |
| Entry stagger animations | All list screens | Splash only | Minimal |
| Privacy screen | Yes | Yes | Missing |
| Daily Challenges screen | Yes | Yes | Missing |

## Mode Accent Colors (Canonical — Android)

| Mode | Color token | Hex |
|------|-------------|-----|
| Classic | `bp_cyan` | #58E2FF |
| Timed Challenge | `bp_gold` | #F6C45A |
| Endless | `bp_green` | #48D894 |
| God Mode | `bp_magenta` | #FF6EA3 |
| Level Rush | `bp_red` | #FF6D61 |
| Volley | `bp_azure` | #5EA8FF |
| Tunnel Siege | `bp_orange` | #FFA453 |
| Survival | `bp_flame` | #FF8A3D |
| Invaders | `bp_violet` | #8B8EFF |
| Zen Mode | `bp_gray` | #A6B3C9 |

## iOS Parity Backlog (Post Android Sign-Off)

Execute only after Android hardening is signed off. No stubs — full ports required.

1. Port `TunnelModeSystem` (supply readiness, pity drops, gate telemetry)
2. Port `VolleyModeSystem` (stall detection, nudge, turn resolution)
3. Port spatial hash + ball sub-stepping collision
4. Port `LevelAdvancePolicy` for GOD/ZEN flow
5. Port `ModeStatusText` mode-specific HUD row
6. Port magnet ball catch from `GameEngineCollision`
7. Wire daily challenge rewards (replace `print()` in `DailyChallengeStore`)
8. Unify design tokens; fix Settings `@AppStorage` duplication
9. Add missing settings: dark mode, FPS, high refresh
10. Fix navigation: contextual back vs always-to-menu

## Mac Target Policy

**BreakoutPlusMac is frozen.** Do not extend or sync it with Android/iOS during hardening. It exists as an optional local dev CLI target only. See [ios/README.md](../ios/README.md).
