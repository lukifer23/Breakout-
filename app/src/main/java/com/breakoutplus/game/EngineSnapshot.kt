package com.breakoutplus.game

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Authoritative state only: no particles, trails, flashes, GL handles or native input. */
class RunSnapshot private constructor(internal val json: JSONObject) {
    val runId: String get() = json.getString("runId")
    val seed: Long get() = json.getLong("seed")
    val mode: GameMode get() = GameMode.valueOf(json.getString("mode"))
    val challengeDate: LocalDate get() = LocalDate.parse(json.getString("challengeDate"))
    val awaitingNextLevel: Boolean get() = json.getJSONObject("state").getBoolean("awaitingNextLevel")
    val terminal: Boolean get() = json.getJSONObject("state").getString("state") == GameState.GAME_OVER.name
    fun encode(): String = json.toString()
    companion object {
        const val VERSION = 1
        fun decode(raw: String): RunSnapshot {
            require(raw.length <= 2000000) { "Run snapshot exceeds bound" }
            val j = JSONObject(raw)
            require(j.getInt("version") == VERSION && j.getInt("rngVersion") == SessionRandom.VERSION)
            require(j.getString("runId").length in 1..128)
            GameMode.valueOf(j.getString("mode"))
            LocalDate.parse(j.getString("challengeDate"))
            val state = j.getJSONObject("state")
            GameState.valueOf(state.getString("state"))
            GameState.valueOf(state.getString("stateBeforePause"))
            require(state.getInt("score") >= 0 && state.getInt("levelIndex") in 0..100000)
            require(state.getInt("lives") in 0..10000)
            require(state.finite("worldWidth") > 0f && state.finite("worldHeight") in 20f..1000f)
            state.getInt("score")
            state.getInt("levelIndex")
            state.getInt("lives")
            state.getInt("lastReportedSecond")
            state.getInt("runBricksBroken")
            state.getInt("runLivesLost")
            state.getInt("combo")
            state.getInt("shieldCharges")
            state.getInt("invaderWaveStyle")
            state.getInt("invaderBurstCount")
            state.getInt("invaderTotal")
            state.getInt("powerupDropsSinceLaser")
            state.getInt("powerupsSinceOffense")
            state.getInt("powerupsSinceDefense")
            state.getInt("powerupsSinceControl")
            state.getInt("volleyBallCount")
            state.getInt("volleyQueuedBalls")
            state.getInt("volleyTurnCount")
            state.getInt("volleyAdvanceRows")
            state.getInt("volleyReturnCount")
            state.getInt("volleyPreferredLaneCol")
            state.getInt("lastVolleySupplyTurn")
            state.getInt("tunnelShotsFired")
            state.getInt("lastTunnelSupplyShot")
            state.getInt("tunnelSupplyReadinessPercent")
            state.getInt("layoutRowBoost")
            state.getInt("layoutColBoost")
            state.getInt("lastResizeWidthPx")
            state.getInt("lastResizeHeightPx")
            state.getInt("streakBonusRemaining")
            state.finite("worldWidth")
            state.finite("worldHeight")
            state.finite("basePaddleWidth")
            state.finite("paddleVelocity")
            state.finite("timeRemaining")
            state.finite("elapsedSeconds")
            state.finite("levelStartTime")
            state.finite("comboTimer")
            state.finite("laserCooldown")
            state.finite("speedMultiplier")
            state.finite("timeWarpMultiplier")
            state.finite("invaderDirection")
            state.finite("invaderSpeed")
            state.finite("invaderBaseSpeed")
            state.finite("invaderShotTimer")
            state.finite("invaderShotCooldown")
            state.finite("invaderBaseShotCooldown")
            state.finite("invaderVolleyTimer")
            state.finite("invaderPauseTimer")
            state.finite("invaderShield")
            state.finite("invaderShieldMax")
            state.finite("invaderFormationOffset")
            state.finite("invaderRowPhase")
            state.finite("invaderRowDrift")
            state.finite("invaderRowPhaseOffset")
            state.finite("invaderTurnSoundCooldown")
            state.finite("aimNormalized")
            state.finite("aimNormalizedTarget")
            state.finite("aimAngle")
            state.finite("touchWorldX")
            state.finite("touchWorldY")
            state.finite("volleyLaunchTimer")
            state.finite("volleyLaunchX")
            state.finite("volleyReturnSumX")
            state.finite("volleyCompactionCheckTimer")
            state.finite("currentAspectRatio")
            state.finite("brickAreaTopRatio")
            state.finite("brickAreaBottomRatio")
            state.finite("brickSpacing")
            state.finite("invaderScale")
            state.finite("globalBrickScale")
            state.finite("rewardScoreMultiplier")
            state.getBoolean("awaitingNextLevel")
            state.getBoolean("lostLifeThisLevel")
            state.getBoolean("guardrailActive")
            state.getBoolean("fireballActive")
            state.getBoolean("magnetActive")
            state.getBoolean("gravityWellActive")
            state.getBoolean("freezeActive")
            state.getBoolean("pierceActive")
            state.getBoolean("invaderShieldAlerted")
            state.getBoolean("invaderShieldCritical")
            state.getBoolean("aimHasInput")
            state.getBoolean("volleyTurnActive")
            state.getBoolean("streakBonusActive")
            state.getBoolean("pendingInitialLayoutRetune")
            require(j.getLong("rngState") in 0 until (1L shl 48))
            require(j.getLong("visualRngState") in 0 until (1L shl 48))
            for ((key, cap) in mapOf("balls" to 256, "bricks" to 5000, "powerups" to 256,
                "beams" to 256, "enemyShots" to 256)) {
                val array = j.getJSONArray(key)
                require(array.length() <= cap)
                for (i in 0 until array.length()) {
                    val entity = array.getJSONObject(i)
                    for (field in entity.keys()) {
                        val value = entity.get(field)
                        if (value is Number) require(value.toDouble().isFinite() && kotlin.math.abs(value.toDouble()) <= 10000000)
                    }
                }
            }
            j.getLong("seed")
            requireNotNull(LevelThemes.themeByName(j.getString("theme")))
            if (!state.isNull("volleyReturnAnchorX")) state.finite("volleyReturnAnchorX")
            fun numbers(o: JSONObject, names: String) {
                names.split(" ").forEach { require(kotlin.math.abs(o.finite(it)) <= 10000000f) }
            }
            fun color(o: JSONObject) {
                val c = o.getJSONArray("color")
                require(c.length() == 4)
                repeat(4) { require(c.getDouble(it).isFinite() && c.getDouble(it) in 0.0..1.0) }
            }
            val paddle = j.getJSONObject("paddle")
            numbers(paddle, "x y width height targetX")
            require(paddle.finite("width") > 0 && paddle.finite("height") > 0)
            j.array("balls").forEach { b ->
                numbers(b, "x y radius vx vy offset"); color(b)
                require(b.finite("radius") > 0)
                b.getBoolean("fireball"); b.getBoolean("stuck")
                if (!b.isNull("ricochet")) require(b.getInt("ricochet") >= 0)
            }
            j.array("bricks").forEach { b ->
                numbers(b, "x y width height baseX baseY vx vy")
                require(b.finite("width") > 0 && b.finite("height") > 0)
                b.getInt("gridX"); b.getInt("gridY"); b.getBoolean("alive")
                BrickType.valueOf(b.getString("type"))
                require(b.getInt("maxHp") in 1..1000000 && b.getInt("hp") in 0..b.getInt("maxHp"))
                require(b.getInt("phase") >= 0 && b.getInt("maxPhase") >= 0 && b.getInt("spawnCount") >= 0)
            }
            j.array("powerups").forEach { p ->
                numbers(p, "x y speed size"); require(p.finite("size") > 0)
                PowerUpType.valueOf(p.getString("type"))
            }
            j.array("beams").forEach { b ->
                numbers(b, "x y width height speed"); color(b)
                require(b.finite("width") > 0 && b.finite("height") > 0)
            }
            j.array("enemyShots").forEach { e ->
                numbers(e, "x y radius vx vy wiggle wobbleFreq age"); color(e)
                require(e.finite("radius") > 0 && e.finite("age") >= 0)
                e.getInt("style")
            }
            val effects = j.getJSONObject("effects")
            effects.keys().forEach { PowerUpType.valueOf(it); require(effects.finite(it) in 0f..100000f) }
            val recent = j.getJSONArray("recentPowerups")
            require(recent.length() <= 100)
            repeat(recent.length()) { PowerUpType.valueOf(recent.getString(it)) }
            if (!j.isNull("challenges")) DailyChallengeCodec.decode(j.getString("challenges"))
            if (j.has("layout")) {
                val l = j.getJSONObject("layout")
                require(l.getInt("rows") in 1..1000 && l.getInt("cols") in 1..1000)
                requireNotNull(LevelThemes.themeByName(l.getString("theme")))
                l.getString("tip")
                require(l.getJSONArray("bricks").length() <= 5000)
                l.array("bricks").forEach { b ->
                    require(b.getInt("col") in 0 until l.getInt("cols") && b.getInt("row") in 0 until l.getInt("rows"))
                    require(b.getInt("hp") in 1..1000000)
                    BrickType.valueOf(b.getString("type"))
                }
            }
            return RunSnapshot(j)
        }
    }
}

private fun JSONObject.finite(key: String): Float = getDouble(key).toFloat().also { require(it.isFinite()) { "Nonfinite $key" } }
private fun color(color: FloatArray) = JSONArray(color.map { it.toDouble() })
private fun JSONObject.color(): FloatArray = getJSONArray("color").let { a ->
    require(a.length() == 4)
    FloatArray(4) { a.getDouble(it).toFloat().coerceIn(0f, 1f) }
}
private fun JSONObject.array(key: String): List<JSONObject> = getJSONArray(key).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

fun GameEngine.captureRun(): RunSnapshot {
    val j = JSONObject().apply {
        put("version", RunSnapshot.VERSION); put("rngVersion", SessionRandom.VERSION)
        put("runId", config.runId); put("seed", config.seed); put("mode", config.mode.name)
        put("challengeDate", config.challengeDate.toString())
        put("rngState", random.state); put("visualRngState", visualRandom.state)
        put("theme", theme.name)
        put("state", JSONObject().apply {
            put("state", state.name); put("stateBeforePause", stateBeforePause.name)
            put("score", score)
            put("levelIndex", levelIndex)
            put("lives", lives)
            put("lastReportedSecond", lastReportedSecond)
            put("runBricksBroken", runBricksBroken)
            put("runLivesLost", runLivesLost)
            put("combo", combo)
            put("shieldCharges", shieldCharges)
            put("invaderWaveStyle", invaderWaveStyle)
            put("invaderBurstCount", invaderBurstCount)
            put("invaderTotal", invaderTotal)
            put("powerupDropsSinceLaser", powerupDropsSinceLaser)
            put("powerupsSinceOffense", powerupsSinceOffense)
            put("powerupsSinceDefense", powerupsSinceDefense)
            put("powerupsSinceControl", powerupsSinceControl)
            put("volleyBallCount", volleyBallCount)
            put("volleyQueuedBalls", volleyQueuedBalls)
            put("volleyTurnCount", volleyTurnCount)
            put("volleyAdvanceRows", volleyAdvanceRows)
            put("volleyReturnCount", volleyReturnCount)
            put("volleyPreferredLaneCol", volleyPreferredLaneCol)
            put("lastVolleySupplyTurn", lastVolleySupplyTurn)
            put("tunnelShotsFired", tunnelShotsFired)
            put("lastTunnelSupplyShot", lastTunnelSupplyShot)
            put("tunnelSupplyReadinessPercent", tunnelSupplyReadinessPercent)
            put("layoutRowBoost", layoutRowBoost)
            put("layoutColBoost", layoutColBoost)
            put("lastResizeWidthPx", lastResizeWidthPx)
            put("lastResizeHeightPx", lastResizeHeightPx)
            put("streakBonusRemaining", streakBonusRemaining)
            put("worldWidth", worldWidth)
            put("worldHeight", worldHeight)
            put("basePaddleWidth", basePaddleWidth)
            put("paddleVelocity", paddleVelocity)
            put("timeRemaining", timeRemaining)
            put("elapsedSeconds", elapsedSeconds)
            put("levelStartTime", levelStartTime)
            put("comboTimer", comboTimer)
            put("laserCooldown", laserCooldown)
            put("speedMultiplier", speedMultiplier)
            put("timeWarpMultiplier", timeWarpMultiplier)
            put("invaderDirection", invaderDirection)
            put("invaderSpeed", invaderSpeed)
            put("invaderBaseSpeed", invaderBaseSpeed)
            put("invaderShotTimer", invaderShotTimer)
            put("invaderShotCooldown", invaderShotCooldown)
            put("invaderBaseShotCooldown", invaderBaseShotCooldown)
            put("invaderVolleyTimer", invaderVolleyTimer)
            put("invaderPauseTimer", invaderPauseTimer)
            put("invaderShield", invaderShield)
            put("invaderShieldMax", invaderShieldMax)
            put("invaderFormationOffset", invaderFormationOffset)
            put("invaderRowPhase", invaderRowPhase)
            put("invaderRowDrift", invaderRowDrift)
            put("invaderRowPhaseOffset", invaderRowPhaseOffset)
            put("invaderTurnSoundCooldown", invaderTurnSoundCooldown)
            put("aimNormalized", aimNormalized)
            put("aimNormalizedTarget", aimNormalizedTarget)
            put("aimAngle", aimAngle)
            put("touchWorldX", touchWorldX)
            put("touchWorldY", touchWorldY)
            put("volleyLaunchTimer", volleyLaunchTimer)
            put("volleyLaunchX", volleyLaunchX)
            put("volleyReturnSumX", volleyReturnSumX)
            put("volleyCompactionCheckTimer", volleyCompactionCheckTimer)
            put("currentAspectRatio", currentAspectRatio)
            put("brickAreaTopRatio", brickAreaTopRatio)
            put("brickAreaBottomRatio", brickAreaBottomRatio)
            put("brickSpacing", brickSpacing)
            put("invaderScale", invaderScale)
            put("globalBrickScale", globalBrickScale)
            put("rewardScoreMultiplier", rewardScoreMultiplier)
            put("awaitingNextLevel", awaitingNextLevel)
            put("lostLifeThisLevel", lostLifeThisLevel)
            put("guardrailActive", guardrailActive)
            put("fireballActive", fireballActive)
            put("magnetActive", magnetActive)
            put("gravityWellActive", gravityWellActive)
            put("freezeActive", freezeActive)
            put("pierceActive", pierceActive)
            put("invaderShieldAlerted", invaderShieldAlerted)
            put("invaderShieldCritical", invaderShieldCritical)
            put("aimHasInput", aimHasInput)
            put("volleyTurnActive", volleyTurnActive)
            put("streakBonusActive", streakBonusActive)
            put("pendingInitialLayoutRetune", pendingInitialLayoutRetune)
            put("volleyReturnAnchorX", if (volleyReturnAnchorX.isFinite()) volleyReturnAnchorX else JSONObject.NULL)
        })
        put("paddle", JSONObject().apply {
            put("x", paddle.x); put("y", paddle.y); put("width", paddle.width)
            put("height", paddle.height); put("targetX", paddle.targetX)
        })
        put("balls", JSONArray().apply { balls.forEach { b -> put(JSONObject().apply {
            put("x", b.x); put("y", b.y); put("radius", b.radius); put("vx", b.vx); put("vy", b.vy)
            put("fireball", b.isFireball); put("color", color(b.color)); put("stuck", b.stuckToPaddle)
            put("offset", b.stickOffset); put("ricochet", b.ricochetBounces ?: JSONObject.NULL)
        }) } })
        put("bricks", JSONArray().apply { bricks.forEach { b -> put(JSONObject().apply {
            put("gridX", b.gridX); put("gridY", b.gridY); put("x", b.x); put("y", b.y)
            put("width", b.width); put("height", b.height); put("baseX", b.baseX); put("baseY", b.baseY)
            put("hp", b.hitPoints); put("maxHp", b.maxHitPoints); put("type", b.type.name); put("alive", b.alive)
            put("vx", b.vx); put("vy", b.vy); put("phase", b.phase); put("maxPhase", b.maxPhase); put("spawnCount", b.spawnCount)
        }) } })
        put("powerups", JSONArray().apply { powerups.forEach { p -> put(JSONObject().apply {
            put("x", p.x); put("y", p.y); put("type", p.type.name); put("speed", p.speed); put("size", p.size)
        }) } })
        put("beams", JSONArray().apply { beams.forEach { b -> put(JSONObject().apply {
            put("x", b.x); put("y", b.y); put("width", b.width); put("height", b.height)
            put("speed", b.speed); put("color", color(b.color))
        }) } })
        put("enemyShots", JSONArray().apply { enemyShots.forEach { shot -> put(JSONObject().apply {
            put("x", shot.x); put("y", shot.y); put("radius", shot.radius); put("vx", shot.vx); put("vy", shot.vy)
            put("color", color(shot.color)); put("style", shot.style); put("wiggle", shot.wiggle)
            put("wobbleFreq", shot.wobbleFreq); put("age", shot.age)
        }) } })
        put("effects", JSONObject().apply { activeEffects.forEach { (type, time) -> put(type.name, time) } })
        put("recentPowerups", JSONArray(recentPowerups.map { it.name }))
        put("challenges", dailyChallenges?.let { DailyChallengeCodec.encode(it) } ?: JSONObject.NULL)
        currentLayout?.let { layout -> put("layout", JSONObject().apply {
            put("rows", layout.rows); put("cols", layout.cols); put("theme", layout.theme.name); put("tip", layout.tip)
            put("bricks", JSONArray().apply { layout.bricks.forEach { b -> put(JSONObject().apply {
                put("col", b.col); put("row", b.row); put("type", b.type.name); put("hp", b.hitPoints)
            }) } })
        }) }
    }
    return RunSnapshot.decode(j.toString())
}

internal fun GameEngine.restoreRun(snapshot: RunSnapshot) {
    require(snapshot.runId == config.runId && snapshot.mode == config.mode && snapshot.seed == config.seed)
    val j = snapshot.json
    val saved = j.getJSONObject("state")
    score = saved.getInt("score")
    levelIndex = saved.getInt("levelIndex")
    lives = saved.getInt("lives")
    lastReportedSecond = saved.getInt("lastReportedSecond")
    runBricksBroken = saved.getInt("runBricksBroken")
    runLivesLost = saved.getInt("runLivesLost")
    combo = saved.getInt("combo")
    shieldCharges = saved.getInt("shieldCharges")
    invaderWaveStyle = saved.getInt("invaderWaveStyle")
    invaderBurstCount = saved.getInt("invaderBurstCount")
    invaderTotal = saved.getInt("invaderTotal")
    powerupDropsSinceLaser = saved.getInt("powerupDropsSinceLaser")
    powerupsSinceOffense = saved.getInt("powerupsSinceOffense")
    powerupsSinceDefense = saved.getInt("powerupsSinceDefense")
    powerupsSinceControl = saved.getInt("powerupsSinceControl")
    volleyBallCount = saved.getInt("volleyBallCount")
    volleyQueuedBalls = saved.getInt("volleyQueuedBalls")
    volleyTurnCount = saved.getInt("volleyTurnCount")
    volleyAdvanceRows = saved.getInt("volleyAdvanceRows")
    volleyReturnCount = saved.getInt("volleyReturnCount")
    volleyPreferredLaneCol = saved.getInt("volleyPreferredLaneCol")
    lastVolleySupplyTurn = saved.getInt("lastVolleySupplyTurn")
    tunnelShotsFired = saved.getInt("tunnelShotsFired")
    lastTunnelSupplyShot = saved.getInt("lastTunnelSupplyShot")
    tunnelSupplyReadinessPercent = saved.getInt("tunnelSupplyReadinessPercent")
    layoutRowBoost = saved.getInt("layoutRowBoost")
    layoutColBoost = saved.getInt("layoutColBoost")
    lastResizeWidthPx = saved.getInt("lastResizeWidthPx")
    lastResizeHeightPx = saved.getInt("lastResizeHeightPx")
    streakBonusRemaining = saved.getInt("streakBonusRemaining")
    worldWidth = saved.finite("worldWidth")
    worldHeight = saved.finite("worldHeight")
    basePaddleWidth = saved.finite("basePaddleWidth")
    paddleVelocity = saved.finite("paddleVelocity")
    timeRemaining = saved.finite("timeRemaining")
    elapsedSeconds = saved.finite("elapsedSeconds")
    levelStartTime = saved.finite("levelStartTime")
    comboTimer = saved.finite("comboTimer")
    laserCooldown = saved.finite("laserCooldown")
    speedMultiplier = saved.finite("speedMultiplier")
    timeWarpMultiplier = saved.finite("timeWarpMultiplier")
    invaderDirection = saved.finite("invaderDirection")
    invaderSpeed = saved.finite("invaderSpeed")
    invaderBaseSpeed = saved.finite("invaderBaseSpeed")
    invaderShotTimer = saved.finite("invaderShotTimer")
    invaderShotCooldown = saved.finite("invaderShotCooldown")
    invaderBaseShotCooldown = saved.finite("invaderBaseShotCooldown")
    invaderVolleyTimer = saved.finite("invaderVolleyTimer")
    invaderPauseTimer = saved.finite("invaderPauseTimer")
    invaderShield = saved.finite("invaderShield")
    invaderShieldMax = saved.finite("invaderShieldMax")
    invaderFormationOffset = saved.finite("invaderFormationOffset")
    invaderRowPhase = saved.finite("invaderRowPhase")
    invaderRowDrift = saved.finite("invaderRowDrift")
    invaderRowPhaseOffset = saved.finite("invaderRowPhaseOffset")
    invaderTurnSoundCooldown = saved.finite("invaderTurnSoundCooldown")
    aimNormalized = saved.finite("aimNormalized")
    aimNormalizedTarget = saved.finite("aimNormalizedTarget")
    aimAngle = saved.finite("aimAngle")
    touchWorldX = saved.finite("touchWorldX")
    touchWorldY = saved.finite("touchWorldY")
    volleyLaunchTimer = saved.finite("volleyLaunchTimer")
    volleyLaunchX = saved.finite("volleyLaunchX")
    volleyReturnSumX = saved.finite("volleyReturnSumX")
    volleyCompactionCheckTimer = saved.finite("volleyCompactionCheckTimer")
    currentAspectRatio = saved.finite("currentAspectRatio")
    brickAreaTopRatio = saved.finite("brickAreaTopRatio")
    brickAreaBottomRatio = saved.finite("brickAreaBottomRatio")
    brickSpacing = saved.finite("brickSpacing")
    invaderScale = saved.finite("invaderScale")
    globalBrickScale = saved.finite("globalBrickScale")
    rewardScoreMultiplier = saved.finite("rewardScoreMultiplier")
    awaitingNextLevel = saved.getBoolean("awaitingNextLevel")
    lostLifeThisLevel = saved.getBoolean("lostLifeThisLevel")
    guardrailActive = saved.getBoolean("guardrailActive")
    fireballActive = saved.getBoolean("fireballActive")
    magnetActive = saved.getBoolean("magnetActive")
    gravityWellActive = saved.getBoolean("gravityWellActive")
    freezeActive = saved.getBoolean("freezeActive")
    pierceActive = saved.getBoolean("pierceActive")
    invaderShieldAlerted = saved.getBoolean("invaderShieldAlerted")
    invaderShieldCritical = saved.getBoolean("invaderShieldCritical")
    aimHasInput = saved.getBoolean("aimHasInput")
    volleyTurnActive = saved.getBoolean("volleyTurnActive")
    streakBonusActive = saved.getBoolean("streakBonusActive")
    pendingInitialLayoutRetune = saved.getBoolean("pendingInitialLayoutRetune")
    state = GameState.valueOf(saved.getString("state"))
    stateBeforePause = GameState.valueOf(saved.getString("stateBeforePause"))
    volleyReturnAnchorX = if (saved.isNull("volleyReturnAnchorX")) Float.NaN else saved.finite("volleyReturnAnchorX")
    isDragging = false; activePointerId = PointerInput.INVALID_POINTER_ID
    val p = j.getJSONObject("paddle")
    paddle = Paddle(p.finite("x"), p.finite("y"), p.finite("width"), p.finite("height"), p.finite("targetX"))
    balls.clear(); balls.addAll(j.array("balls").map { b ->
        Ball(b.finite("x"), b.finite("y"), b.finite("radius"), b.finite("vx"), b.finite("vy"),
            b.getBoolean("fireball"), b.color(), b.getBoolean("stuck"), b.finite("offset"),
            if (b.isNull("ricochet")) null else b.getInt("ricochet"))
    })
    bricks.clear(); bricks.addAll(j.array("bricks").map { b ->
        Brick(b.getInt("gridX"), b.getInt("gridY"), b.finite("x"), b.finite("y"), b.finite("width"), b.finite("height"),
            b.finite("baseX"), b.finite("baseY"), b.getInt("hp"), b.getInt("maxHp"), BrickType.valueOf(b.getString("type")), b.getBoolean("alive")).apply {
            vx = b.finite("vx"); vy = b.finite("vy"); phase = b.getInt("phase")
            maxPhase = b.getInt("maxPhase"); spawnCount = b.getInt("spawnCount")
        }
    })
    powerups.clear(); powerups.addAll(j.array("powerups").map { p -> PowerUp(p.finite("x"), p.finite("y"),
        PowerUpType.valueOf(p.getString("type")), p.finite("speed"), p.finite("size")) })
    beams.clear(); beams.addAll(j.array("beams").map { b -> Beam(b.finite("x"), b.finite("y"), b.finite("width"), b.finite("height"), b.finite("speed"), b.color()) })
    enemyShots.clear(); enemyShots.addAll(j.array("enemyShots").map { s -> EnemyShot(s.finite("x"), s.finite("y"), s.finite("radius"),
        s.finite("vx"), s.finite("vy"), s.color(), s.getInt("style"), s.finite("wiggle"), s.finite("wobbleFreq"), s.finite("age")) })
    activeEffects.clear()
    val effects = j.getJSONObject("effects")
    effects.keys().forEach { name -> activeEffects[PowerUpType.valueOf(name)] = effects.finite(name).coerceAtLeast(0f) }
    recentPowerups.clear()
    val recent = j.getJSONArray("recentPowerups")
    repeat(recent.length()) { recentPowerups.add(PowerUpType.valueOf(recent.getString(it))) }
    if (!j.isNull("challenges")) {
        val savedChallenges = DailyChallengeCodec.decode(j.getString("challenges"))
        val current = dailyChallenges.orEmpty().associateBy { it.id }
        // Daily progress is monotonic. A newer durable daily transaction must survive
        // restoration of a slightly older gameplay checkpoint.
        savedChallenges.forEach { c -> current[c.id]?.let { durable ->
            c.progress = maxOf(c.progress, durable.progress)
            c.completed = c.completed || durable.completed
            c.rewardGranted = c.rewardGranted || durable.rewardGranted
        } }
        dailyChallenges?.clear(); dailyChallenges?.addAll(savedChallenges)
    }
    fun findTheme(name: String) = requireNotNull(LevelThemes.themeByName(name))
    theme = findTheme(j.getString("theme"))
    if (j.has("layout")) {
        val layout = j.getJSONObject("layout")
        currentLayout = LevelFactory.LevelLayout(layout.getInt("rows"), layout.getInt("cols"), layout.array("bricks").map {
            LevelFactory.BrickSpec(it.getInt("col"), it.getInt("row"), BrickType.valueOf(it.getString("type")), it.getInt("hp"))
        }, findTheme(layout.getString("theme")), layout.getString("tip"))
    }
    random.state = j.getLong("rngState"); visualRandom.state = j.getLong("visualRngState")
    particles.clear(); waves.clear(); sortedEffectsDirty = true
    invaderBricks.clear(); invaderBricks.addAll(bricks.filter { it.type == BrickType.INVADER })
    dynamicBrickLayout = config.mode.invaders || bricks.any { it.type == BrickType.MOVING }
    recalcAliveBrickCounters(); buildSpatialHash(); markTunnelGateIntegrityDirty()
}
