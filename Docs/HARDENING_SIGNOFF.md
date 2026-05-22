# Android Hardening Sign-Off

Status: **Complete** (2026-05-22)  
Scope: Android hardening initiative — Play Store release is a separate follow-on.

## Exit Criteria

| Gate | Status |
|------|--------|
| Doc drift resolved (GAMEPLAY, .github/README, PARITY.md) | Done |
| GitHub Actions CI (test + lint + assembleDebug) | Done |
| Mac target frozen (documented) | Done |
| Volley ball count locked (5 start, 20 max) + turn stall tests | Done |
| Tunnel supply/pity + gate integrity regression tests | Done |
| GOD/ZEN silent auto-advance verified in GameActivity | Done |
| InvadersModeSystem extracted + tested | Done |
| Mode accent colors unified (Tunnel vs Survival) | Done |
| Version bumped to 1.0.11 | Done |

## Validation Commands

Run locally before merge or release sign-off (JDK 17 required):

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

With a connected Android device:

```bash
tools/mode_smoke_test.sh
tools/god_zen_progression_probe.sh
tools/all_modes_progression_probe.sh
# Multi-device: BP_SERIAL=<serial> ...
```

## Device Matrix (Manual QA)

Validate on phone portrait, fold folded, fold unfolded, and slate where available:

- All 10 modes: launch, play, level advance or game over
- Volley: 15+ minute session, no turn stalls
- Tunnel: supply readiness HUD updates, pity drop under pressure
- Zen: no level-complete overlay; silent auto-advance
- GOD: manual skip + auto-advance recovery

## Deferred (Not Blocking Sign-Off)

- Play Store upload (signing, tablet screenshots, Console forms) — see `RELEASE_CHECKLIST.md`
- iOS parity execution — see `PARITY.md` Phase 5 backlog
- BreakoutPlusMac — frozen, no changes

## Key Changes (1.0.11)

- Added `ModeAccent`, `InvadersModeSystem`, `Docs/PARITY.md`, CI workflow
- Volley constants in `VolleyModeSystem` (starting balls = 5)
- Survival mode accent: `bp_flame` (#FF8A3D), distinct from Tunnel orange
- Expanded unit tests: Volley, Tunnel gate metrics, Invaders pacing, ModeAccent
