# Renderer measurements — October 5, 2026

## Method and scope

API 36 arm64 emulator-5556, 1080x2400, 60 Hz; Apple M3 Pro macOS host.
SurfaceFlinger identifies Google (Apple) / Android Emulator OpenGL ES Translator,
OpenGL ES 3.0 (4.1 Metal - 91.7). The app requests GLES2. Debug APK, seed
20261005, six actual engine stress scenarios. Each capture lasts 12 seconds;
the first 120 samples are discarded. No other Gradle builds run during capture.
No publishing credentials or remote telemetry participate.

CPU time measures GameRenderer simulation + feedback drain + shape generation +
GL submission, excluding the following logcat write. Interval measures starts of
successive render callbacks. It is not GPU execution time or a presentation fence.
Objects and simulation ticks are captured with every sample. Logcat overhead is
present in both versions. Scenarios have real moving/destroyed objects; counts can
vary with frame timing and the existing visual stress adaptation.

Before: pooled records still issue one matrix/color/draw per shape. After:
ordered position/color triangle streams, bulk NIO copy, reused dynamic GLES VBO,
28-segment circles, one projection upload per flush. Chunk capacity is 16,380
vertices; explicit layer boundaries flush. There are no per-frame buffer objects.
Shader compile/link status and logs are checked; context recreation rebuilds
programs, locations and VBOs. Visual animation now uses an accumulating local
clock rather than large uptime floats.

## Measured results

Single captures; repeated physical-device results are not established.

| Scenario | Frames before/after | Mean CPU ms before/after | CPU p95 ms before/after | Mean draws before/after | Intervals >20ms before/after |
|---|---:|---:|---:|---:|---:|
| dense | 604/604 | 1.283/1.066 | 1.601/1.929 | 543.54/3.00 | 1/14 |
| multiball | 590/584 | 2.151/1.396 | 3.292/2.717 | 808.99/3.48 | 1/3 |
| particles | 605/603 | 1.492/1.341 | 1.823/2.255 | 691.36/3.40 | 0/11 |
| invaders | 591/587 | 3.200/1.781 | 4.025/3.408 | 1849.62/5.03 | 0/13 |
| volley | 606/605 | 1.648/1.221 | 2.714/2.140 | 554.04/3.79 | 3/0 |
| tunnel | 599/604 | 1.588/0.732 | 1.937/1.151 | 805.53/3.00 | 1/1 |

Raw metric distributions: [before](evidence/renderer-before.json) and
[batched](evidence/renderer-batched.json). Mean CPU falls in each capture and GL
calls drop by over 99%. Dense/particle p95 and several slow-frame counts worsen.
This supports lower average submission cost, not a universal frame-pacing win.
GPU/presentation profiling and repeated measurements on physical high-refresh
hardware remain required before an overall smoothness claim.

An earlier implementation wrote each vertex component directly to NIO. Dense
CPU p95 was 5.839ms; multiball 49.824ms, and particles produced too few frames for
the minimum sample gate. That implementation was rejected. Bulk array upload
removed that overhead (separate 20-second exploratory captures: dense p95
1.296ms, multiball 2.571ms). Different durations make those exploratory numbers
inappropriate substitutes for the table's consistent 12-second captures.

## Reproduce

Build/install only on the selected device; scripts never install to every attached
device. Debug automation does not persist scores, XP, stats, daily progress or
consume daily rewards.

```sh
./gradlew assembleDebug
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
python3 tools/profile_stress.py --serial emulator-5556 --output /tmp/breakout-perf
```

Use `--adb /absolute/path/to/adb` if needed. Capture logs/summary outside source;
only reviewed measurement summaries belong in Docs/evidence. The script fails
on launch errors, fatal crashes and insufficient samples. It does not fabricate
missing measurements. Allocation profiling, GPU timelines, battery/thermal
behavior and physical 90/120Hz frame pacing are not measured by this script.
