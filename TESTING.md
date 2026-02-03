# Testing

## Manual Test Checklist
- Launch app → Splash → Main menu appears.
- Play Classic mode: paddle controls, ball launch, brick collision.
- Timed Challenge: timer counts down, game ends at 0.
- Endless: levels advance with difficulty scaling.
- God Mode: no life loss on miss.
- Level Rush: per-level timer resets.
- Powerups drop and activate correctly.
- Explosive bricks damage neighbors.
- Unbreakable bricks require fireball/laser.
- Scoreboard persists after game over.
- Settings toggles persist between launches.
- Left-handed mode moves pause button to left.
- Fold/unfold device and confirm UI reflows + padding avoids hinge.

## Performance Targets
- 60 FPS or higher during gameplay on Z Fold 7.
- No frame spikes during multi-ball or heavy explosions.

## Build Verification
```bash
./gradlew assembleDebug
```

## Unit Tests (JVM)
```bash
./gradlew testDebugUnitTest
```

## What We Cover
- Level patterns include advanced brick types (moving/spawning/phase/boss).
- Difficulty scaling increases hit points.
- Mode launch speeds are positive and balanced.
