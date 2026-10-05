fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build signed release AAB locally (no Play credentials needed)

### android compile_check

```sh
[bundle exec] fastlane android compile_check
```

Validate minified APK and AAB, debug signed with .compilecheck application ID

### android upload_internal

```sh
[bundle exec] fastlane android upload_internal
```

Build and upload signed AAB and metadata to the internal track as draft

### android publish_internal

```sh
[bundle exec] fastlane android publish_internal
```

Build and publish signed AAB to internal track

### android publish_internal_existing

```sh
[bundle exec] fastlane android publish_internal_existing
```

Publish existing internal release for the authoritative version code

### android upload_metadata

```sh
[bundle exec] fastlane android upload_metadata
```

Upload metadata only (no signing or AAB needed)

### android build_and_upload_internal

```sh
[bundle exec] fastlane android build_and_upload_internal
```

Build and upload signed AAB to internal track as draft

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
