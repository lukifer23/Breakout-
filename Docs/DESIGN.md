# Breakout+ Design & UX

## Visual Direction
- Light porcelain/frosted Android shell with sky-blue accents; dark neon gameplay.
- Dark mode is a native shell variant. Existing glossy icon remains an asset source,
  not a reason to redesign gameplay. Store/brand export consolidation is unfinished.
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
| Classic | `bp_cyan` | #22D3EE |
| Timed Challenge | `bp_gold` | #FBBF24 |
| Endless | `bp_green` | #34D399 |
| God Mode | `bp_magenta` | #F472B6 |
| Level Rush | `bp_red` | #F87171 |
| Volley | `bp_azure` | #60A5FA |
| Tunnel Siege | `bp_orange` | #FB923C |
| Survival | `bp_flame` | #F97316 |
| Invaders | `bp_violet` | #A78BFA |
| Zen Mode | `bp_gray` | #64748B |

Use `ModeAccent.colorRes(mode)` in Android UI code. Cross-platform parity matrix: `Docs/PARITY.md`.

## Foldable / Large Screen
- `sw600dp` and `sw720dp` layout variants for larger displays.
- Game overlays use `hud_glass_panel_elevated` on all form factors.
- Mode select uses a 2-column card grid on tablet/slate.
- Daily Challenges and Privacy use max-width tablet layouts at `sw600dp`.
- `FoldAwareActivity` observes hinge padding; common menu system insets need further audit.
- Handedness toggle keeps high-priority controls reachable.

## Glass Drawable Catalog
- Shell panels: `glass_panel`, `glass_panel_elevated`, `card_background` (compact radius).
- HUD overlays: `hud_glass_panel_elevated`, `hud_glass_button_icon`, `hud_chip`, `hud_banner`.
- Buttons: `glass_button_primary|secondary|gold|green|teal|azure|danger|icon`.
- Tokens: `bp_glass_fill*`, `bp_glass_stroke*`, `bp_hud_glass_*` in `colors_hud.xml`.

## Source-of-truth tokens and observed gaps

Android res/values/colors.xml defines light shell canvas #F0F4FA, surface #FAFCFF,
primary text #1E293B and sky accent #38BDF8. values-night supplies native night
variants. colors_hud.xml defines gameplay text/glass; dimens/styles/drawables
define actual spacing, radius and sans-serif typography. UiMotion defines motion
envelopes. ModeAccent maps mode tokens; gameplay LevelThemes is separate.

The existing HUD uses pale text over a light shell area and was visibly low
contrast in the API 36 capture. Fixing and validating that, text scaling, TalkBack
and reduced motion/flashing controls remains prioritized work. This document
records current tokens, not a claim that the visual/accessibility pass is complete.
