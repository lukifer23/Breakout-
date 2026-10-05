# Data Safety Summary

## Data Collection
Breakout+ does not collect or transmit user data off-device.

## Data Stored On Device
- Settings (sound, music, vibration, sensitivity, refresh‑rate preference)
- Scoreboard entries (player name, score, mode, level, duration)
- Progression, unlocks, lifetime statistics and daily challenge progress
- Optional gameplay logs (only if “Enable Game Logging” is turned on)

## Data Sharing
No data is shared with third parties. Gameplay logs (when enabled) are stored only on device and are never transmitted.

## Security
The app performs no data uploads. No accounts or network services are required.
OS-managed backup behavior differs by platform as described below.

## Android backup and deletion

Android cloud backup and OS-managed device transfer of application data are
disabled/excluded by the manifest and extraction rules. Uninstalling or clearing
app storage removes local progress. Breakout+ has no network permission or
telemetry service. Optional diagnostic logs are local and disabled by default.
The separate iOS port uses local UserDefaults, which may participate in
Apple-managed device backup; the app itself does not upload that data.
