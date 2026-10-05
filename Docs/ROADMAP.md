# Remaining modernization work

The October 2026 maintenance milestone is paused after build/core/daily/recovery/
batching/tooling cleanup. It is not the completion of the original full pass.
Completed work/evidence: [README](../README.md), [baseline](MODERNIZATION_BASELINE.md),
[persistence](PERSISTENCE.md), [performance](PERFORMANCE.md).

## Priority 0 — qualification and durable data

- Fix observed white HUD text on the light shell; verify phone/tablet/night
  contrast, text scaling, insets and TalkBack.
- Version, migrate, validate and recover all older preference stores. Use Long
  for growing XP/lifetime counters. Finish reward-ledger double-corruption
  recovery and bounded reservation/receipt retention.
- Establish atomic/idempotent cross-store completion/stat/XP side effects.
  Checkpoint rollback can currently replay already-persisted side effects.
- Complete release compile/signature verification and real device resize/lifecycle
  QA. Increment version/code before publishing; never reuse code 12 for an update.
- Capture verified canonical phone and large-screen screenshots. Old duplicate/
  mislabeled/timestamp captures were removed; the listing is not ready to upload.

## Priority 1 — gameplay, maintainability and performance

- Physics regression matrix and high-speed substep/sweep analysis; current
  substep cap is four. Do not alter feel constants without evidence.
- Continue extracting simulation/mode/effect coordination from GameEngine.
- Extract LevelFactory authored/procedural/special/difficulty/theme/validation
  responsibilities and define seed behavior without random balance changes.
- Repeat renderer measurements on physical 60/high-refresh devices. Investigate
  dense/particle p95 and slow-frame regressions; profile GPU/allocations/thermal
  behavior instead of assuming fewer calls means universally smoother frames.
- Audit SoundPool readiness/spam, audio focus/duck restoration and pause/restart.
- Add reduced-motion/flashing and ball legibility controls, progressive mechanic
  reference/onboarding and all-mode UI/instrumentation coverage.

## Priority 2 — content/platform consolidation

- Data-driven Challenge Journey, then a small curated teaching pack after the
  correctness/device gates are green. No new normal game mode or monetization.
- iOS XCTest/macOS CI and deterministic Android/iOS parity fixtures; fix daily
  reward, collision, Volley, Tunnel, Magnet, HUD and level-advance gaps.
- Evidence-based KMP assessment after neutral core/parity work, not before it.
- Consolidate icon/store/shell/gameplay exports around the documented brand and
  add a controlled screenshot-state workflow; no generated variant pile.

Historical investigations live under Docs/Archive and ios/Archive. They do not
supersede current build instructions or establish current release evidence.
