# Assets

## Audio
- Generated locally via `tools/generate_sfx.py`.
- Files live in `app/src/main/res/raw/`:
  - `sfx_bounce.wav`
  - `sfx_brick.wav`
  - `sfx_explosion.wav`
  - `sfx_gameover.wav`
  - `sfx_laser.wav`
  - `sfx_life.wav`
  - `sfx_powerup.wav`
  - `music_loop.wav`

## Icon
- Modern adaptive icon with brickbreaker game elements (bricks, paddle, ball with motion trail, "Breakout+" symbol)
- Built from vector layers in:
  - `app/src/main/res/drawable/ic_launcher_foreground.xml` (main design)
  - `app/src/main/res/drawable/ic_launcher_background.xml` (dark radial gradient)
  - Referenced in `app/src/main/res/mipmap-anydpi-v26/`
- Alternative variant foregrounds available:
  - `app/src/main/res/drawable/ic_launcher_foreground_neon.xml` (orbit ring)
  - `app/src/main/res/drawable/ic_launcher_foreground_fold.xml` (folded hinge)
  - `app/src/main/res/drawable/ic_launcher_foreground_minimal.xml` (minimal plus)

## Visuals
- All UI colors defined in `app/src/main/res/values/colors.xml`.
- Themes and style tokens in `app/src/main/res/values/themes.xml`.
