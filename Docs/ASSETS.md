# Assets and listing captures

## Canonical authored sources

- Launcher foreground source: root icon2.png; exported foreground:
  app/src/main/res/drawable-nodpi/ic_launcher_foreground_raw.png.
- Android adaptive wrappers/background: res/drawable and mipmap-anydpi-v26.
- Current store icon: store_assets/icon/BreakoutPlus-icon-512.png; feature graphic:
  store_assets/feature_graphic/BreakoutPlus-feature-1024x500.png.
- iOS AppIcon/BrandMark exports: ios/BreakoutPlus/BreakoutPlus/Resources/Assets.xcassets.
  tools/generate_ios_appicon.py is the existing exporter.
- Small offline sound library: app/src/main/res/raw; tools/generate_sfx.py.
  iOS copies live under Resources/Audio. Cross-platform canonical copy automation
  still needs consolidation; do not generate arbitrary extra variants.

Existing sources remain; historical capture dumps and generated builds are not
source assets. No visual redesign/export was claimed in this maintenance milestone.

## Store screenshots: not ready

Old timestamp directories, zero-byte captures and misleading duplicate upload
images were removed. Active canonical upload directories are
fastlane/metadata/android/en-US/images/phoneScreenshots and tenInchScreenshots.
Both require eight distinct PNGs: title, mode_select, gameplay, specialist,
powerup, daily_challenges, scoreboard and settings. The current set is missing;
README intentionally advertises no old screenshot as a current screen.

```sh
python3 tools/check_store_assets.py                 # strict release gate
python3 tools/check_store_assets.py --allow-missing # source CI, reports incomplete
```

Validation rejects zero/truncated/CRC/compression errors, dimensions outside the
project's 320–3840 pixel contract, duplicate hashes, unexpected names and missing
screens. Review Play's current listing requirements and actual screenshot content
before upload; a valid PNG alone does not prove its label or device class.

The old tools/capture_screenshots.sh is a manual/timed helper, not the promised
controlled-state capture pipeline. Its output remains ignored. Deterministic
menu/game/demo states and verified phone/large-screen capture automation are
unfinished work. Select adb serial explicitly, inspect every capture, then promote
only verified images. Publishing lanes fail until the strict set passes.
