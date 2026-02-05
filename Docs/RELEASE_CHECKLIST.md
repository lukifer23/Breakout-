# Play Store Release Checklist

## Build Artifacts
- [ ] `./gradlew bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`
- [ ] Version code/name updated in `app/build.gradle.kts`

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
