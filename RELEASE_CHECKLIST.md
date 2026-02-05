# Play Store Release Checklist

## Build Artifacts
- [ ] `./gradlew bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`
- [ ] Version code/name updated in `app/build.gradle.kts`

## Play Console Setup
- [ ] App created in Play Console
- [ ] App signing by Google Play enabled
- [ ] Store listing completed (title, short/long description, category, contact email)
- [ ] Feature graphic uploaded
- [ ] Phone + tablet screenshots uploaded
- [ ] Privacy policy URL set
- [ ] Data Safety form completed (see `DATA_SAFETY.md`)
- [ ] Data Safety form completed
- [ ] Content rating questionnaire completed
- [ ] App access declaration completed

## Release Track
- [ ] Internal testing track created
- [ ] AAB uploaded to internal track
- [ ] Pre-launch report reviewed
- [ ] Closed or Open testing track ready (optional)
- [ ] Production rollout configured

## Post-Release
- [ ] Verify listing on Play Store
- [ ] Monitor ANR/Crash reports
- [ ] Review user feedback
