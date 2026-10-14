# Testing and qualification

Use JDK 17 and Android SDK 36. Exact setup/signing: [BUILD](BUILD.md).

```sh
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleReleaseCheck bundleReleaseCheck
python3 tools/check_repository.py
python3 tools/check_docs.py
python3 tools/check_store_assets.py --allow-missing
bundle exec bundler-audit check --update
ruby tools/check_release_contract.rb
bundle exec fastlane lanes
```

Compile-check APK/AAB are debug signed under a distinct ID/version. They do not
prove production signing, Play acceptance or a release upload.

## Evidence at the October 2026 stopping point

Baseline c1be9b6: 111 tests passed, lint 51 warnings / zero errors, debug and old
CI-fallback release APK/AAB built. Current JVM suite: 139 tests passed locally.
Tests include existing mode/layout/brick/collision policies plus date/version
challenge generation, maximum/accumulator/level-condition semantics, legacy daily
migration, reward save/reload/idempotent reservations, actual atomic checkpoint
files, malformed entities and real-engine restore/replay across all ten modes.

Known seed + scripted neutral input yields equivalent authoritative state across
60/90/120/144/240 Hz render schedules in JVM tests. That does not demonstrate
physical display pacing at those rates. No new tests simulate fake gameplay.

API 36 emulator-5556 (arm64, 1080x2400, 60Hz) actually ran:
- Four-second autoplay launches of all ten modes without fatal logged errors.
- Six controlled seeded stress scenes, with measured CPU/interval/draw/object/
  update distributions; see [PERFORMANCE](PERFORMANCE.md).
- Classic Home/pause, force-stop and fresh Activity recovery: score 100, lives 2,
  elapsed 5.18334, same run/seed/RNG, paused overlay after restoration.

No full Xcode is installed. iOS build/XCTest, physical Fold, tablet, fold/unfold,
TalkBack and long-session playtesting were not verified in this milestone.
There is no Android instrumentation/UI suite yet. The latest lint run reports 73 warnings and zero errors: 34 unused resources,
20 KTX suggestions, 7 dependency suggestions, 4 obsolete SDK branches, 4 text/
typography warnings, and one each for synchronous preference commit, AGP update,
discouraged API and touch accessibility. Synchronous reward commits are intentional
transaction boundaries; API/tool upgrades are deliberate rather than blanket latest
updates. Unused resources are preserved pending asset review. Touch accessibility,
text and obsolete branches remain unfinished qualification work.

## Device utilities

Always select a device explicitly. The mode/progression scripts assemble locally
and install only through their selected adb connection.

```sh
BP_SERIAL=<serial> BP_AUTO_PLAY=1 tools/mode_smoke_test.sh
BP_SERIAL=<serial> tools/god_zen_progression_probe.sh
BP_SERIAL=<serial> tools/all_modes_progression_probe.sh
python3 tools/profile_stress.py --serial <serial> --output /tmp/breakout-perf
```

Debug autoplay/probes/stress fixtures are not competitive play: they do not
record score/XP/stats/daily rewards. Keep captures outside source until validated.

## Remaining acceptance matrix

Phone/tablet/fold portrait and landscape; each mode's objective/end/HUD/score flow;
start/pause/resume/restart/exit; rotation/resize/fold; Activity/process recreation;
settings/name/daily navigation; background/foreground loops; high-ball-count,
explosive-chain, Invaders/Volley/Tunnel long sessions; paddle/spin/minimum vertical
velocity, corners/grazing/high-speed tunneling, Fireball/Pierce/Magnet, moving/
spawning/boss bricks, shots/shields and gate/turn behavior. Add regression tests
from measured failures, then verify on actual hardware. Profiler/Perfetto GPU,
allocation, thermal and high-refresh qualification remains outstanding.
