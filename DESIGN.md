# Breakout+ Design & UX

## Visual Direction
- High-contrast neon palette against deep navy backgrounds.
- Chunky button styles for touch accuracy on large screens.
- HUD chips for readable in-game stats.
- Typography: serif title treatment with sans-serif UI labels for a sharper, premium feel.

## Screens
- **Splash**: branded intro.
- **Title/Main**: quick access to Play, Modes, Scoreboard, Settings, How-To.
- **Mode Select**: cards describing each mode with start button.
- **Settings**: toggles + sensitivity slider + score reset.
- **Scoreboard**: ranked list of best scores.
- **How-To**: concise rules, powerups, brick types, modes.
- **Game**: full-screen OpenGL with overlay HUD and pause/end states.

## Foldable UX
- `sw600dp` layouts reflow content into two-column layout for the main menu.
- Hinge-aware padding avoids UI elements being placed under the hinge.

## Interaction Notes
- One-handed use supported with left-handed toggle (pause button shifts left).
- HUD tip bubble used for quick contextual guidance.
- Screen entry animations: titles and lists ease in to reduce visual abruptness.
