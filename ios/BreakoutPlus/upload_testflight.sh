#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
[[ -f private_keys/api_key.json ]] || { echo 'Missing private_keys/api_key.json' >&2; exit 1; }
[[ -f build/TestFlight/BreakoutPlus.ipa ]] || { echo 'Build TestFlight IPA first' >&2; exit 1; }
# Fastlane validates the API-key config and performs the actual upload. Its exit
# status is the result; server processing must still be checked in App Store Connect.
bundle exec fastlane run upload_to_testflight \
  api_key_path:private_keys/api_key.json ipa:build/TestFlight/BreakoutPlus.ipa \
  skip_waiting_for_build_processing:true
