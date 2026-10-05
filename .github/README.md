# Breakout+ repository automation

The [root README](../README.md) is the project source of truth.
[Android CI](workflows/android.yml) runs clean unit/lint/debug/minified APK/AAB
compile checks plus Ruby advisory and release-contract checks. Dependabot checks
Bundler, Gradle and GitHub Actions. Generated products never belong in source.
See [build/release](../Docs/BUILD.md) and [release checklist](../Docs/RELEASE_CHECKLIST.md).
