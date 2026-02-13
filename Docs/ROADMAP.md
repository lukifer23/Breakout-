# Roadmap

## Completed (Android Foundation)
- Full Android gameplay stack with OpenGL ES renderer and fixed-step simulation.
- 10 game modes, 10 brick types, 15 powerups.
- Fold-aware UI with phone/tablet/foldable layout support.
- Per-mode scoreboards, daily challenges, progression, and local lifetime stats.
- Advanced mode-specific systems:
  - Volley turn logic and descent pressure.
  - Tunnel Siege fortress + gate identity.
  - Invaders fleet movement, telegraphing, enemy fire, and shield behavior.

## Current Focus (Android Polish)
- Animation/effect coherence (timing, amplitude, transitions).
- Mode balance iteration from playtest feedback.
- HUD consistency across extreme aspect ratios and large displays.
- Stability/performance hardening for release track confidence.
- Viewport calibration pass using folded portrait + unfolded/tablet validation matrix across all modes.

## Next (Android)
- Expanded regression automation for mode-specific edge cases.
- Add layout tuning unit coverage (aspect buckets -> expected row/column boost + HUD reserve bands).
- Additional long-session profiling and memory pressure checks.
- Accessibility polish pass (contrast/readability/input comfort).

## iOS Track (Parallel)
- Keep iOS parity moving without blocking Android release quality work.
