# Store Assets

Drop Play Store graphics and screenshots into the subfolders below.

- `icon/` for the 512x512 Play Store icon
- `feature_graphic/` for the 1024x500 feature graphic
- `screenshots/phone/` for phone screenshots (2-8)
- `screenshots/tablet/` for large-screen screenshots (4+ if targeting tablets/Chromebook)

Generated assets:
- Run `tools/generate_store_assets.py` to generate the icon and feature graphic.
- Run `tools/capture_screenshots.sh` to capture device screenshots via ADB.
  - Set `BP_DISPLAY_ID` if multiple displays are detected (foldables).

Recommended filenames are listed in `Docs/STORE_LISTING.md`.
