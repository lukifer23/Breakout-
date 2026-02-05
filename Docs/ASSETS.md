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
- Modern adaptive icon with a neon orbit ring, plus mark, ball, and paddle
- Default foreground: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Background: `app/src/main/res/drawable/ic_launcher_background.xml` (dark radial gradient)
- Referenced in `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`
- Alternative variant foregrounds available:
  - `app/src/main/res/drawable/ic_launcher_foreground_neon.xml` (orbit ring)
  - `app/src/main/res/drawable/ic_launcher_foreground_fold.xml` (folded hinge)
  - `app/src/main/res/drawable/ic_launcher_foreground_minimal.xml` (minimal plus)
- To switch default icon: replace the drawable reference in `mipmap-anydpi-v26/ic_launcher.xml` (e.g., change `@drawable/ic_launcher_foreground` to `@drawable/ic_launcher_foreground_neon`)

## Visuals
- All UI colors defined in `app/src/main/res/values/colors.xml`.
- Themes and style tokens in `app/src/main/res/values/themes.xml`.

## Play Store Assets
Store listing assets live under `store_assets/`:
- Icon: `store_assets/icon/BreakoutPlus-icon-512.png` (512x512 PNG, 32-bit, <= 1024 KB)
- Feature graphic: `store_assets/feature_graphic/BreakoutPlus-feature-1024x500.png`
- Screenshots: `store_assets/screenshots/phone/` and `store_assets/screenshots/tablet/`

Use `tools/capture_screenshots.sh` to capture device screenshots via ADB.
Generate icon + feature graphic with:
```bash
python3 -m venv tools/.venv
tools/.venv/bin/pip install -r tools/requirements.txt
tools/.venv/bin/python tools/generate_store_assets.py
```

If the device has multiple displays (foldables), set a display id:
```bash
export BP_DISPLAY_ID=0
tools/capture_screenshots.sh phone
```
List display ids with:
```bash
adb shell dumpsys SurfaceFlinger --display-id
```
