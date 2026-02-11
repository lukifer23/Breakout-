#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ADB_BIN="${ADB_BIN:-adb}"
PACKAGE="${BP_PACKAGE:-com.breakoutplus.debug}"
ACTIVITY="${BP_ACTIVITY:-com.breakoutplus.GameActivity}"
FULL_ACTIVITY="${PACKAGE}/${ACTIVITY}"
WAIT_SECONDS="${BP_MODE_WAIT:-4}"
MODES_RAW="${BP_GAME_MODES:-CLASSIC TIMED ENDLESS GOD RUSH VOLLEY SURVIVAL INVADERS}"
AUTO_PLAY="${BP_AUTO_PLAY:-0}"
AUTO_PLAY_SECONDS="${BP_AUTO_PLAY_SECONDS:-0}"

if ! command -v "${ADB_BIN}" >/dev/null 2>&1; then
  echo "adb not found (set ADB_BIN if needed)." >&2
  exit 1
fi

adb_cmd() {
  if [[ -n "${BP_SERIAL:-}" ]]; then
    "${ADB_BIN}" -s "${BP_SERIAL}" "$@"
  else
    "${ADB_BIN}" "$@"
  fi
}

has_fatal_crash() {
  if command -v rg >/dev/null 2>&1; then
    rg -q "FATAL EXCEPTION|AndroidRuntime.*${PACKAGE}"
  else
    grep -Eq "FATAL EXCEPTION|AndroidRuntime.*${PACKAGE}"
  fi
}

print_autoplay_events() {
  if command -v rg >/dev/null 2>&1; then
    rg "BreakoutAutoPlay" || true
  else
    grep -E "BreakoutAutoPlay" || true
  fi
}

echo "Checking connected device..."
adb_cmd get-state >/dev/null

echo "Installing debug build..."
./gradlew installDebug >/dev/null

echo "Starting mode smoke pass on ${FULL_ACTIVITY} (auto_play=${AUTO_PLAY}, auto_play_seconds=${AUTO_PLAY_SECONDS})"
failures=0
adb_cmd logcat -c >/dev/null 2>&1 || true
for mode in ${MODES_RAW}; do
  echo
  echo "[mode:${mode}] launch"
  adb_cmd shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
  START_ARGS=(shell am start -W -n "${FULL_ACTIVITY}" --es extra_mode "${mode}")
  if [[ "${AUTO_PLAY}" == "1" ]]; then
    START_ARGS+=(--ez extra_debug_autoplay true)
    if [[ "${AUTO_PLAY_SECONDS}" =~ ^[0-9]+$ ]] && [[ "${AUTO_PLAY_SECONDS}" -gt 0 ]]; then
      START_ARGS+=(--ei extra_debug_autoplay_seconds "${AUTO_PLAY_SECONDS}")
    fi
  fi
  adb_cmd "${START_ARGS[@]}" >/tmp/bp_mode_start.log 2>&1 || true
  cat /tmp/bp_mode_start.log | tail -n 8

  if ! grep -q "Status: ok" /tmp/bp_mode_start.log; then
    echo "[mode:${mode}] failed to start"
    failures=$((failures + 1))
    continue
  fi

  sleep "${WAIT_SECONDS}"
  if adb_cmd logcat -d | has_fatal_crash; then
    echo "[mode:${mode}] fatal crash found in logcat"
    failures=$((failures + 1))
  else
    echo "[mode:${mode}] pass"
  fi
  if [[ "${AUTO_PLAY}" == "1" ]]; then
    echo "[mode:${mode}] autoplay events:"
    adb_cmd logcat -d | print_autoplay_events | tail -n 6 || true
  fi
  adb_cmd logcat -c >/dev/null 2>&1 || true
done

if [[ "${failures}" -gt 0 ]]; then
  echo
  echo "Mode smoke test failed: ${failures} mode(s) failed."
  exit 1
fi

echo
echo "Mode smoke test passed for all modes."
