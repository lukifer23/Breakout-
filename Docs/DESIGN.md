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

## Foldable / Large Screen
- `sw600dp` and `sw720dp` layout variants for larger displays.
- `FoldAwareActivity` applies hinge/inset-safe padding.
- Handedness toggle keeps high-priority controls reachable.
