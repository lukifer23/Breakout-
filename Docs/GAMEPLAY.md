# Breakout+ Gameplay

## Core Loop
- Aim, launch, and keep balls in play.
- Clear all breakable bricks to complete a level.
- Collect powerups and combine effects.
- Track chapter/stage progression and XP gain.

## Mode Rules
- `Classic`: 3 lives, no timer, balanced progression.
- `Timed Challenge`: 2 lives, 150-second run timer.
- `Endless`: infinite progression with scaling layouts.
- `God Mode`: endless practice, no life loss.
- `Level Rush`: 1 life, 55-second level timer.
- `Volley`: turn-based chain launch; rows descend each turn, starts with 5 balls and can grow.
- `Tunnel Siege`: fortified unbreakable ring, narrow gate lane into dense interior core.
- `Survival`: 1 life with aggressive speed scaling.
- `Invaders`: moving invader fleet fires back; shield and telegraph systems are active.
- `Zen`: relaxed flow mode (no score/life pressure, continuous restarts/advances).

## Brick Types
- `NORMAL`: 1 HP.
- `REINFORCED`: 2 HP baseline.
- `ARMORED`: 3 HP baseline.
- `EXPLOSIVE`: damages nearby bricks.
- `UNBREAKABLE`: requires special handling (fireball/laser pressure).
- `MOVING`: horizontal motion over time.
- `SPAWNING`: can produce child bricks.
- `PHASE`: staged durability behavior.
- `BOSS`: high-HP special brick with stronger FX.
- `INVADER`: fleet unit used in Invaders mode.

## Powerups
- Multi-ball
- Laser
- Guardrail
- Shield
- Extra Life
- Wide Paddle
- Shrink
- Slow
- Overdrive
- Fireball
- Magnet
- Gravity Well
- Ball Splitter
- Freeze
- Pierce

## Scoring & Progression
- Brick score values scale with combo multipliers.
- Combo windows reward sustained hit streaks.
- Journey labels are chapter/stage based (every 10 levels).
- XP is awarded on level completion and tracked persistently.

## Android Control Model
- Drag to position paddle.
- Release/tap launch behavior from READY state.
- Laser can be fired from the HUD `FIRE` button (or two-finger gesture path).
- Aim guide updates continuously while dragging.
