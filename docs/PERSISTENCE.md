# Local persistence and run recovery

Android remains offline. SharedPreferences stores retain their original names;
this pass does not replace them with a new storage framework.

## Daily objectives and rewards

Daily schema 2 accepts the legacy JSON array. Incomplete legacy combo totals reset
because an accumulated total cannot establish a maximum combo. Completed legacy
objectives and their receipts remain intact. Invalid daily data is quarantined
and regenerated from the date. Date generation uses LocalDate and schema 2;
same date/schema produces the same IDs and objectives.

Counts accumulate, combo/score use maximum achievement, and perfect/time goals
receive a level-completed event with that level's duration and life-loss flag.
Completion is idempotent. Multiball counts actual powerup activations.

A completion commits before its reward outbox. UnlockManager commits the durable
effect and receipt together in reward ledger schema 1. Outbox retries check that
receipt. Theme/cosmetic unlocks persist. Score percentage and streak-brick bonuses
queue for the next scored run, reserve against its UUID, and restore with it.
Zen and debug automation do not consume rewards. Existing XP unlock keys remain
authoritative. A redundant ledger copy supports recovery of a damaged primary;
if both copies are invalid, reward processing fails rather than silently issuing
or losing unknown transactions. The old implementation's already-consumed,
unpersisted run-local bonuses cannot be reconstructed.

## Active run schema 1

Identity is committed before reward reservation. The GL thread captures entities,
mode state, score/lives, timers, level layout, effects, remaining reward balances,
and versioned random-generator positions. An ordered background writer fsyncs a
same-directory temporary file and atomically replaces the checkpoint in
noBackupFilesDir. Periodic snapshots occur once per second and on pause.
Superseded pending snapshots coalesce; an old run cannot replace a newer identity.

A fresh Activity/process in the same mode restores the checkpoint paused. Resume
is explicit. A completed level restores its next-level overlay. Invalid schemas,
enums, nonfinite geometry, malformed entities and oversized snapshots are rejected
and quarantined. A crash before a first checkpoint restarts the same seeded run
with the same reward reservation. Exit/restart/game-over clears or supersedes the
identity. Selecting another mode starts a new run; there is one active save slot.

Particles, trails, flashes, input pointer IDs, audio playback position, and GL
resources are excluded. Abrupt process death can roll back up to one checkpoint
interval plus queued write latency. Newer durable daily progress/receipts survive
an older checkpoint. Precise cross-store atomicity between simulation snapshots,
daily progress, lifetime stats and progression is not yet established; this is an
open qualification item rather than a claim of exact crash replay of every side
effect.

## Verification recorded October 5, 2026

Real-engine JVM tests resume all ten modes and compare subsequent authoritative
states against uninterrupted simulation. Seed plus scripted input replays across
60/90/120/144/240 Hz render schedules. Actual temporary files exercise atomic
replacement, interrupted temporary writes, reward save/reload and receipt retry.
Malformed snapshot and legacy daily migration tests run in the same suite.

On emulator-5556 (API 36, 1080x2400), an actual Classic run reached score 100,
2 lives, elapsed 5.18334 seconds. Home/pause, force-stop, and a fresh Activity
launch restored run UUID, seed, RNG position, score, lives, level and elapsed
exactly, and showed the paused overlay. This establishes one process-recovery
case, not the full rotation/foldable/UI stress matrix. Full Xcode is unavailable
on this host; these changes do not establish iOS parity. General settings, scores,
XP and lifetime-stat schema consolidation remains work in progress.
