#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ADB_BIN="${ADB_BIN:-adb}"
PACKAGE="${BP_PACKAGE:-com.breakoutplus.debug}"
ACTIVITY="${BP_ACTIVITY:-com.breakoutplus.GameActivity}"
FULL_ACTIVITY="${PACKAGE}/${ACTIVITY}"
RUN_SECONDS="${BP_PROGRESSION_RUN_SECONDS:-35}"
WAIT_PAD_SECONDS="${BP_PROGRESSION_WAIT_PAD_SECONDS:-8}"
MODES_RAW="${BP_PROGRESSION_MODES:-GOD ZEN}"
MODE_ATTEMPTS="${BP_PROGRESSION_MODE_ATTEMPTS:-2}"

if ! [[ "${RUN_SECONDS}" =~ ^[0-9]+$ ]]; then
  RUN_SECONDS=35
fi
if [[ "${RUN_SECONDS}" -lt 20 ]]; then
  RUN_SECONDS=20
fi
if ! [[ "${WAIT_PAD_SECONDS}" =~ ^[0-9]+$ ]]; then
  WAIT_PAD_SECONDS=8
fi
if ! [[ "${MODE_ATTEMPTS}" =~ ^[0-9]+$ ]]; then
  MODE_ATTEMPTS=2
fi
if [[ "${MODE_ATTEMPTS}" -lt 1 ]]; then
  MODE_ATTEMPTS=1
fi

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

resolve_serial_if_needed() {
  if [[ -z "${BP_SERIAL:-}" ]]; then
    return
  fi
  if "${ADB_BIN}" -s "${BP_SERIAL}" get-state >/dev/null 2>&1; then
    return
  fi

  local inferred
  inferred="$(${ADB_BIN} devices -l | awk -v base="${BP_SERIAL}" '
    $2 == "device" && ($1 == base || index($1, base) > 0 || index($1, "adb-" base "-") == 1) { print $1; exit }
  ')"
  if [[ -n "${inferred}" ]]; then
    echo "Resolved BP_SERIAL ${BP_SERIAL} -> ${inferred}"
    BP_SERIAL="${inferred}"
  fi
}

wait_for_device_ready() {
  local retries="${1:-12}"
  local delay_seconds="${2:-5}"
  local i
  for ((i=1; i<=retries; i++)); do
    if adb_cmd get-state >/dev/null 2>&1; then
      return 0
    fi
    sleep "${delay_seconds}"
  done
  return 1
}

ensure_device_awake() {
  adb_cmd shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb_cmd shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb_cmd shell input keyevent 82 >/dev/null 2>&1 || true
}

wait_with_keep_awake() {
  local total_seconds="$1"
  local remaining="${total_seconds}"
  while [[ "${remaining}" -gt 0 ]]; do
    local slice=5
    if [[ "${remaining}" -lt "${slice}" ]]; then
      slice="${remaining}"
    fi
    sleep "${slice}"
    ensure_device_awake
    remaining=$((remaining - slice))
  done
}

has_fatal_crash() {
  local file="$1"
  if command -v rg >/dev/null 2>&1; then
    rg -q "FATAL EXCEPTION|AndroidRuntime.*${PACKAGE}" "${file}"
  else
    grep -Eq "FATAL EXCEPTION|AndroidRuntime.*${PACKAGE}" "${file}"
  fi
}

extract_max_level_start() {
  local file="$1"
  local mode="$2"
  if command -v rg >/dev/null 2>&1; then
    rg -o "event=level_start mode=${mode} level=[0-9]+" "${file}" | sed -E 's/.*level=([0-9]+)/\1/' | sort -nr | head -n 1
  else
    grep -Eo "event=level_start mode=${mode} level=[0-9]+" "${file}" | sed -E 's/.*level=([0-9]+)/\1/' | sort -nr | head -n 1
  fi
}

has_event() {
  local file="$1"
  local pattern="$2"
  if command -v rg >/dev/null 2>&1; then
    rg -q "${pattern}" "${file}"
  else
    grep -Eq "${pattern}" "${file}"
  fi
}

echo "Checking connected device..."
resolve_serial_if_needed
if ! wait_for_device_ready 12 5; then
  echo "No ready adb device found. Connect a device or set BP_SERIAL to an active serial from 'adb devices -l'." >&2
  exit 1
fi

echo "Installing debug build..."
./gradlew installDebug >/dev/null

echo "Running deterministic progression probe on ${FULL_ACTIVITY} (run_seconds=${RUN_SECONDS})"
failures=0
for mode in ${MODES_RAW}; do
  echo
  echo "[mode:${mode}] launch"
  mode_passed=0
  mode_error="unknown"
  for ((attempt=1; attempt<=MODE_ATTEMPTS; attempt++)); do
    if ! wait_for_device_ready 6 5; then
      mode_error="device not ready"
      break
    fi
    ensure_device_awake
    adb_cmd logcat -c >/dev/null 2>&1 || true
    adb_cmd shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
    adb_cmd shell am start -W -n "${FULL_ACTIVITY}" \
      --es extra_mode "${mode}" \
      --ez extra_debug_autoplay true \
      --ei extra_debug_autoplay_seconds "${RUN_SECONDS}" \
      --ez extra_debug_progression_probe true >/tmp/bp_progression_start.log 2>&1 || true
    cat /tmp/bp_progression_start.log | tail -n 8

    if ! grep -q "Status: ok" /tmp/bp_progression_start.log; then
      mode_error="failed to start"
      if [[ "${attempt}" -lt "${MODE_ATTEMPTS}" ]]; then
        echo "[mode:${mode}] retrying after start failure (attempt ${attempt}/${MODE_ATTEMPTS})"
        sleep 2
        continue
      fi
      break
    fi

    wait_with_keep_awake "$((RUN_SECONDS + WAIT_PAD_SECONDS))"
    log_file="/tmp/bp_progression_${mode}.log"
    if ! adb_cmd logcat -d >"${log_file}"; then
      mode_error="failed to read logcat"
      if [[ "${attempt}" -lt "${MODE_ATTEMPTS}" ]]; then
        echo "[mode:${mode}] retrying after logcat read failure (attempt ${attempt}/${MODE_ATTEMPTS})"
        sleep 2
        continue
      fi
      break
    fi

    if has_fatal_crash "${log_file}"; then
      mode_error="fatal crash found in logcat"
      break
    fi

    max_level="$(extract_max_level_start "${log_file}" "${mode}")"
    if [[ -z "${max_level}" ]]; then
      max_level=0
    fi
    has_level_complete=0
    if has_event "${log_file}" "event=level_complete mode=${mode}"; then
      has_level_complete=1
    fi

    if has_event "${log_file}" "event=next_level_fallback mode=${mode}"; then
      mode_error="auto-advance fallback detected"
      break
    fi

    if [[ "${has_level_complete}" -eq 1 || "${max_level}" -ge 2 ]]; then
      mode_passed=1
      echo "[mode:${mode}] pass (max_level_start=${max_level}, level_complete=${has_level_complete})"
      break
    fi

    mode_error="missing progression events"
    if [[ "${attempt}" -lt "${MODE_ATTEMPTS}" ]]; then
      echo "[mode:${mode}] retrying after missing progression events (attempt ${attempt}/${MODE_ATTEMPTS})"
      sleep 2
      continue
    fi
  done

  if [[ "${mode_passed}" -eq 0 ]]; then
    echo "[mode:${mode}] ${mode_error}"
    failures=$((failures + 1))
  fi
done

if [[ "${failures}" -gt 0 ]]; then
  echo
  echo "Progression probe failed: ${failures} mode(s) failed."
  exit 1
fi

echo
echo "Progression probe passed for all modes."
