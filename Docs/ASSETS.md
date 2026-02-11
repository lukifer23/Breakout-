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
- Adaptive icon background: `app/src/main/res/drawable/ic_launcher_background.xml`.
- Adaptive icon foreground wrapper: `app/src/main/res/drawable/ic_launcher_foreground_asset.xml`.
- Foreground source image: `app/src/main/res/drawable-nodpi/ic_launcher_foreground_raw.png` (1024x1024 RGBA).
- Current foreground is generated from `icon2.png` with safe-zone padding to avoid launcher-mask clipping.
- Adaptive icon XML references:
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

Rebuild the foreground from `icon2.png` (center crop + safe padding):
```bash
ffmpeg -y -i icon2.png -vf "crop=1024:1024:0:256,scale=920:920,pad=1024:1024:52:52:color=black@0" -frames:v 1 app/src/main/res/drawable-nodpi/ic_launcher_foreground_raw.png
```

## Visuals
- All UI colors defined in `app/src/main/res/values/colors.xml`.
- Themes and style tokens in `app/src/main/res/values/themes.xml`.

## Play Store Assets
Store listing assets live under `store_assets/`:
- Icon: `store_assets/icon/BreakoutPlus-icon-512.png` (512x512 PNG, 32-bit, <= 1024 KB)
- Feature graphic: `store_assets/feature_graphic/BreakoutPlus-feature-1024x500.png`
- Screenshots: `store_assets/screenshots/phone/` and `store_assets/screenshots/tablet/`

Use `tools/capture_screenshots.sh` to capture device screenshots via ADB. The script launches the app before each shot and can auto-capture with a timed delay.
For emulator captures (repeatable device sizes), you can boot an AVD directly:
```bash
export BP_EMULATOR_AVD="Pixel_7"
export BP_PREFER_EMULATOR=1
export BP_EMULATOR_HEADLESS=0
tools/capture_screenshots.sh phone
```
If multiple devices are connected, select one with:
```bash
export BP_SERIAL="emulator-5554"
```
If the app is not installed on the target device, the script will attempt to install `app/build/outputs/apk/debug/app-debug.apk`. You can override with:
```bash
export BP_APK="/absolute/path/to/app-debug.apk"
```
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

Optional auto-capture (no prompts, 4s delay between shots):
```bash
export BP_AUTO=1
export BP_SHOT_DELAY=4
tools/capture_screenshots.sh phone
```

Capture specific shots and modes (debug builds can auto-spawn powerups):
```bash
export BP_SHOTS="title mode_select gameplay powerup pause scoreboard daily_challenges settings"
export BP_GAME_MODES="CLASSIC RUSH VOLLEY GOD TIMED INVADERS"
export BP_AUTO=1
export BP_AUTO_PLAY=1
export BP_PLAY_WAIT=2
export BP_POWERUP_TYPE="LASER"
tools/capture_screenshots.sh phone
```
