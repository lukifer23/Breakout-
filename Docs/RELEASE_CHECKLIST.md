# Play Store Release Checklist

## Build Artifacts
- [ ] `./gradlew bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`
- [x] Version code/name updated in `app/build.gradle.kts` (1.0.11 / versionCode 11)
- [x] Android validation gates pass: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`
- [x] GitHub Actions CI workflow added (`.github/workflows/android.yml`)
- [ ] Device smoke test passes across all modes (`tools/mode_smoke_test.sh`)
- [ ] Deterministic progression probes pass (`tools/god_zen_progression_probe.sh` and `tools/all_modes_progression_probe.sh`)
- [ ] If multiple devices are connected, `BP_SERIAL` is set for probe scripts

## Play Console Setup
- [ ] App created in Play Console
- [ ] App signing by Google Play enabled
- [ ] Service account JSON key stored locally (gitignored) for automated uploads
- [ ] `GOOGLE_PLAY_JSON` (or equivalent) env var set before Fastlane uploads
- [ ] Store listing completed (title, short/long description, category, contact email)
- [ ] App icon uploaded (512x512 PNG, 32-bit, <= 1024 KB)
- [ ] Feature graphic uploaded (1024x500)
- [ ] Phone screenshots uploaded (2-8)
- [ ] Tablet/Chromebook screenshots uploaded (4+ if targeting large screens)
- [ ] Privacy policy URL set
- [ ] Data Safety form completed (see `DATA_SAFETY.md`)
- [ ] Content rating questionnaire completed
- [ ] App access declaration completed

## Release Track
- [ ] Internal testing track created
- [ ] AAB uploaded to internal track
- [ ] Fastlane upload verified (AAB + metadata/screenshots)
- [ ] Release notes added for current version
- [ ] Pre-launch report reviewed
- [ ] Closed or Open testing track ready (optional)
- [ ] Production rollout configured

## Post-Release
- [ ] Verify listing on Play Store
- [ ] Monitor ANR/Crash reports
- [ ] Review user feedback

## Documentation Sync
- [x] `README.md` reflects latest Android runtime and validation state
- [x] `Docs/PARITY.md` and `Docs/HARDENING_SIGNOFF.md` added
- [x] `Docs/ARCHITECTURE.md`, `Docs/GAMEPLAY.md`, `Docs/DESIGN.md` updated for 1.0.11
- [x] `Docs/TESTING.md` updated for 1.0.11 (CI, test count, Volley 5-ball checklist)
- [ ] `Docs/TESTING.md` device matrix results recorded after manual QA pass
