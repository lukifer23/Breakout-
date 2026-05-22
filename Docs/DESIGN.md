# Breakout+ Design & UX

## Visual Direction
- High-contrast neon-forward arcade styling.
- Theme-driven palettes and animated backgrounds.
- Crisp, readable HUD chips and labels across device classes.

## Core Screens
- Main / title
- Mode select
- Gameplay (OpenGL surface + overlay HUD)
- Settings
- Scoreboard
- How-To
- Daily Challenges
- Privacy policy

## HUD Principles
- Keep score/lives/time/level/meta stable during gameplay.
- Reserve top HUD height dynamically for different aspect ratios.
- Scale typography/chips/buttons with responsive `hudScale` behavior.
- Avoid intrusive tip overlays in active gameplay space.

## Motion & Feedback
- Consistent overlay and banner timings via `UiMotion` constants.
- Gameplay FX includes particles, flashes, shield pulses, and controlled screen shake.
- Animation oscillators are frame-time coherent to reduce visual drift.

## Mode Accent Colors (Canonical)

| Mode | Token | Hex |
|------|-------|-----|
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

Use `ModeAccent.colorRes(mode)` in Android UI code. Cross-platform parity matrix: `Docs/PARITY.md`.

## Foldable / Large Screen
- `sw600dp` and `sw720dp` layout variants for larger displays.
- `FoldAwareActivity` applies hinge/inset-safe padding.
- Handedness toggle keeps high-priority controls reachable.
