package com.breakoutplus

import android.util.Log
import java.util.concurrent.Executors

/** Application-owned ordered writes survive Activity teardown without blocking gameplay. */
object LocalDataWriter {
    private val executor = Executors.newSingleThreadExecutor { job ->
        Thread(job, "BreakoutLocalData").apply { isDaemon = true }
    }
    private val pending = mutableMapOf<String, () -> Unit>()
    /** Replace superseded snapshots instead of growing a hot-path write backlog. */
    fun submitLatest(key: String, action: () -> Unit) {
        synchronized(pending) {
            val scheduled = pending.containsKey(key)
            pending[key] = action
            if (!scheduled) submit {
                val latest = synchronized(pending) { pending.remove(key) }
                latest?.invoke()
            }
        }
    }
    fun awaitPreviousWrites() { executor.submit {}.get(3, java.util.concurrent.TimeUnit.SECONDS) }
    fun submit(action: () -> Unit) {
        executor.execute {
            try { action() } catch (e: Exception) { Log.e("BreakoutLocalData", "Local transaction failed; retained for retry", e) }
        }
    }
}
