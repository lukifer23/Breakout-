package com.breakoutplus.game

import java.nio.file.Files
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class RunRestoreTest {
    private fun config(mode: GameMode) = GameConfig(mode,
        GameSettings(false, false, false, false, false, 0.7f),
        seed = 1234L, runId = "restoration-$mode", persistRun = false)
    private fun engine(config: GameConfig) = GameEngine(config, RecordingGameEvents(), GameFeedbackQueue())
    private fun state(engine: GameEngine) = JSONObject(engine.captureRun().encode()).apply {
        // FX capacity after restoring without particles can change only the visual stream.
        remove("visualRngState")
    }
    private fun assertEquivalent(first: GameEngine, second: GameEngine) {
        val a = state(first); val b = state(second)
        fun canonical(value: Any): Any = when (value) {
            is JSONObject -> value.keys().asSequence().toList().sorted().associateWith { canonical(value.get(it)) }
            is JSONArray -> (0 until value.length()).map { canonical(value.get(it)) }
            is Number -> value.toDouble()
            else -> value
        }
        assertEquals("Authoritative run states differ", canonical(a), canonical(b))
    }

    @Test fun everyModeResumesTheActualSimulationFromCheckpoint() {
        for (mode in GameMode.entries) {
            val config = config(mode)
            val original = engine(config)
            original.onResize(1080, 2200)
            original.launchBall()
            repeat(160) { original.update(FixedStepClock.STEP) }
            val checkpoint = RunSnapshot.decode(original.captureRun().encode())
            val resumed = engine(config.copy(initialState = checkpoint))
            assertEquivalent(original, resumed)
            repeat(240) {
                original.update(FixedStepClock.STEP); resumed.update(FixedStepClock.STEP)
            }
            assertEquivalent(original, resumed)
        }
    }

    @Test fun restoresEffectsMagnetQueueShotsAndRemainingBonuses() {
        val config = config(GameMode.INVADERS).copy(rewardBonuses = RewardBonuses(15, 5))
        val original = engine(config)
        original.applyPowerup(PowerUpType.MAGNET)
        original.applyPowerup(PowerUpType.LASER)
        original.streakBonusRemaining = 2
        original.balls.first().stuckToPaddle = true
        original.balls.first().stickOffset = 3f
        original.enemyShots.add(EnemyShot(10f, 20f, 1f, 1f, -20f, floatArrayOf(1f, 1f, 1f, 1f)))
        val restored = engine(config.copy(initialState = original.captureRun(), restorePaused = true))
        assertEquals(GameState.PAUSED, restored.state)
        assertEquals(2, restored.streakBonusRemaining)
        assertTrue(restored.magnetActive)
        assertTrue(restored.balls.first().stuckToPaddle)
        assertEquals(3f, restored.balls.first().stickOffset, 0f)
        assertEquals(1, restored.enemyShots.size)
        assertTrue(restored.activeEffects.containsKey(PowerUpType.LASER))
        restored.resume()
        assertEquals(GameState.READY, restored.state)
    }

    @Test fun storesRealFilesAtomicallyAndIgnoresUncommittedTemporaryWrites() {
        val directory = Files.createTempDirectory("breakout-run").toFile()
        try {
            val store = SnapshotFileStore(directory)
            val original = engine(config(GameMode.CLASSIC)).captureRun()
            store.write("active.json", original.encode())
            java.io.File(directory, "active.json.new").writeText("incomplete write")
            assertEquals(original.runId, RunSnapshot.decode(store.read("active.json")!!).runId)
            store.write("active.json", original.encode())
            assertFalse(java.io.File(directory, "active.json.new").exists())
            store.quarantine("active.json")
            assertNull(store.read("active.json"))
            assertTrue(java.io.File(directory, "active.json.corrupt").isFile)
        } finally { directory.deleteRecursively() }
    }

    @Test fun invalidSnapshotsCannotBeAcceptedAsGameplay() {
        val valid = engine(config(GameMode.CLASSIC)).captureRun().encode()
        for (bad in listOf("", "{}", valid.replace("\"version\":1", "\"version\":999"),
            valid.replace("\"score\":0", "\"score\":-1"))) {
            assertThrows(Exception::class.java) { RunSnapshot.decode(bad) }
        }
    }

    @Test fun rejectsMissingEntityFieldsAndInvalidEnumsBeforeRestoration() {
        val valid = engine(config(GameMode.CLASSIC)).captureRun().encode()
        val missing = JSONObject(valid).apply { getJSONArray("balls").getJSONObject(0).remove("radius") }
        val invalid = JSONObject(valid).apply { getJSONArray("bricks").getJSONObject(0).put("type", "UNKNOWN") }
        val negative = JSONObject(valid).apply { getJSONObject("paddle").put("width", -1) }
        for (bad in listOf(missing, invalid, negative)) {
            assertThrows(Exception::class.java) { RunSnapshot.decode(bad.toString()) }
        }
    }

    @Test fun aNewerDurableChallengeTransactionSurvivesOlderRunCheckpoint() {
        val date = java.time.LocalDate.of(2026, 10, 5)
        val challenges = DailyChallengeManager.generateDailyChallenges(date).toMutableList()
        val config = config(GameMode.CLASSIC).copy(dailyChallenges = challenges, challengeDate = date)
        val saved = engine(config).captureRun()
        challenges[0].progress = challenges[0].targetValue
        challenges[0].completed = true
        challenges[0].rewardGranted = true
        val restored = engine(config.copy(initialState = saved))
        assertTrue(restored.dailyChallenges!![0].completed)
        assertTrue(restored.dailyChallenges!![0].rewardGranted)
    }

    @Test fun sameSeedAndScriptedInputReplayAcrossRefreshRates() {
        var reference: GameEngine? = null
        for (hz in listOf(60, 90, 120, 144, 240)) {
            val engine = engine(config(GameMode.CLASSIC))
            engine.onResize(1080, 2200)
            engine.handleInput(GameInput.Pointer(PointerInput(PointerAction.UP, 0,
                listOf(GamePointer(1, 0.5f, 0.8f, 1f)), 0)))
            val clock = FixedStepClock()
            repeat(hz * 3) {
                repeat(clock.advance(1f / hz)) { engine.update(FixedStepClock.STEP) }
            }
            reference?.let { assertEquivalent(it, engine) }
            reference = engine
        }
    }
}
