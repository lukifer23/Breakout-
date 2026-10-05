package com.breakoutplus

import android.content.Context
import android.util.Log
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.RunSnapshot
import com.breakoutplus.game.SnapshotFileStore
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

object ActiveRunRepository {
    data class Identity(val runId: String, val seed: Long, val mode: GameMode, val date: LocalDate)
    private fun store(context: Context) = SnapshotFileStore(File(context.noBackupFilesDir, "active_run"))
    @Synchronized fun identity(context: Context): Identity? {
        val raw = store(context).read("identity.json") ?: return null
        return runCatching {
            val j = JSONObject(raw)
            require(j.getInt("version") == 1)
            Identity(j.getString("runId"), j.getLong("seed"), GameMode.valueOf(j.getString("mode")), LocalDate.parse(j.getString("date")))
        }.getOrElse {
            store(context).quarantine("identity.json")
            Log.e("ActiveRunRepository", "Invalid saved identity quarantined", it); null
        }
    }
    @Synchronized fun begin(context: Context, config: GameConfig) {
        store(context).write("identity.json", JSONObject().apply {
            put("version", 1); put("runId", config.runId); put("seed", config.seed)
            put("mode", config.mode.name); put("date", config.challengeDate.toString())
        }.toString())
    }
    @Synchronized fun load(context: Context, identity: Identity): RunSnapshot? {
        val raw = store(context).read("active.json") ?: return null
        return runCatching { RunSnapshot.decode(raw) }.getOrElse {
            store(context).quarantine("active.json")
            Log.e("ActiveRunRepository", "Invalid gameplay checkpoint quarantined", it); null
        }?.takeIf { it.runId == identity.runId && it.seed == identity.seed && it.mode == identity.mode && !it.terminal }
    }
    @Synchronized fun save(context: Context, snapshot: RunSnapshot) {
        if (identity(context)?.runId != snapshot.runId || snapshot.terminal) return
        store(context).write("active.json", snapshot.encode())
    }
    @Synchronized fun clear(context: Context, runId: String) {
        if (identity(context)?.runId != runId) return
        store(context).delete("identity.json"); store(context).delete("active.json")
    }
}
