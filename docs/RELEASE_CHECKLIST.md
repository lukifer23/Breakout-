# Release checklist

Current release line: 1.0.12 / code 12. The maintenance milestone is not a
published release. This is the authoritative checklist; historical sign-offs
under Archive do not check these items automatically.

## Source and automated checks

- [x] Generated Swift/Gradle build products, test binary, project backup and old
  APK dumps removed from tracking; no history rewrite.
- [x] Compile/target API 36 with stable compatible build tools.
- [x] Date-based daily semantics, durable reward receipts and seeded run recovery
  have real JVM regression tests.
- [x] Real GLES batching/shader checks and bounded background diagnostic writes.
- [x] Updated locked Ruby graph passes advisory scan and release contract tests.
- [ ] Remote main CI verified for the exact landed commit (record run URL).
- [ ] All remaining lint warnings assessed/fixed or individually justified.
- [x] Fresh-clone full gate: clean/unit/lint/debug/releaseCheck APK/AAB.

## Product/device acceptance

- [ ] HUD contrast, TalkBack/text scaling/48dp controls, menus/insets verified.
- [ ] Ten-mode objectives/end/HUD/score/restart flows fully playtested.
- [ ] Phone/tablet/fold resizing, rotation, background and recreation verified.
- [ ] Cross-store crash side effects and all legacy persistence migrations tested.
- [ ] Full physics/high-speed/ball-count regression matrix completed.
- [ ] Physical 60/high-refresh profiling and long-session audio/thermal checks.
- [ ] iOS separately qualified; do not infer parity from Android results.

## Publishable artifacts and store

- [ ] Increment authoritative version.properties name/code above 12.
- [ ] Release signing present; bundleRelease certificate matches Play upload key.
- [ ] Strict `python3 tools/check_store_assets.py` passes phone/large-screen images.
- [ ] Store metadata/screenshots/privacy URL and Data Safety reviewed together.
- [ ] Target API / content rating / app access / testing requirements confirmed
  in Play Console for the actual submission.
- [ ] Explicit operator upload to internal track; review pre-launch report.
- [ ] Release notes/changelog match the actual candidate.
- [ ] Approve rollout, verify listing/install/update and monitor ANR/crash reports.

Local compile_check has `.compilecheck` ID and debug signature; it is not a
publishable release. Building never requires Play credentials; upload does.
See [BUILD](BUILD.md), [TESTING](TESTING.md) and [privacy](PRIVACY_POLICY.md).
